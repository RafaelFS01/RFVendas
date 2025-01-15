package BackEnd.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import BackEnd.model.entity.Usuario;
import BackEnd.model.service.UsuarioService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;
import BackEnd.util.ValidationHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class CadastrarUsuarioController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField nomeField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Integer> nivelAcessoCombo;
    @FXML private Label mensagemErro;
    @FXML private Label tituloLabel;
    @FXML private Label subtituloLabel;

    private final UsuarioService usuarioService;
    private Usuario usuarioParaEditar;
    private boolean modoEdicao;

    public CadastrarUsuarioController() {
        this.usuarioService = new UsuarioService();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboNivelAcesso();
        configurarValidacoes();
    }

    public void setUsuario(Usuario usuario) {
        this.usuarioParaEditar = usuario;
        this.modoEdicao = true;
        preencherCampos();
        atualizarTitulo();
    }

    private void configurarComboNivelAcesso() {
        nivelAcessoCombo.getItems().addAll(1, 2, 3); // Níveis de acesso disponíveis
    }

    private void configurarValidacoes() {
        // Adicione validações em tempo real se necessário
        emailField.textProperty().addListener((obs, old, novo) -> {
            if (!ValidationHelper.isValidEmail(novo)) {
                emailField.setStyle("-fx-border-color: red;");
            } else {
                emailField.setStyle("");
            }
        });
    }

    private void preencherCampos() {
        if (usuarioParaEditar != null) {
            usernameField.setText(usuarioParaEditar.getUsername());
            usernameField.setDisable(true); // Não permite alterar username em edição
            nomeField.setText(usuarioParaEditar.getNome());
            emailField.setText(usuarioParaEditar.getEmail());
            nivelAcessoCombo.setValue(usuarioParaEditar.getNivelAcesso());
            passwordField.setPromptText("Deixe em branco para manter a senha atual");
        }
    }

    private void atualizarTitulo() {
        if (modoEdicao) {
            tituloLabel.setText("Editar Usuário");
            subtituloLabel.setText("Altere os dados do usuário");
        }
    }

    @FXML
    private void salvar() {
        try {
            if (validarCampos()) {
                Usuario usuario = modoEdicao ? usuarioParaEditar : new Usuario();
                usuario.setUsername(usernameField.getText());
                usuario.setNome(nomeField.getText());
                usuario.setEmail(emailField.getText());
                usuario.setNivelAcesso(nivelAcessoCombo.getValue());
                usuario.setAtivo(true);

                if (!passwordField.getText().isEmpty()) {
                    usuario.setPassword(passwordField.getText());
                }

                if (modoEdicao) {
                    usuarioService.atualizar(usuario);
                    AlertHelper.showSuccess("Usuário atualizado com sucesso!");
                } else {
                    usuarioService.cadastrar(usuario);
                    AlertHelper.showSuccess("Usuário cadastrado com sucesso!");
                }
                ConnectionFactory.exportarBancoDeDados("BACKUP.2024");
                fecharJanela();
            }
        } catch (Exception e) {
            AlertHelper.showError("Erro", e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        fecharJanela();
    }

    private boolean validarCampos() {
        StringBuilder erros = new StringBuilder();

        if (ValidationHelper.isNullOrEmpty(usernameField.getText())) {
            erros.append("Username é obrigatório\n");
        }

        if (!modoEdicao && ValidationHelper.isNullOrEmpty(passwordField.getText())) {
            erros.append("Senha é obrigatória para novos usuários\n");
        }

        if (ValidationHelper.isNullOrEmpty(nomeField.getText())) {
            erros.append("Nome é obrigatório\n");
        }

        if (ValidationHelper.isNullOrEmpty(emailField.getText())) {
            erros.append("Email é obrigatório\n");
        } else if (!ValidationHelper.isValidEmail(emailField.getText())) {
            erros.append("Email inválido\n");
        }

        if (nivelAcessoCombo.getValue() == null) {
            erros.append("Nível de acesso é obrigatório\n");
        }

        if (erros.length() > 0) {
            mensagemErro.setText(erros.toString());
            mensagemErro.setVisible(true);
            return false;
        }

        return true;
    }

    private void fecharJanela() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}