package BackEnd.controller;

import BackEnd.model.entity.Cliente;
import BackEnd.model.service.GrupoService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import BackEnd.model.service.ClienteService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ListarClientesController implements Initializable {

    @FXML
    private TextField pesquisaField;
    @FXML
    private TableView<Cliente> tabelaClientes;
    @FXML
    private TableColumn<Cliente, String> colunaId;
    @FXML
    private TableColumn<Cliente, String> colunaNome;
    @FXML
    private TableColumn<Cliente, String> colunaCpfCnpj;
    @FXML
    private TableColumn<Cliente, String> colunaLogradouro;
    @FXML
    private TableColumn<Cliente, String> colunaBairro;
    @FXML
    private TableColumn<Cliente, String> colunaCidade;
    @FXML
    private TableColumn<Cliente, String> colunaNumero;
    @FXML
    private TableColumn<Cliente, String> colunaComplemento;
    @FXML
    private TableColumn<Cliente, String> colunaTelefone;
    @FXML
    private TableColumn<Cliente, String> colunaEmail;
    @FXML
    private TableColumn<Cliente, String> colunaComprador;
    @FXML
    private TableColumn<Cliente, String> colunaTipoCliente;
    @FXML
    private TableColumn<Cliente, String> colunaGrupo;

    @FXML
    private TableColumn<Cliente, Void> colunaAcoes;
    @FXML
    private Label statusLabel;

    private final ClienteService clienteService;
    private ObservableList<Cliente> clientes;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public ListarClientesController() {
        this.clienteService = new ClienteService();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarPesquisa();
        carregarClientes();
    }

    private void configurarColunas() {
        colunaId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));

        colunaNome.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNome()));

        colunaCpfCnpj.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCpfCnpj()));

        colunaLogradouro.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getLogradouro()));

        colunaBairro.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getBairro()));

        colunaCidade.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCidade()));

        colunaNumero.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNumero()));

        colunaComplemento.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getComplemento()));

        colunaTelefone.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTelefoneCelular()));

        colunaEmail.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

        colunaComprador.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getComprador()));

        colunaTipoCliente.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTipoCliente().toString()));

        colunaGrupo.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getGrupo() != null ? data.getValue().getGrupo().getNome() : ""));

        configurarColunaAcoes();
    }

    private void configurarColunaAcoes() {
        colunaAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnDeletar = new Button("Deletar");
            private final HBox container = new HBox(5, btnEditar, btnDeletar);

            {
                btnEditar.getStyleClass().add("btn-edit");
                btnDeletar.getStyleClass().add("btn-delete");

                btnEditar.setOnAction(e -> editarCliente(getTableRow().getItem()));
                btnDeletar.setOnAction(e -> deletarCliente(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void configurarPesquisa() {
        pesquisaField.textProperty().addListener((obs, old, novo) -> {
            if (clientes != null) {
                FilteredList<Cliente> dadosFiltrados = new FilteredList<>(clientes);
                dadosFiltrados.setPredicate(cliente -> {
                    if (novo == null || novo.isEmpty()) {
                        return true;
                    }
                    String filtroLowerCase = novo.toLowerCase();
                    return cliente.getNome().toLowerCase().contains(filtroLowerCase) ||
                            cliente.getCidade().toLowerCase().contains(filtroLowerCase) ||
                            cliente.getBairro().toLowerCase().contains(filtroLowerCase) ||
                            cliente.getCpfCnpj().toLowerCase().contains(filtroLowerCase);
                });
                tabelaClientes.setItems(dadosFiltrados);
                atualizarStatusLabel();
            }
        });
    }

    @FXML
    private void pesquisar() {
        // A pesquisa já é feita pelo listener do TextField
        // Este método existe para responder ao botão de pesquisa
    }

    @FXML
    private void atualizarLista() {
        pesquisaField.clear();
        carregarClientes();
        ConnectionFactory.importarBancoDeDados("BACKUP.2024");
    }

    private void carregarClientes() {
        try {
            clientes = FXCollections.observableArrayList(
                    clienteService.listarTodos()
            );
            tabelaClientes.setItems(clientes);
            atualizarStatusLabel();
        } catch (Exception e) {
            AlertHelper.showError("Erro", "Erro ao carregar clientes: " + e.getMessage());
        }
    }

    private void editarCliente(Cliente cliente) {
        if (cliente == null) return;
        if (mainController == null) { // Verifica se MainController foi injetado
            AlertHelper.showError("Erro de Navegação", "MainController não definido.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CadastrarCliente.fxml")); // Ajuste o caminho se necessário
            Parent root = loader.load();

            CadastrarClienteController cadastrarController = loader.getController();
            // Chama um NOVO método no CadastrarClienteController para carregar dados
            cadastrarController.carregarClienteParaEdicao(cliente.getId());

            mainController.setAreaPrincipal(root); // Usa MainController para exibir

        } catch (IOException e) {
            AlertHelper.showError("Erro ao Abrir Edição", "Não foi possível carregar '/fxml/CadastrarCliente.fxml': " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Erro Inesperado", "Ocorreu um erro ao tentar editar o cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deletarCliente(Cliente cliente) {
        if (cliente != null) {
            try {
                Optional<ButtonType> result = AlertHelper.showConfirmation(
                        "Confirmar Exclusão",
                        "Deseja realmente excluir o cliente?",
                        String.format("Cliente: %s%nCódigo: %s%n%n" +
                                        "Esta ação não poderá ser desfeita.",
                                cliente.getNome(), cliente.getId())
                );

                if (result.isPresent() && result.get() == ButtonType.YES) {
                    clienteService.deletar(cliente.getId());
                    ConnectionFactory.exportarBancoDeDados("BACKUP.2024");
                    carregarClientes();
                    AlertHelper.showSuccess("Cliente excluído com sucesso!");
                }
            } catch (Exception e) {
                AlertHelper.showError("Erro",
                        "Erro ao excluir cliente: " + e.getMessage());
            }
        }
    }

    private void atualizarStatusLabel() {
        int total = tabelaClientes.getItems().size();
        statusLabel.setText(String.format("Total de clientes: %d", total));
    }
}