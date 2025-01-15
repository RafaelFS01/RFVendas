package BackEnd.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import BackEnd.model.NivelAcesso;
import BackEnd.util.ConnectionFactory;
import BackEnd.util.SessionManager;
import BackEnd.util.AlertHelper;

public class MainController {

    @FXML private VBox areaPrincipal;
    @FXML private Label labelAdministracao;
    @FXML private Button btnGerenciarUsuarios;
    @FXML private Label usuarioLabel;

    public void setAreaPrincipal(Parent content) {
        areaPrincipal.getChildren().clear();
        areaPrincipal.getChildren().add(content);
    }

    @FXML
    private void initialize() {
        verificarPermissoes();
        usuarioLabel.setText("Usuário: " + SessionManager.getUsuarioLogado().getNome());
    }

    private void verificarPermissoes() {
        boolean isAdmin = SessionManager.getUsuarioLogado().getNivelAcesso() ==
                NivelAcesso.ADMIN.getNivel();
        labelAdministracao.setVisible(isAdmin);
        btnGerenciarUsuarios.setVisible(isAdmin);
    }

    private void carregarFXML(String fxmlPath) {
        try {
            ConnectionFactory.importarBancoDeDados("BACKUP.2024");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            areaPrincipal.getChildren().clear();
            areaPrincipal.getChildren().add(loader.load());
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar tela",
                    "Não foi possível carregar a tela solicitada: " + e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void telaPrincipal() {
        carregarFXML("/fxml/tela-principal.fxml");
    }

    @FXML
    private void mostrarRegistrarPedido() {
        carregarFXML("/fxml/RegistrarPedido.fxml");
    }

    @FXML
    private void mostrarListarPedidos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListarPedidos.fxml"));
            Parent root = loader.load();
            ListarPedidosController listarPedidosController = loader.getController();
            listarPedidosController.setMainController(this); // Passa a referência do MainController

            areaPrincipal.getChildren().clear();
            areaPrincipal.getChildren().add(root);
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar tela",
                    "Não foi possível carregar a tela solicitada: " + e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void mostrarRegistroEquipamento() {
        carregarFXML("/fxml/CadastrarItem.fxml");
    }

    @FXML
    private void mostrarLancarItem() {
        carregarFXML("/fxml/LançarItemEstoque.fxml");
    }

    @FXML
    private void mostrarListaEquipamentos() {
        carregarFXML("/fxml/ListarItens.fxml");
    }

    @FXML
    private void mostrarListaCompras() {
        carregarFXML("/fxml/ListarCompras.fxml");
    }

    @FXML
    private void mostrarCadastroFuncionario() {
        carregarFXML("/fxml/CadastrarCliente.fxml");
    }

    @FXML
    private void mostrarListaFuncionarios() {
        carregarFXML("/fxml/ListarClientes.fxml");
    }

    @FXML
    private void mostrarGerenciamentoUsuarios() {
        carregarFXML("/fxml/GerenciarPermissoes.fxml");
    }

    @FXML
    private void logout() {
        try {
            SessionManager.limparSessao();
            // Aqui você pode adicionar a lógica para voltar para a tela de login
        } catch (Exception e) {
            AlertHelper.showError("Erro ao fazer logout", e.getMessage());
        }
    }
}