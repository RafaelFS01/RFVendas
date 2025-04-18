package BackEnd;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import BackEnd.util.ConnectionFactory;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carrega o �cone


        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(root, 1200, 600);

        // Carrega os estilosA
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        primaryStage.setTitle("Login - RF Vendas");

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Abre maximizado
        primaryStage.show();

    }

    public static void carregarTelaPrincipal(Stage stage) {
        try {
            ConnectionFactory.importarBancoDeDados("BACKUP.2024");
            Parent root = FXMLLoader.load(Main.class.getResource("/fxml/Main.fxml"));
            Scene scene = new Scene(root, 1200, 800);

            // Carrega os estilos
            scene.getStylesheets().add(Main.class.getResource("/styles/styles.css").toExternalForm());

            stage.setTitle("RF Vendas - Sistema de Gerenciamento");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.setMaximized(true); // Abre maximizado
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}