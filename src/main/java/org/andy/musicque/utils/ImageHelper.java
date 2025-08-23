package org.andy.musicque.utils;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageHelper {
    public static Image loadImage(String path) {
        return new Image(Objects.requireNonNull(ImageHelper.class.getResource(path)).toExternalForm());
    }
}
