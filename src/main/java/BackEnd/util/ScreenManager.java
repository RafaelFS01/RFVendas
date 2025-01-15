package BackEnd.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ScreenManager {

    public static void loadScreen(String fxmlPath, String title, Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        // Adicionar folha de estilos
        scene.getStylesheets().add(ScreenManager.class.getResource("/styles/styles.css").toExternalForm());

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    public static Parent loadFXML(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
        return loader.load();
    }

    public static <T> T loadFXMLWithController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
        loader.load();
        return loader.getController();
    }
}