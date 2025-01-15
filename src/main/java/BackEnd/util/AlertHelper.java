package BackEnd.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class AlertHelper {
    public static void showError(String title, String content) {
        show(AlertType.ERROR, title, content);
    }

    public static void showWarning(String title, String content) {
        show(AlertType.WARNING, title, content);
    }

    public static void showInfo(String title, String content) {
        show(AlertType.INFORMATION, title, content);
    }

    public static void showSuccess(String content) {
        show(AlertType.INFORMATION, "Sucesso", content);
    }

    public static Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO); // Use YES e NO
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait();
    }

    private static void show(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}