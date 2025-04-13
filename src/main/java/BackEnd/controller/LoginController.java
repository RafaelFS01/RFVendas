package BackEnd.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import BackEnd.Main;
import BackEnd.util.SessionManager;
import BackEnd.model.entity.Usuario;
import BackEnd.model.service.UsuarioService;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label mensagemErro;
    @FXML private Button loginButton;
    @FXML private ImageView leftImage;
    @FXML private ImageView iconPicImage;
    @FXML private ImageView rightImage;

    private final UsuarioService usuarioService;

    public LoginController() {
        this.usuarioService = new UsuarioService();
    }

    @FXML
    public void initialize() {
        try {
            // Carrega a imagem

            // Define a imagem para ambos os ImageViews
        } catch (Exception e) {
            System.out.println("Erro ao carregar a imagem: " + e.getMessage());
            e.printStackTrace();
        }

        // Configurar validações dos campos
        configurarValidacoes();
    }

    private void configurarValidacoes() {

        // Configurar ação de enter nos campos
        passwordField.setOnAction(event -> realizarLogin());
    }

    @FXML
    private void realizarLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            mostrarErro("Por favor, preencha todos os campos");
            return;
        }

        try {


            // Tenta autenticar o usuário
            if (!usuarioService.autenticar(username, password)) {
                mostrarErro("Usuário ou senha inválidos.");
                passwordField.clear();
                return;
            }

            Usuario usuario = usuarioService.buscarPorUsername(username);
            // Verifica se há algum usuário com o username digitado.
            if (usuario != null) {
                // Verifica se o usuário está ativo.
                if (!usuario.isAtivo()) {
                    mostrarErro("Usuário está inativo. Contate o administrador.");
                    return;
                }

                // Login bem sucedido
                SessionManager.setUsuarioLogado(usuario);

                // Fecha a tela de login
                Stage loginStage = (Stage) loginButton.getScene().getWindow();
                loginStage.close();

                // Abre a tela principal
                Main.carregarTelaPrincipal(new Stage());
            }

        } catch (Exception e) {
            mostrarErro("Erro ao realizar login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarErro(String mensagem) {
        mensagemErro.setText(mensagem);
        mensagemErro.setVisible(true);
        passwordField.clear(); // Limpa a senha por segurança
    }
}