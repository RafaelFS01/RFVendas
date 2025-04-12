package BackEnd.controller;

import BackEnd.model.service.DependenciaService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import BackEnd.model.entity.Item;
import BackEnd.model.service.ItemService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ListarItensController implements Initializable {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroTipo;
    @FXML private ComboBox<String> filtroStatus;
    @FXML private TableView<Item> tabelaItens;
    @FXML private TableColumn<Item, String> colunaId;
    @FXML private TableColumn<Item, String> colunaNome;
    @FXML private TableColumn<Item, Double> colunaQuantidadeAtual;
    @FXML private TableColumn<Item, Double> colunaQuantidadeEstoque;
    @FXML private TableColumn<Item, Double> colunaQuantidadeMinima;
    @FXML private TableColumn<Item, Double> colunaVenda;
    @FXML private TableColumn<Item, Double> colunaCusto;
    @FXML private TableColumn<Item, String> colunaMedida;
    @FXML private TableColumn<Item, String> colunaCategoria;
    @FXML private TableColumn<Item, String> colunaStatus;
    @FXML private TableColumn<Item, Void> colunaAcoes;
    @FXML private Label statusLabel;

    private final ItemService itemService;
    private final DependenciaService dependenciaService;
    private ObservableList<Item> items;

    public ListarItensController() {
        this.itemService = new ItemService();
        this.dependenciaService = new DependenciaService();
    }


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltros();
        configurarColunas();
        configurarPesquisa();
        carregarItens();
    }

    private void configurarFiltros() {
        filtroTipo.setItems(FXCollections.observableArrayList(
                "Todos",
                "Emprestáveis",
                "Consumíveis"
        ));
        filtroTipo.setValue("Todos");

        filtroStatus.setItems(FXCollections.observableArrayList(
                "Todos",
                "Disponível",
                "Em uso",
                "Estoque baixo"
        ));
        filtroStatus.setValue("Todos");

        filtroTipo.setOnAction(e -> aplicarFiltros());
        filtroStatus.setOnAction(e -> aplicarFiltros());
    }

    private void configurarColunas() {
        colunaId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getId())
        ));

        colunaNome.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNome())
        );

        colunaQuantidadeAtual.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantidadeAtual()).asObject()
        );

        colunaQuantidadeEstoque.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantidadeEstoque()).asObject()
        );

        colunaQuantidadeMinima.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantidadeMinima()).asObject()
        );

        colunaVenda.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getPrecoVenda()).asObject()
        );

        colunaCusto.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getPrecoCusto()).asObject()
        );

        colunaMedida.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getUnidadeMedida())
        );

        colunaCategoria.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCategoria().getNome())
        );


        colunaStatus.setCellValueFactory(data -> {
            Item eq = data.getValue();
            String status;
            if (eq.getQuantidadeAtual() < eq.getQuantidadeMinima()) {
                status = "Estoque baixo";
            } else if (eq.getQuantidadeAtual() == eq.getQuantidadeEstoque()) {
                status = "Disponível";
            } else {
                status = "Em uso";
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        // Configurar coluna de ações
        colunaAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnDeletar = new Button("Deletar");
            private final Button btnAvaria = new Button("Registrar Avaria");
            private final HBox box = new HBox(5);

            {
                btnEditar.getStyleClass().add("btn-edit");
                btnDeletar.getStyleClass().add("btn-delete");
                btnAvaria.getStyleClass().add("btn-avaria");

                btnDeletar.setOnAction(e -> deletarEquipamento(getTableRow().getItem()));

                box.getChildren().addAll(btnEditar, btnDeletar, btnAvaria);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Estilizar coluna de status
        colunaStatus.setCellFactory(column -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Disponível":
                            setStyle("-fx-text-fill: #2e7d32;"); // Verde
                            break;
                        case "Em uso":
                            setStyle("-fx-text-fill: #1565c0;"); // Azul
                            break;
                        case "Estoque baixo":
                            setStyle("-fx-text-fill: #c62828;"); // Vermelho
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });
    }

    private void configurarPesquisa() {
        campoBusca.textProperty().addListener((obs, old, novo) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        if (items == null) return;

        FilteredList<Item> dadosFiltrados = new FilteredList<>(items);
        String textoBusca = campoBusca.getText().toLowerCase();
        String statusSelecionado = filtroStatus.getValue();

        dadosFiltrados.setPredicate(equipamento -> {
            boolean matchBusca = textoBusca.isEmpty() ||
                    equipamento.getDescricao().toLowerCase().contains(textoBusca) ||
                    String.valueOf(equipamento.getId()).contains(textoBusca);


            String status;
            if (equipamento.getQuantidadeAtual() < equipamento.getQuantidadeMinima()) {
                status = "Estoque baixo";
            } else if (equipamento.getQuantidadeAtual() == equipamento.getQuantidadeEstoque()) {
                status = "Disponível";
            } else {
                status = "Em uso";
            }

            boolean matchStatus = statusSelecionado.equals("Todos") ||
                    statusSelecionado.equals(status);

            return matchBusca && matchStatus;
        });
        ConnectionFactory.importarBancoDeDados("BACKUP.2024");
        tabelaItens.setItems(dadosFiltrados);
        atualizarStatusLabel();

    }

    private void carregarItens() {
        try {
            items = FXCollections.observableArrayList(
                    itemService.listarItens()
            );
            tabelaItens.setItems(items);
            atualizarStatusLabel();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar equipamentos", e.getMessage());
        }
    }

    private void deletarEquipamento(Item item) {
        try {


            Optional<ButtonType> result = AlertHelper.showConfirmation(
                    "Confirmar Exclusão",
                    "Deseja realmente excluir o equipamento?",
                    "Esta ação não poderá ser desfeita."
            );

            if (result.isPresent() && result.get() == ButtonType.YES) {
                dependenciaService.excluirItem(item.getId());
                itemService.deletar(item.getId());
                ConnectionFactory.exportarBancoDeDados("BACKUP.2024");
                carregarItens();
                AlertHelper.showSuccess("Equipamento excluído com sucesso!");
            }
        } catch (Exception e) {
            AlertHelper.showError("Erro ao excluir equipamento", e.getMessage());
        }
    }

    @FXML
    private void exportarLista() {
        AlertHelper.showWarning("Em desenvolvimento",
                "A funcionalidade de exportação será implementada em breve.");
    }

    private void atualizarStatusLabel() {
        int total = tabelaItens.getItems().size();
        long disponiveis = tabelaItens.getItems().stream()
                .filter(e -> e.getQuantidadeAtual() == e.getQuantidadeEstoque())
                .count();
        long emUso = tabelaItens.getItems().stream()
                .filter(e -> e.getQuantidadeAtual() < e.getQuantidadeEstoque() &&
                        e.getQuantidadeAtual() >= e.getQuantidadeMinima())
                .count();
        long estoqueBaixo = tabelaItens.getItems().stream()
                .filter(e -> e.getQuantidadeAtual() < e.getQuantidadeMinima())
                .count();

        statusLabel.setText(String.format(
                "Total: %d | Disponíveis: %d | Em uso: %d | Estoque baixo: %d",
                total, disponiveis, emUso, estoqueBaixo
        ));
    }
}