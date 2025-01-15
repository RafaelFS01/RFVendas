package BackEnd.controller;

import BackEnd.model.entity.Item;
import BackEnd.model.service.ItemService;
import BackEnd.util.AlertHelper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SelecionarItensController {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroTipo;
    @FXML private ComboBox<String> filtroStatus;
    @FXML private TableView<Item> tabelaItens;
    @FXML private TableColumn<Item, Boolean> colunaSelecao;
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
    @FXML private Button btnFinalizar;

    private ItemService itemService;
    private ObservableList<Item> todosItens; // Lista de todos os itens carregados.
    private ObservableList<Item> itensSelecionados; // Lista para armazenar os itens selecionados.
    private Consumer<List<Item>> callback;

    public SelecionarItensController() {
        this.itemService = new ItemService();
        this.itensSelecionados = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        configurarTabela();
        carregarItens();
        configurarBusca();
    }

    public void setCallback(Consumer<List<Item>> callback) {
        this.callback = callback;
    }

    private void configurarTabela() {
        colunaId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colunaNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colunaQuantidadeAtual.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getQuantidadeAtual()).asObject());
        colunaQuantidadeEstoque.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getQuantidadeEstoque()).asObject());
        colunaQuantidadeMinima.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getQuantidadeMinima()).asObject());
        colunaVenda.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrecoVenda()).asObject());
        colunaCusto.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrecoCusto()).asObject());
        colunaMedida.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnidadeMedida()));
        colunaCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria().getNome()));
        colunaStatus.setCellValueFactory(data -> {
            Item item = data.getValue();
            String status;
            if (item.getQuantidadeAtual() < item.getQuantidadeMinima()) {
                status = "Estoque baixo";
            } else if (item.getQuantidadeAtual() == item.getQuantidadeEstoque()) {
                status = "Disponível";
            } else {
                status = "Em uso";
            }
            return new SimpleStringProperty(status);
        });

        colunaSelecao.setCellFactory(tc -> new CheckBoxTableCell<Item, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    TableRow<Item> currentRow = getTableRow();
                    Item currentItem = currentRow.getItem();

                    if (currentItem != null) {
                        // Verifica se o item atual está na lista de itens selecionados
                        boolean isSelected = itensSelecionados.contains(currentItem);
                        checkBox.setSelected(isSelected);

                        // Atualiza a lista de itens selecionados quando o estado do CheckBox muda
                        checkBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                            if (isNowSelected) {
                                if (!itensSelecionados.contains(currentItem)) {
                                    itensSelecionados.add(currentItem);
                                }
                            } else {
                                itensSelecionados.remove(currentItem);
                            }
                        });
                    }

                    setGraphic(checkBox);
                }
            }
        });
    }

    private void configurarBusca() {
        // Embrulha a ObservableList em uma FilteredList (inicialmente exibe todos os dados)
        FilteredList<Item> filteredData = new FilteredList<>(todosItens, p -> true);

        // Define o predicado do filtro sempre que o filtro for alterado
        campoBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                // Se o texto do filtro estiver vazio, exibe todos os itens
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compara o nome do item com o texto do filtro
                String lowerCaseFilter = newValue.toLowerCase();

                if (item.getNome().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filtro corresponde ao nome do item
                } else if (String.valueOf(item.getId()).contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(item.getQuantidadeAtual()).contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(item.getQuantidadeEstoque()).contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(item.getQuantidadeMinima()).contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(item.getPrecoVenda()).contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(item.getPrecoCusto()).contains(lowerCaseFilter)) {
                    return true;
                } else if (item.getUnidadeMedida().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (item.getCategoria().getNome().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else {
                    String status;
                    if (item.getQuantidadeAtual() < item.getQuantidadeMinima()) {
                        status = "Estoque baixo";
                    } else if (item.getQuantidadeAtual() == item.getQuantidadeEstoque()) {
                        status = "Disponível";
                    } else {
                        status = "Em uso";
                    }
                    if (status.toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                }

                return false; // Não corresponde ao filtro
            });
        });

        // Adiciona os dados filtrados à tabela
        tabelaItens.setItems(filteredData);
    }

    private void carregarItens() {
        try {
            todosItens = FXCollections.observableArrayList(itemService.listarItens());
            tabelaItens.setItems(todosItens);
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar itens", e.getMessage());
        }
    }

    @FXML
    private void finalizarSelecao() {
        if (callback != null) {
            callback.accept(itensSelecionados);
        }
        // Fecha a janela atual
        ((Stage) btnFinalizar.getScene().getWindow()).close();
    }
}