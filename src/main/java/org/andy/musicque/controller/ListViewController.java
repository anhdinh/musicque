package org.andy.musicque.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.andy.musicque.event.EventSelectedMusic;
import org.andy.musicque.model.FileItem;
import org.andy.musicque.utils.EventBusUtils;
import org.andy.musicque.utils.ImageHelper;


import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ListViewController {
    public TableView<FileItem> tableView;
    public TableColumn<FileItem, String> columnName;
    public TableColumn<FileItem, String> columnDuration;
    public TableColumn<FileItem, String> columnType;
    private final Queue<File> fileQueue = new LinkedList<>();
    @FXML
    private TableColumn<FileItem, Void> columnAction; // Đã sửa kiểu dữ liệu thành Void
    @FXML
    private TableColumn<FileItem, Number> columnNo;

    private int playingIndex = -1;

    private MediaPlayer mediaPlayer;
    Image image = ImageHelper.loadImage("/images/pause-icon.png");

    @FXML
    public void initialize() {
        tableView.getSelectionModel().setCellSelectionEnabled(false);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        columnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        columnNo.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(tableView.getItems().indexOf(cellData.getValue()) + 1)
        );
        tableView.getItems().addListener((ListChangeListener<FileItem>) _ -> tableView.refresh());
        setupActionColumn();
        tableView.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                // Lấy FileItem từ hàng đã click
                FileItem selectedItem = tableView.getSelectionModel().getSelectedItem();
                playingIndex =  tableView.getSelectionModel().getSelectedIndex();
                if (selectedItem != null) {
                    tableView.refresh();
                    EventBusUtils.post(new EventSelectedMusic(selectedItem.getFilePath()));
                }
            } else if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 1) {
                // Tắt trạng thái chọn của hàng nếu có click đơn
                tableView.getSelectionModel().clearSelection();
                tableView.refresh();
                // Lấy item tại vị trí click và chọn nó
                // Đây là cách để chọn hàng bằng tay
                Node clickedNode = event.getPickResult().getIntersectedNode();
                while (clickedNode != null && !(clickedNode instanceof TableRow)) {
                    clickedNode = clickedNode.getParent();
                }
                if (clickedNode != null) {
                    TableRow row = (TableRow) clickedNode;
                    tableView.getSelectionModel().select(row.getIndex());
                }
            }
        });
    }

    public void setupActionColumn() {
        columnAction.setCellFactory(param -> new TableCell<FileItem, Void>() {

           // private final Button playButton = new Button("Play");
            private final Label label = new Label("Playing");
            private final HBox pane = new HBox( label);

            {
                // Initially, both the button and label are invisible.
               // playButton.setVisible(false);
                label.setVisible(false);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(16);
                imageView.setFitHeight(16);
                imageView.setPreserveRatio(true);
                label.setGraphic(imageView);

//                playButton.setOnAction(event -> {
//                    FileItem item = getTableView().getItems().get(getIndex());
//                    if (item != null) {
//                        EventBusUtils.post(new EventSelectedMusic(item.getFilePath()));
//                        // Update the global playingIndex here to reflect the clicked item.
//                        // Assuming 'playingIndex' is a public or accessible field in your controller class.
//                        playingIndex = getIndex();
//                        // Force a refresh so the correct label appears.
//                        getTableView().refresh();
//                    }
//                });

                // This listener will only manage the visibility of the playButton based on selection.
//                this.tableRowProperty().addListener((obs, oldRow, newRow) -> {
//                    if (newRow != null) {
//                        newRow.selectedProperty().addListener((selectedObs, wasSelected, isSelected) -> {
//                            // The playButton should only appear on selection.
//                            playButton.setVisible(isSelected);
//                        });
//                    }
//                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);

                    // Set initial visibility based on the current selection state.
                   // playButton.setVisible(getTableView().getSelectionModel().isSelected(getIndex()));

                    // The label's visibility is independent of the selection. It depends solely on the global playingIndex.
                    label.setVisible(getIndex() == playingIndex);
                }
            }
        });
    }

    public void updateList(List<File> files) {
        tableView.getItems().clear();
        playingIndex = -1;
        if (files != null) {
            fileQueue.addAll(files);
            processNextFile();
        }
    }

    private void processNextFile() {
        if (fileQueue.isEmpty()) {
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            return;
        }

        File file = fileQueue.poll();
        Media media = new Media(file.toURI().toString());

        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            Duration duration = media.getDuration();
            long minutes = (long) duration.toMinutes();
            long seconds = (long) (duration.toSeconds() % 60);
            String formattedDuration = String.format("%02d:%02d", minutes, seconds);

            FileItem item = new FileItem(
                    file.getName(),
                    file.getAbsolutePath(),
                    formattedDuration,
                    getFileType(file)
            );

            Platform.runLater(() -> {
                tableView.getItems().add(item);
            });

            processNextFile();
        });
    }

    private String getFileType(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            return name.substring(idx + 1).toUpperCase();
        }
        return "Unknown";
    }
}