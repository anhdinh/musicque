package org.andy.musicque.controller;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.andy.musicque.Musicque;
import org.andy.musicque.event.EventSelectedMusic;
import org.andy.musicque.utils.EventBusUtils;
import org.andy.musicque.utils.NotificationUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MusicqueController {
    @FXML
    public Slider progressSlider;

    public Label currentTimeLabel;

    public Label totalTimeLabel;

    public Label songTitleLabel;

    public MediaPlayer mediaPlayer;

    public Button btnPlay;

    public Button btnRepayOne;

    public Canvas spectrumCanvas;

    public Slider volumeSlider;

    public BorderPane mainPane;

    private boolean isReplayOne = false;

    private GraphicsContext gc;
    private static final double CANVAS_WIDTH = 520;
    private static final double CANVAS_HEIGHT = 210;
    private static final double BASS_DAMPING_FACTOR = 0.7;
    private static final int NUM_BANDS = 64;
    private double barWidth;
    private static final double MID_GAIN_FACTOR = 1;
    private static final double TREBLE_GAIN_FACTOR = 1.2;
    private static final double SUPER_TREBLE_GAIN_FACTOR = 2.5;

    private static final int NUM_COLORS = 64;
    private final Color[] barColors = new Color[NUM_COLORS];

    // New array to store current bar heights for decay animation
    private final double[] barHeights = new double[NUM_BANDS];
    private static final double DECAY_RATE = 0.8; // Adjust this value to control decay speed
    ListViewController listViewcontroller;
    Stage listStage;
    String currentFilePath;
    public void initialize() {
        EventBusUtils.register(this);
        gc = spectrumCanvas.getGraphicsContext2D();
        spectrumCanvas.setWidth(CANVAS_WIDTH);
        spectrumCanvas.setHeight(CANVAS_HEIGHT);
        barWidth = CANVAS_WIDTH / NUM_BANDS;
        initMenuBar();
        for (int i = 0; i < NUM_COLORS; i++) {
            double normalizedValue = (double) i / NUM_COLORS;
            double acceleratedValue = Math.pow(normalizedValue, 0.5);
            double hue = (1.0 - acceleratedValue) * 240;
            barColors[i] = Color.hsb(hue, 1.0, 1.0);
        }

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumeSlider.getValue());
        }

        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newValue.doubleValue());
            }
        });
    }

    @FXML
    protected void onPlayButtonClick() {
//        if (isPlaying(mediaPlayer)) {
//            btnPlay.setText("Play");
//            mediaPlayer.pause();
//            return;
//        }
//        if (mediaPlayer != null) {
//            if (mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
//                mediaPlayer.seek(Duration.ZERO);
//                mediaPlayer.play();
//            } else {
//                mediaPlayer.play();
//            }
//            btnPlay.setText("Pause");
//            return;
//        }
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        if(currentFilePath==null){
            currentFilePath = "/home/andy/Music/demo2.mp3";
        }
        String filePath = currentFilePath;
        File file = new File(filePath);
        if (!file.exists()) {
            NotificationUtils.showInfo("Not found", "The file not found or remove!");
            return;
        }
        Media media = new Media(file.toURI().toString());
        mediaPlayer = getMediaPlayer(media);

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            Duration totalDuration = media.getDuration();
            if (totalDuration.greaterThan(Duration.ZERO)) {
                double currentPosition = newValue.toMillis();
                currentTimeLabel.setText(formatDuration(newValue));
                double totalSeconds = totalDuration.toMillis();
                double progress = (currentPosition / totalSeconds);
                progressSlider.setValue(progress * 100);
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            if (isReplayOne) {
                mediaPlayer.play();
            } else {
                mediaPlayer.stop();
                btnPlay.setText("Play");
            }
        });

        mediaPlayer.setAudioSpectrumInterval(0.05);
        mediaPlayer.setAudioSpectrumNumBands(128);

        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            double segmentHeight = CANVAS_HEIGHT / NUM_COLORS;
            double x = 0;
            double sampleRate = 44100.0;
            double nyquist = sampleRate / 2.0;
            double minFreq = 30;

            for (int band = 0; band < NUM_BANDS; band++) {
                double logIndexStart = (double) band / (NUM_BANDS);
                double freqStart = minFreq * Math.pow(nyquist / minFreq, logIndexStart);

                double logIndexEnd = (double) (band + 1) / (NUM_BANDS);
                double freqEnd = minFreq * Math.pow(nyquist / minFreq, logIndexEnd);

                double bandWidth = nyquist / magnitudes.length;
                int startIndex = (int) (freqStart / bandWidth);
                int endIndex = (int) (freqEnd / bandWidth);

                startIndex = Math.min(Math.max(0, startIndex), magnitudes.length - 1);
                endIndex = Math.min(Math.max(0, endIndex), magnitudes.length - 1);
                endIndex = Math.max(startIndex + 1, endIndex);

                float sumMagnitude = 0;
                int count = 0;
                for (int i = startIndex; i < endIndex; i++) {
                    sumMagnitude += magnitudes[i];
                    count++;
                }

                float avgMagnitude = count > 0 ? sumMagnitude / count : -60.0f;
                double cappedMagnitude = Math.max(avgMagnitude, -60.0);
                double adjustedValue = cappedMagnitude + 60.0;

                if (band < 16) {
                    if (band < 4) {
                        adjustedValue *= BASS_DAMPING_FACTOR *0.7;
                    } else if (band < 8) {
                        adjustedValue *= BASS_DAMPING_FACTOR*0.8;
                    } else if (band < 12) {
                        adjustedValue *= BASS_DAMPING_FACTOR *0.9;
                    } else {
                        adjustedValue *= BASS_DAMPING_FACTOR ;
                    }
                } else if (band < 32) {
                    adjustedValue *= MID_GAIN_FACTOR ;
                } else if (band < 48) {
                    adjustedValue *= TREBLE_GAIN_FACTOR;
                } else {
                    adjustedValue *= SUPER_TREBLE_GAIN_FACTOR;
                }
                // Apply decay to bar height
                double newHeight = (adjustedValue / 60.0) * CANVAS_HEIGHT;
                if (newHeight > barHeights[band]) {
                    barHeights[band] = newHeight;
                } else {
                    barHeights[band] *= DECAY_RATE;
                }
                double columnHeight = barHeights[band];
                int numSegments = (int) Math.ceil(columnHeight / segmentHeight);
                for (int j = 0; j < numSegments && j < NUM_COLORS; j++) {
                    double segmentY = CANVAS_HEIGHT - (j + 1) * segmentHeight;
                    gc.setFill(barColors[j]);
                    gc.fillRect(x, segmentY, barWidth, segmentHeight);
                }
                x += barWidth;
            }
        });
        mediaPlayer.play();
        btnPlay.setText("Pause");
    }

    @Subscribe
    public void onSelectedMusic(EventSelectedMusic eventSelectedMusic){
        currentFilePath = eventSelectedMusic.filePath();
        onPlayButtonClick();
    }

    @NotNull
    private MediaPlayer getMediaPlayer(Media media) {
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            Map<String, Object> metadata = media.getMetadata();
            Duration totalDuration = media.getDuration();
            totalTimeLabel.setText(formatDuration(totalDuration));
            if (metadata.containsKey("title")) {
                String title = (String) metadata.get("title");
                songTitleLabel.setText(title);
            } else {
                String fileName = media.getSource();
                songTitleLabel.setText(fileName.substring(fileName.lastIndexOf('/') + 1));
            }
        });
        return mediaPlayer;
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }
        long totalSeconds = (long) duration.toSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isPlaying(MediaPlayer mediaPlayer) {
        if (mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void onReplayOneClick(ActionEvent actionEvent) {
        this.isReplayOne = !this.isReplayOne;
        if (isReplayOne) {
            btnRepayOne.setText("Replay on");
        } else {
            btnRepayOne.setText("Replay off");
        }
    }

    public void initMenuBar(){
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Options");
        MenuItem openItem = new MenuItem("import music");
        MenuItem reOpenList = new MenuItem("Show List");
        MenuItem exitItem = new MenuItem("Exit");
        fileMenu.getItems().addAll(reOpenList,openItem, exitItem);
        menuBar.getMenus().addAll(fileMenu);
        reOpenList.setOnAction(actionEvent -> {
            if (listViewcontroller!=null){
                listStage.show();
            }else{
                NotificationUtils.showError("List empty","list is empty");
            }
        });
        exitItem.setOnAction(actionEvent -> {
            Platform.exit();
            System.exit(0);
        });
        openItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn file");
            List<File> files = fileChooser.showOpenMultipleDialog(mainPane.getScene().getWindow());
            if (CollectionUtils.isNotEmpty(files)) {
                try {
                    if (listViewcontroller!=null){
                        listViewcontroller.updateList(files);
                        listStage.show();
                        return;
                    }
                    FXMLLoader loader = new FXMLLoader(Musicque.class.getResource("list-view.fxml"));
                    Parent root = loader.load();
                    double width = root.prefWidth(-1);
                    double height = root.prefHeight(-1);
                    listViewcontroller = loader.getController();
                    listViewcontroller.updateList(files);
                    Stage newStage = new Stage();
                    listStage = newStage;
                    newStage.setTitle("List Musics");
                    newStage.setScene( new Scene(root, width, height));
                    newStage.setMinWidth(width);
                    newStage.setMinHeight(height);
                    newStage.setOnCloseRequest(xxxx -> {
                        xxxx.consume(); // ngăn mặc định đóng stage
                        newStage.hide(); // chỉ ẩn đi
                    });
                    newStage.show();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        mainPane.setTop(menuBar);
    }
}