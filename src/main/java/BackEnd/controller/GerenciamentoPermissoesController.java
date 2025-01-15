package BackEnd.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import BackEnd.model.entity.Usuario;
import BackEnd.model.service.UsuarioService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class GerenciamentoPermissoesController implements Initializable {

    @FXML private TextField campoBusca;
    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, String> colunaUsername;
    @FXML private TableColumn<Usuario, String> colunaNome;
    @FXML private TableColumn<Usuario, String> colunaEmail;
    @FXML private TableColumn<Usuario, String> colunaNivel;
    @FXML private TableColumn<Usuario, String> colunaStatus;
    @FXML private TableColumn<Usuario, Void> colunaAcoes;
    @FXML private Label statusLabel;

    private final UsuarioService usuarioService;
    private ObservableList<Usuario> usuarios;

    public GerenciamentoPermissoesController() {
        this.usuarioService = new UsuarioService();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarPesquisa();
        carregarUsuarios();
    }

    private void configurarColunas() {
        colunaUsername.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));

        colunaNome.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNome()));

        colunaEmail.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

        colunaNivel.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getNivelAcesso())));

        colunaStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().isAtivo() ? "Ativo" : "Inativo"));

        // Estilização do status
        colunaStatus.setCellFactory(column -> new TableCell<Usuario, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Ativo")) {
                        setStyle("-fx-text-fill: #2e7d32;"); // Verde
                    } else {
                        setStyle("-fx-text-fill: #c62828;"); // Vermelho
                    }
                }
            }
        });

        // Configurar coluna de ações
        colunaAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnAlterarStatus = new Button("Alterar Status");
            private final HBox box = new HBox(5);

            {
                btnEditar.getStyleClass().add("btn-edit");
                btnAlterarStatus.getStyleClass().add("btn-avaria");

                btnEditar.setOnAction(event -> {
                    Usuario usuario = getTableRow().getItem();
                    if (usuario != null) {
                        editarUsuario(usuario);
                    }
                });

                btnAlterarStatus.setOnAction(event -> {
                    Usuario usuario = getTableRow().getItem();
                    if (usuario != null) {
                        alterarStatusUsuario(usuario);
                    }
                });

                box.getChildren().addAll(btnEditar, btnAlterarStatus);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void configurarPesquisa() {
        campoBusca.textProperty().addListener((obs, old, novo) -> {
            if (usuarios != null) {
                FilteredList<Usuario> dadosFiltrados = new FilteredList<>(usuarios);
                String filtro = novo.toLowerCase();

                dadosFiltrados.setPredicate(usuario ->
                        filtro.isEmpty() ||
                                usuario.getUsername().toLowerCase().contains(filtro) ||
                                usuario.getNome().toLowerCase().contains(filtro) ||
                                usuario.getEmail().toLowerCase().contains(filtro)
                );

                tabelaUsuarios.setItems(dadosFiltrados);
                atualizarStatusLabel();
            }
        });
    }

    @FXML
    private void novoUsuario() {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CadastrarUsuario.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/styles.css").toExternalForm()
            );

            dialogStage.setTitle("Novo Usuário");
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            // Atualiza a lista após o fechamento do diálogo
            carregarUsuarios();
        } catch (IOException e) {
            AlertHelper.showError("Erro", "Erro ao abrir formulário: " + e.getMessage());
        }
    }

    @FXML
    private void atualizarLista() {
        campoBusca.clear();
        carregarUsuarios();
    }

    private void editarUsuario(Usuario usuario) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CadastrarUsuario.fxml")
            );

            Scene scene = new Scene(loader.load());

            CadastrarUsuarioController controller = loader.getController();
            controller.setUsuario(usuario);

            scene.getStylesheets().add(
                    getClass().getResource("/styles/styles.css").toExternalForm()
            );

            dialogStage.setTitle("Editar Usuário");
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            // Atualiza a lista após o fechamento do diálogo
            carregarUsuarios();
        } catch (IOException e) {
            AlertHelper.showError("Erro", "Erro ao abrir formulário: " + e.getMessage());
        }
    }

    private void alterarStatusUsuario(Usuario usuario) {
        String acao = usuario.isAtivo() ? "desativar" : "ativar";
        String status = usuario.isAtivo() ? "inativo" : "ativo";

        Optional<ButtonType> confirmacao = AlertHelper.showConfirmation(
                "Confirmar Alteração",
                "Deseja " + acao + " o usuário " + usuario.getUsername() + "?",
                "O usuário ficará " + status + " no sistema."
        );

        if (confirmacao.isPresent() && confirmacao.get() == ButtonType.OK) {
            try {
                usuario.setAtivo(!usuario.isAtivo());
                usuarioService.atualizarStatus(usuario);
                ConnectionFactory.exportarBancoDeDados("BACKUP.2024");
                carregarUsuarios();
                AlertHelper.showSuccess("Status do usuário alterado com sucesso!");
            } catch (Exception e) {
                AlertHelper.showError("Erro ao alterar status do usuário", e.getMessage());
            }
        }
    }

    private void carregarUsuarios() {
        try {
            usuarios = FXCollections.observableArrayList(usuarioService.listarTodos());
            tabelaUsuarios.setItems(usuarios);
            atualizarStatusLabel();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar usuários", e.getMessage());
        }
    }

    private void atualizarStatusLabel() {
        long totalAtivos = usuarios.stream().filter(Usuario::isAtivo).count();
        long totalInativos = usuarios.size() - totalAtivos;
        statusLabel.setText(String.format(
                "Total de usuários: %d | Ativos: %d | Inativos: %d",
                usuarios.size(), totalAtivos, totalInativos
        ));
    }
}