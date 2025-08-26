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
    requires org.apache.commons.collections4;
    requires com.google.common;
    requires javafx.graphics;
    requires java.desktop;

    opens org.andy.musicque to javafx.fxml;
    exports org.andy.musicque;
    exports org.andy.musicque.controller;
    exports org.andy.musicque.model;
    exports org.andy.musicque.event;
    opens org.andy.musicque.controller to javafx.fxml;
}