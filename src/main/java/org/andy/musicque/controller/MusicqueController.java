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
import java.util.List;
import java.util.Map;

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

    private static final int NUM_BANDS = 64;
    private double barWidth;
    private static final double BASS_DAMPING_FACTOR = 0.7;
    private static final double MID_GAIN_FACTOR = 0.7;
    private static final double TREBLE_GAIN_FACTOR = 1.5;
    private static final double SUPER_TREBLE_GAIN_FACTOR = 2;

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

        drawDefaultBars(gc);
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
            drawDefaultBars(gc);
            double segmentHeight = CANVAS_HEIGHT / NUM_COLORS;
            double x = 0;
            double nyquist = 44100.0 / 2.0; // sampleRate/2
            for (int band = 0; band <NUM_BANDS; band++) {
                // --- Tính dải tần cho band ---
                int[] range = getFreqIndexRange(band, nyquist, magnitudes.length);
                int startIndex = range[0];
                int endIndex   = range[1];

                // --- Trung bình magnitude ---
                float avgMag = averageMagnitude(magnitudes, startIndex, endIndex);
                double adjustedValue = applyGain(band, avgMag + 60.0);

                // --- Decay & bar height ---
                double newHeight = (adjustedValue / 60.0) * CANVAS_HEIGHT;
                barHeights[band] = Math.max(newHeight, barHeights[band] * DECAY_RATE);

                // --- Vẽ bar ---
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
        btnPlay.setText("Pause");
    }
    // --- Vẽ mặc định khi chưa có dữ liệu ---
    private void drawDefaultBars(GraphicsContext gc) {
        double segmentHeight = CANVAS_HEIGHT / NUM_COLORS;
        double x = 0;
        for (int band = 0; band < NUM_BANDS-10; band++) {
            double y = CANVAS_HEIGHT - segmentHeight;
            gc.setFill(barColors[63]); // chỉ màu này
            gc.fillRect(x, y, barWidth, segmentHeight);
            x += barWidth;
        }
    }


    private int[] getFreqIndexRange(int band, double nyquist, int spectrumSize) {
        double minFreq = 20;
        double bassMax = 200;
        double midMax = 5000;
        double maxFreq = nyquist;

        int bassBands = 2;                         // This was the issue
        int remainingBands = MusicqueController.NUM_BANDS - bassBands;
        int midBands = 16;
        int trebleBands = remainingBands - midBands;

        double freqStart, freqEnd;

        if (band < bassBands) {
            // Bass: 20–200Hz
            double logStart = (double) band / bassBands;
            double logEnd   = (double) (band + 1) / bassBands;
            freqStart = minFreq * Math.pow(bassMax / minFreq, logStart);
            freqEnd   = minFreq * Math.pow(bassMax / minFreq, logEnd);

        } else if (band < bassBands + midBands) {
            // Mid: 200–5000Hz
            int idx = band - bassBands;
            double logStart = (double) idx / midBands;
            double logEnd   = (double) (idx + 1) / midBands;
            freqStart = bassMax * Math.pow(midMax / bassMax, logStart);
            freqEnd   = bassMax * Math.pow(midMax / bassMax, logEnd);

        } else {
            // Treble: 5000–Nyquist
            int idx = band - bassBands - midBands;
            double logStart = (double) idx / trebleBands;
            double logEnd   = (double) (idx + 1) / trebleBands;
            freqStart = midMax * Math.pow(maxFreq / midMax, logStart);
            freqEnd   = midMax * Math.pow(maxFreq / midMax, logEnd);
        }

        // Quy đổi sang index FFT
        double bandWidth = nyquist / spectrumSize;
        int startIndex = Math.min((int) (freqStart / bandWidth), spectrumSize - 1);
        int endIndex   = Math.min((int) (freqEnd / bandWidth), spectrumSize - 1);
        endIndex = Math.max(startIndex + 1, endIndex);
        return new int[]{startIndex, endIndex};
    }

    private float averageMagnitude(float[] magnitudes, int start, int end) {
        float sum = 0;
        for (int i = start; i < end; i++) sum += magnitudes[i];
        return (end > start) ? (sum / (end - start)) : -64.0f;
    }

    private double applyGain(int band, double value) {
        int bassBands = 2;
        int midBands = 16;

        if (band < bassBands) { // Bass
            return value * BASS_DAMPING_FACTOR;
        } else if (band < bassBands + midBands) { // Mid
            return value * MID_GAIN_FACTOR;
        } else { // Treble
            return value * TREBLE_GAIN_FACTOR;
        }
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
        MenuItem openItem = new MenuItem("Import music");
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