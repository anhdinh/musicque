package org.andy.musicque;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.andy.musicque.utils.ImageHelper;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.awt.*;

import static java.awt.Image.SCALE_SMOOTH;

public class Musicque extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Fix 1: Prevent JavaFX thread from exiting when the window is hidden.
        Platform.setImplicitExit(false);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Musicque.class.getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 500);
            stage.setTitle("Music Player");
            stage.setScene(scene);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm()
            );
            ImageHelper.showIcon(stage);
            stage.show();

            addAppToTray(stage);

            stage.setOnCloseRequest(event -> {
                event.consume();
                stage.hide();
            });
        } catch (Exception e) {
            System.out.println("error");
            e.printStackTrace(); // It's better to print the stack trace for debugging
        }
    }

    private void addAppToTray(Stage stage) {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported!");
            return;
        }

        try (InputStream is = getClass().getResourceAsStream("/images/app-tray.png")) {
            if (is == null) throw new IOException("Icon not found!");

            java.awt.Image image = ImageIO.read(is).getScaledInstance(24, 24, SCALE_SMOOTH);

            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "My JavaFX App", popup);
            trayIcon.setImageAutoSize(true);

            exitItem.addActionListener(e -> {
                SystemTray.getSystemTray().remove(trayIcon);
                // The correct way to exit the application completely.
                System.exit(0);
            });

            trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                // Fix 2: Use mouseClicked instead of mousePressed for better cross-platform compatibility.
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    System.out.println("Mouse clicked!");
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                        System.out.println("Mouse clicked2!");
                        // Fix 3: Keep Platform.runLater() as it's the correct way to update the UI from another thread.
                        Platform.runLater(() -> {
                            System.out.println("in runlater");
                            if (!stage.isShowing()) {
                                stage.show();
                            }
                            stage.toFront();
                            // Fix 4: Remove stage.requestFocus() as it can cause issues and is often redundant.
                            // stage.requestFocus();
                        });
                    }
                }
            });

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}