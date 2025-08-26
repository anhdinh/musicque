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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.shape.Rectangle;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.andy.musicque.Musicque;
import org.andy.musicque.event.EventSelectedMusic;
import org.andy.musicque.utils.EventBusUtils;
import org.andy.musicque.utils.ImageHelper;
import org.andy.musicque.utils.NotificationUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public Button btnPrev;
    public Button btnNext;
    public Button btnFavorite;
    public Button btnVolume;
    //🔈 🔊 🔇 🔉
    public String volumeLevelText = "🔊";

    private boolean isFavorite = false;
    private boolean isMute = false;

    private RepeatMode currentMode = RepeatMode.ALL;

    private GraphicsContext gc;
    private static final double CANVAS_WIDTH = 520;
    private static final double CANVAS_HEIGHT = 210;

    private static final int NUM_BANDS = 64;
    private double barWidth;
    private static final int NUM_COLORS = 128;
    private final Color[] barColors = new Color[NUM_COLORS];

    // New array to store current bar heights for decay animation
    private final double[] barHeights = new double[NUM_BANDS];
    private static final double DECAY_RATE = 0.8; // Adjust this value to control decay speed
    ListViewController listViewcontroller;
    Stage listStage;
    String currentFilePath;


    public void initialize() {
        Rectangle background = new Rectangle(mainPane.getWidth(), mainPane.getHeight());
        background.setFill(Color.color(0, 0, 0, 0.9));
        GaussianBlur blur = new GaussianBlur(20);
        background.setEffect(blur);
        background.widthProperty().bind(mainPane.widthProperty());
        background.heightProperty().bind(mainPane.heightProperty());
        mainPane.getChildren().addFirst(background);
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
            btnVolume.setText(getVolumeLevelText(volumeSlider.getValue()));
        }

        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                btnVolume.setText(getVolumeLevelText(newValue.doubleValue()));
                mediaPlayer.setVolume(newValue.doubleValue());
            }
        });


    }

    @FXML
    protected void onPlayButtonClick() {
        if (isPlaying(mediaPlayer)) {
            btnPlay.setText("▶");
            mediaPlayer.pause();
            return;
        }
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
//            if (isReplayOne) {
//                mediaPlayer.play();
//            } else {
//                mediaPlayer.stop();
//                btnPlay.setText("▶");
//            }
        });


        mediaPlayer.setAudioSpectrumInterval(0.05);
        mediaPlayer.setAudioSpectrumNumBands(1024);
        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

            double segmentHeight = CANVAS_HEIGHT / NUM_COLORS;
            double x = 0;

            int newNumBands = NUM_BANDS;
            float[] newMagnitudes = new float[newNumBands];

            double nyquist = 44100.0 / 2.0;

            // --- Band allocation: 0-5kHz 80%, 5-10kHz 15%, 10-15kHz 5%, >15kHz remaining ---
            int bandsFor5kHz = (int) Math.round(newNumBands * 0.8);
            int bandsFor10kHz = (int) Math.round(newNumBands * 0.15);
            int bandsFor15kHz = (int) Math.round(newNumBands * 0.05);
            int remainingBand = newNumBands - (bandsFor5kHz + bandsFor10kHz + bandsFor15kHz);
            if (remainingBand < 1) remainingBand = 1;

            int maxIndex5kHz = (int) (5000 / nyquist * magnitudes.length);
            int maxIndex10kHz = (int) (10000 / nyquist * magnitudes.length);
            int maxIndex15kHz = (int) (15000 / nyquist * magnitudes.length);

            int bandIndex = 0;

            // --- 0-5kHz ---
            for (int i = 0; i < bandsFor5kHz; i++, bandIndex++) {
                int startIndex = (int) Math.floor((double) i / bandsFor5kHz * maxIndex5kHz);
                int endIndex = (int) Math.floor((double) (i + 1) / bandsFor5kHz * maxIndex5kHz);
                newMagnitudes[bandIndex] = averageMagnitudeSafe(magnitudes, startIndex, endIndex);
            }

            // --- 5-10kHz ---
            for (int i = 0; i < bandsFor10kHz; i++, bandIndex++) {
                int startIndex = (int) Math.floor((double) i / bandsFor10kHz * (maxIndex10kHz - maxIndex5kHz)) + maxIndex5kHz;
                int endIndex = (int) Math.floor((double) (i + 1) / bandsFor10kHz * (maxIndex10kHz - maxIndex5kHz)) + maxIndex5kHz;
                newMagnitudes[bandIndex] = averageMagnitudeSafe(magnitudes, startIndex, endIndex);
            }

            // --- 10-15kHz ---
            for (int i = 0; i < bandsFor15kHz; i++, bandIndex++) {
                int startIndex = (int) Math.floor((double) i / bandsFor15kHz * (maxIndex15kHz - maxIndex10kHz)) + maxIndex10kHz;
                int endIndex = (int) Math.floor((double) (i + 1) / bandsFor15kHz * (maxIndex15kHz - maxIndex10kHz)) + maxIndex10kHz;
                newMagnitudes[bandIndex] = averageMagnitudeSafe(magnitudes, startIndex, endIndex);
            }

            // --- >15kHz ---
            float sumRemaining = 0;
            int countRemaining = 0;
            for (int i = maxIndex15kHz; i < magnitudes.length; i++) {
                sumRemaining += magnitudes[i];
                countRemaining++;
            }
            for (int i = 0; i < remainingBand && bandIndex < newNumBands; i++, bandIndex++) {
                newMagnitudes[bandIndex] = countRemaining > 0 ? sumRemaining / countRemaining : 0;
            }

            // --- Vẽ bar ---
            for (int band = 0; band < newNumBands; band++) {
                if (band >= barHeights.length) continue;

                float magnitude = newMagnitudes[band];
                double normalizedMagnitude = magnitude + 60.0;
                if (normalizedMagnitude < 0) normalizedMagnitude = 0;

                // --- Nén logarithm để giảm bass quá cao ---
                double logMagnitude = Math.log10(normalizedMagnitude + 1) * 30; // 20 = hệ số tùy chỉnh

                // --- Weight bass/mid ---
                double bandFreq;
                if (band < bandsFor5kHz)
                    bandFreq = 5000.0 * band / bandsFor5kHz;
                else if (band < bandsFor5kHz + bandsFor10kHz)
                    bandFreq = 5000.0 + 5000.0 * (band - bandsFor5kHz) / bandsFor10kHz;
                else if (band < bandsFor5kHz + bandsFor10kHz + bandsFor15kHz)
                    bandFreq = 10000.0 + 5000.0 * (band - bandsFor5kHz - bandsFor10kHz) / bandsFor15kHz;
                else
                    bandFreq = 15000.0;

                double weight = 1.0;
                if (bandFreq < 200) weight = 1.1;   // giảm mạnh bass
                else if (bandFreq < 500) weight = 1.2;
                else if (bandFreq < 1000) weight = 1.2;
                else if (bandFreq < 2000) weight = 1.5;
                else if (bandFreq < 4000) weight = 2.0;
                else weight = 2.2;

                logMagnitude *= weight;

                // --- Decay ---
                double newHeight = logMagnitude;
                barHeights[band] = Math.max(newHeight, barHeights[band] * DECAY_RATE);

                // --- Vẽ segment ---
                double columnHeight = barHeights[band];
                int numSegments = (int) Math.ceil(columnHeight / segmentHeight);
                for (int j = 0; j < numSegments && j < NUM_COLORS; j++) {
                    double y = CANVAS_HEIGHT - (j + 1) * segmentHeight;
                    gc.setFill(barColors[j]);
                    gc.fillRect(x, y, barWidth, segmentHeight);
                }
                x += barWidth;
            }
        });
        mediaPlayer.play();
        btnPlay.setText("⏸");
    }
    private float averageMagnitudeSafe(float[] magnitudes, int startIndex, int endIndex) {
        if (startIndex < 0) startIndex = 0;
        if (endIndex > magnitudes.length) endIndex = magnitudes.length;
        if (startIndex >= endIndex) return 0;

        float sum = 0;
        for (int i = startIndex; i < endIndex; i++) {
            sum += magnitudes[i];
        }
        return sum / (endIndex - startIndex);
    }

    // ==== drawDefaultBars đồng bộ với main draw ====


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
                try {
                    // Giải mã chuỗi URL
                    String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                    // Trích xuất tên file sau khi đã giải mã
                    songTitleLabel.setText(decodedFileName.substring(decodedFileName.lastIndexOf('/') + 1));
                } catch (Exception e) {
                    // Xử lý lỗi nếu có
                    songTitleLabel.setText("Lỗi: Không thể giải mã tên file");
                }
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
//        this.isReplayOne = !this.isReplayOne;
//        if (isReplayOne) {
//            btnRepayOne.setText("Replay on");
//        } else {
//            btnRepayOne.setText("Replay off");
//        }

        switch (currentMode) {
            case ALL:
                currentMode = RepeatMode.ONE;
                btnRepayOne.setText("🔂"); // Repeat One
                break;
            case ONE:
                currentMode = RepeatMode.SHUFFLE;
                btnRepayOne.setText("🔀"); // Shuffle
                break;
            case SHUFFLE:
                currentMode = RepeatMode.ALL;
                btnRepayOne.setText("🔁"); // Repeat All
                break;
        }
    }

    public void initMenuBar(){
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Options");
        MenuItem openItem = new MenuItem("Import");
        MenuItem reOpenList = new MenuItem("List");
        MenuItem exitItem = new MenuItem("Exit");
        fileMenu.getItems().addAll(reOpenList,openItem, exitItem);
        menuBar.getMenus().addAll(fileMenu);
        reOpenList.setOnAction(actionEvent -> {
            if (listViewcontroller!=null){
                listStage.show();
                listStage.toFront();
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
            files.forEach(ite->{
                System.out.println(ite.getAbsolutePath());;
            });
            if (CollectionUtils.isNotEmpty(files)) {
                try {
                    if (listViewcontroller!=null){
                        listViewcontroller.updateList(files);
                        listStage.show();
                        return;
                    }
                    initList(files);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        mainPane.setTop(menuBar);
    }

    private void initList(List<File> files) throws IOException {
        FXMLLoader loader = new FXMLLoader(Musicque.class.getResource("list-view.fxml"));
        Parent root = loader.load();
        double width = root.prefWidth(-1);
        double height = root.prefHeight(-1);
        listViewcontroller = loader.getController();
        listViewcontroller.updateList(files);
        Stage newStage = new Stage();
        listStage = newStage;
        newStage.setTitle("List");
        newStage.setScene( new Scene(root, width, height));
        newStage.setMinWidth(width);
        newStage.setMinHeight(height);
        ImageHelper.showIcon(newStage);
        newStage.setOnCloseRequest(xxxx -> {
            xxxx.consume(); // ngăn mặc định đóng stage
            newStage.hide(); // chỉ ẩn đi
        });
        newStage.show();
        newStage.toFront();
    }

    public void onFavoriteClick(ActionEvent actionEvent) {
        isFavorite = !isFavorite;
        if (isFavorite) {
            btnFavorite.setText("♡"); // Loại bỏ yêu thích
        } else {
            btnFavorite.setText("❤"); // Thêm yêu thích
        }
        // Thực hiện logic thêm/bỏ yêu thích ở đây, ví dụ:
        // updateFavoriteStatus(isFavorite);
    }

    public void onVolumeClick(ActionEvent actionEvent) {
        if(mediaPlayer==null){
            NotificationUtils.showInfo("No Music","No song playing");
            return;
        }
        isMute = !isMute;
        if(isMute){
            btnVolume.setText("🔇");
            mediaPlayer.setVolume(0.0);
            volumeSlider.setValue(0.0);
        }else{
            volumeSlider.setValue(0.75);
            btnVolume.setText("🔊");
            mediaPlayer.setVolume(0.75);

        }
    }

    //🔈 🔊 🔇 🔉
    public String getVolumeLevelText(double volume) {
        //volumeSlider.getValue()
        if (volume == 0.0){
            isMute = true;
            return "🔇";
        }else if(volume<0.2){
            isMute = false;
            return "🔈";
        }else if(volume<0.5){
            isMute =false;
            return "🔉";
        }else{
            isMute = false;
            return "🔊";
        }
    }
}

