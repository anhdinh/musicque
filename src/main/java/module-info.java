module org.andy.musicque {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.media;
    requires annotations;

    opens org.andy.musicque to javafx.fxml;
    exports org.andy.musicque;
    exports org.andy.musicque.controller;
    opens org.andy.musicque.controller to javafx.fxml;
}