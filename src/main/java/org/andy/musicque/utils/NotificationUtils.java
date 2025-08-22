package org.andy.musicque.utils;

import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public final class NotificationUtils {

    private NotificationUtils() {
        // Lớp tiện ích không nên được khởi tạo
    }

    /**
     * Hiển thị một thông báo thông tin.
     * @param title Tiêu đề của thông báo
     * @param text Nội dung của thông báo
     */
    public static void showInfo(String title, String text) {
        Notifications.create()
                .title(title)
                .text(text)
                .hideAfter(Duration.seconds(4))
                .position(Pos.TOP_RIGHT)
                .showInformation();
    }

    /**
     * Hiển thị một thông báo cảnh báo.
     * @param title Tiêu đề của thông báo
     * @param text Nội dung của thông báo
     */
    public static void showWarning(String title, String text) {
        Notifications.create()
                .title(title)
                .text(text)
                .hideAfter(Duration.seconds(4))
                .position(Pos.TOP_RIGHT)
                .showWarning();
    }

    /**
     * Hiển thị một thông báo lỗi.
     * @param title Tiêu đề của thông báo
     * @param text Nội dung của thông báo
     */
    public static void showError(String title, String text) {
        Notifications.create()
                .title(title)
                .text(text)
                .hideAfter(Duration.seconds(4))
                .position(Pos.TOP_RIGHT)
                .showError();
    }
}