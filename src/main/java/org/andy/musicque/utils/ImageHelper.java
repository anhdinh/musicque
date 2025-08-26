package org.andy.musicque.utils;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.andy.musicque.controller.MusicqueController;

import java.util.Objects;

public class ImageHelper {
    public static Image loadImage(String path) {
        return new Image(Objects.requireNonNull(ImageHelper.class.getResource(path)).toExternalForm());
    }

    public static void showIcon(Stage newStage) {
        newStage.getIcons().add(
                new Image(Objects.requireNonNull(MusicqueController.class.getResourceAsStream("/images/icon-app.png")))
        );
    }
}
