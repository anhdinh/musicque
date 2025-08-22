package org.andy.musicque.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.andy.musicque.utils.NotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

    private boolean isReplayOne = false;

    private GraphicsContext gc;
    private static final double CANVAS_WIDTH = 520;
    private static final double CANVAS_HEIGHT = 210;

    private static final int NUM_BANDS = 60;
    private double barWidth;


    private static final int NUM_COLORS = 60;
    private final Color[] barColors = new Color[NUM_COLORS];

    public void initialize() {
        gc = spectrumCanvas.getGraphicsContext2D();
        spectrumCanvas.setWidth(CANVAS_WIDTH);
        spectrumCanvas.setHeight(CANVAS_HEIGHT);
        barWidth = CANVAS_WIDTH / NUM_BANDS;

        for (int i = 0; i < NUM_COLORS; i++) {
            // Giá trị này sẽ chạy từ 0 đến gần 1
            double normalizedValue = (double) i / NUM_COLORS;

            // Sử dụng hàm pow để tăng tốc độ thay đổi
            // Khi power càng lớn, màu sẽ chuyển nhanh hơn ở cuối dải (gần màu đỏ)
            double acceleratedValue = Math.pow(normalizedValue, 0.5); // Sử dụng lũy thừa 2

            // Màu vẫn chạy từ 240 (xanh blue) đến 0 (đỏ)
            double hue = (1.0 - acceleratedValue) * 240;

            barColors[i] = Color.hsb(hue, 1.0, 1.0);
        }

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumeSlider.getValue());
        }

        // Thêm listener để lắng nghe sự thay đổi của Slider
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Khi giá trị của slider thay đổi, cập nhật âm lượng của MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newValue.doubleValue());
            }
        });
    }


    @FXML
    protected void onPlayButtonClick() {
        if (isPlaying(mediaPlayer)) {
            btnPlay.setText("Play");
            mediaPlayer.pause();
            return;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else {
                mediaPlayer.play();
            }
            btnPlay.setText("Pause");
            return;
        }

        String filePath = "/home/andy/Music/demo2.mp3";
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

        mediaPlayer.setAudioSpectrumInterval(0.1);

        mediaPlayer.setAudioSpectrumNumBands(64);

        mediaPlayer.setAudioSpectrumListener((timestamp, duration,  magnitudes,  phases)->{
            double[] spectrumHeights = new double[magnitudes.length];
            for (int i = 0; i < magnitudes.length; i++) {
                float magnitude = magnitudes[i];
                double cappedMagnitude = Math.max(magnitude, -60.0);
                double adjustedValue = cappedMagnitude + 60.0;
                spectrumHeights[i] = adjustedValue;
            }

            gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            gc.setFill(Color.web("#1A1A1A"));
            double x = 0;
            double segmentHeight = CANVAS_HEIGHT / NUM_COLORS;

            for (double positiveValue : spectrumHeights) {
                // Tính chiều cao tổng của thanh
                double do_cao_cua_1_cot = (positiveValue / 60.0) * CANVAS_HEIGHT;

                int so_luong_segment_da_lam_tron = (int) Math.ceil(do_cao_cua_1_cot/segmentHeight);

                for(int j = 0; j <= so_luong_segment_da_lam_tron; j++){
                    double segmentY = CANVAS_HEIGHT - j*segmentHeight;
                    gc.setFill(barColors[j]);
                    gc.fillRect(x, segmentY, barWidth, segmentHeight);
                }
                x += barWidth;
            }
        });
        mediaPlayer.play();
        btnPlay.setText("Pause");
    }

    @NotNull
    private MediaPlayer getMediaPlayer(Media media) {
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            Map<String, Object> metadata = media.getMetadata();
            Duration totalDuration = media.getDuration();
            totalTimeLabel.setText(formatDuration(totalDuration));
            metadata.forEach((metadataKey, metadataValue) -> {
                System.out.println(metadataKey + ": " + metadataValue);
            });
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
        if(isReplayOne){
            btnRepayOne.setText("Replay on");
        }else{
            btnRepayOne.setText("Replay off");
        }
    }
}
