package BackEnd.controller;

import BackEnd.model.entity.Pedido;
import BackEnd.model.entity.StatusPedido;
import BackEnd.model.entity.TipoVenda;
import BackEnd.model.service.PedidoService;
import BackEnd.util.AlertHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public class ListarPedidosController {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<TipoVenda> filtroTipo;
    @FXML private ComboBox<StatusPedido> filtroStatus;
    @FXML private DatePicker dataInicial;
    @FXML private DatePicker dataFinal;
    @FXML private TableView<Pedido> tabelaPedidos;
    @FXML private TableColumn<Pedido, Boolean> colunaSelecao;
    @FXML private TableColumn<Pedido, Integer> colunaId;
    @FXML private TableColumn<Pedido, String> colunaCliente;
    @FXML private TableColumn<Pedido, TipoVenda> colunaTipo;
    @FXML private TableColumn<Pedido, LocalDate> colunaData;
    @FXML private TableColumn<Pedido, Double> colunaValor;
    @FXML private TableColumn<Pedido, StatusPedido> colunaStatus;
    @FXML private TableColumn<Pedido, Void> colunaAcoes;
    @FXML private Button btnVisualizarPedido;

    private PedidoService pedidoService;
    private ObservableList<Pedido> pedidos;
    private ObservableList<Pedido> pedidosSelecionados = FXCollections.observableArrayList();
    private FilteredList<Pedido> filteredData;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public ListarPedidosController() {
        this.pedidoService = new PedidoService();
    }

    @FXML
    private void initialize() {
        try {
            pedidos = FXCollections.observableArrayList(pedidoService.listarPedidos());
            filteredData = new FilteredList<>(pedidos, p -> true);
            tabelaPedidos.setItems(filteredData);
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar pedidos", e.getMessage());
        }
        configurarFiltros();
        configurarColunas();
        configurarBusca();
        carregarPedidos();
    }

    private void configurarFiltros() {
        filtroTipo.setItems(FXCollections.observableArrayList(TipoVenda.values()));
        filtroTipo.getItems().add(0, null);
        filtroTipo.setConverter(new javafx.util.StringConverter<TipoVenda>() {
            @Override
            public String toString(TipoVenda tipoVenda) {
                return tipoVenda == null ? "Todos" : tipoVenda.name();
            }

            @Override
            public TipoVenda fromString(String string) {
                return "Todos".equals(string) ? null : TipoVenda.valueOf(string);
            }
        });
        filtroTipo.setValue(null);

        filtroStatus.setItems(FXCollections.observableArrayList(StatusPedido.values()));
        filtroStatus.getItems().add(0, null);
        filtroStatus.setConverter(new javafx.util.StringConverter<StatusPedido>() {
            @Override
            public String toString(StatusPedido statusPedido) {
                return statusPedido == null ? "Todos" : statusPedido.name();
            }

            @Override
            public StatusPedido fromString(String string) {
                return "Todos".equals(string) ? null : StatusPedido.valueOf(string);
            }
        });
        filtroStatus.setValue(null);

        campoBusca.textProperty().addListener((obs, old, newV) -> aplicarFiltros());
        filtroTipo.valueProperty().addListener((obs, old, newV) -> aplicarFiltros());
        filtroStatus.valueProperty().addListener((obs, old, newV) -> aplicarFiltros());
        dataInicial.valueProperty().addListener((obs, old, newV) -> aplicarFiltros());
        dataFinal.valueProperty().addListener((obs, old, newV) -> aplicarFiltros());
    }

    private void configurarColunas() {
        colunaId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colunaCliente.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCliente().getNome()));
        colunaTipo.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTipoVenda()));
        colunaData.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataPedido()));
        colunaValor.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorTotal()));
        colunaStatus.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));

        colunaSelecao.setCellFactory(tc -> new CheckBoxTableCell<Pedido, Boolean>() {
            private CheckBox checkBox;

            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (checkBox == null) {
                        checkBox = new CheckBox();
                        checkBox.setOnAction(event -> {
                            Pedido pedido = getTableView().getItems().get(getIndex());
                            if (checkBox.isSelected()) {
                                pedidosSelecionados.add(pedido);
                            } else {
                                pedidosSelecionados.remove(pedido);
                            }
                        });
                    }
                    // Atualiza o estado do CheckBox com base na lista de pedidos selecionados
                    checkBox.setSelected(pedidosSelecionados.contains(getTableView().getItems().get(getIndex())));
                    setGraphic(checkBox);
                }
            }
        });

        colunaAcoes.setCellFactory(configurarColunaAcoes());
    }

    private void configurarBusca() {
        // Já configurado no listener do campoBusca em configurarFiltros()
    }

    private void carregarPedidos() {
        try {
            // Atualiza o ObservableList 'pedidos' e reconfigura o filtro
            pedidos.setAll(pedidoService.listarPedidos());
            aplicarFiltros();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar pedidos", e.getMessage());
        }
    }

    private void aplicarFiltros() {
        // Usa o filteredData já criado no initialize para aplicar os filtros
        filteredData.setPredicate(pedido -> {
            String textoBusca = campoBusca.getText().toLowerCase();
            boolean matchBusca = (pedido.getCliente().getNome().toLowerCase().contains(textoBusca)
                    || String.valueOf(pedido.getId()).toLowerCase().contains(textoBusca));

            TipoVenda tipo = filtroTipo.getValue();
            boolean matchTipo = (tipo == null || pedido.getTipoVenda().equals(tipo));

            StatusPedido status = filtroStatus.getValue();
            boolean matchStatus = (status == null || pedido.getStatus().equals(status));

            boolean matchData = true;
            if (dataInicial.getValue() != null && dataFinal.getValue() != null) {
                LocalDate dataIni = dataInicial.getValue();
                LocalDate dataFim = dataFinal.getValue();
                matchData = !pedido.getDataPedido().isBefore(dataIni) && !pedido.getDataPedido().isAfter(dataFim);
            }

            return matchBusca && matchTipo && matchStatus && matchData;
        });
    }

    private Callback<TableColumn<Pedido, Void>, TableCell<Pedido, Void>> configurarColunaAcoes() {
        return new Callback<>() {
            @Override
            public TableCell<Pedido, Void> call(final TableColumn<Pedido, Void> param) {
                return new TableCell<>() {
                    private final Button btnCancelar = new Button("Cancelar");
                    private final Button btnEditar = new Button("Editar");
                    private final Button btnFinalizar = new Button("Finalizar");
                    private final HBox pane = new HBox(5);

                    {
                        btnCancelar.setOnAction(event -> {
                            Pedido pedido = getTableView().getItems().get(getIndex());
                            cancelarPedido(pedido);
                        });
                        btnCancelar.getStyleClass().add("btn-delete");

                        btnEditar.setOnAction(event -> {
                            Pedido pedido = getTableView().getItems().get(getIndex());
                            editarPedido(pedido);
                        });
                        btnEditar.getStyleClass().add("btn-edit");

                        btnFinalizar.setOnAction(event -> {
                            Pedido pedido = getTableView().getItems().get(getIndex());
                            finalizarPedido(pedido);
                        });
                        btnFinalizar.getStyleClass().add("btn-avaria");
                        pane.getChildren().addAll(btnCancelar, btnEditar, btnFinalizar);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Pedido pedido = getTableView().getItems().get(getIndex());
                            // Desabilita o botão Editar se o status for CONCLUIDO
                            btnEditar.setDisable(pedido.getStatus() == StatusPedido.CONCLUIDO);
                            setGraphic(pane);
                        }
                    }
                };
            }
        };
    }

    private void cancelarPedido(Pedido pedido) {
        try {
            Optional<ButtonType> result = AlertHelper.showConfirmation("Cancelar Pedido",
                    "Tem certeza que deseja cancelar o pedido?",
                    "O status do pedido será alterado para 'Cancelado'.");
            if (result.isPresent() && result.get() == ButtonType.YES) {
                pedidoService.cancelarPedido(pedido.getId());
                AlertHelper.showSuccess("Pedido cancelado com sucesso!"); // Exibe a mensagem de sucesso
                carregarPedidos(); // Recarrega os pedidos DEPOIS de exibir a mensagem
            }
        } catch (Exception e) {
            AlertHelper.showError("Erro ao cancelar pedido", e.getMessage());
        }
    }

    private void editarPedido(Pedido pedido) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegistrarPedido.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Editar Pedido");
            stage.initModality(Modality.APPLICATION_MODAL);

            RegistrarPedidoController controller = loader.getController();
            controller.preencherDadosPedido(pedido);

            stage.showAndWait();
            carregarPedidos();
        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir janela de edição de pedido", e.getMessage());
        }
    }

    private void finalizarPedido(Pedido pedido) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Finalizar Pedido");
        alert.setHeaderText("Selecione o tipo de venda para finalizar o pedido:");
        alert.initModality(Modality.APPLICATION_MODAL);

        ButtonType btnVenda = new ButtonType("Venda");
        ButtonType btnNotaFiscal = new ButtonType("Nota Fiscal");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnVenda, btnNotaFiscal, btnCancelar);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            try {
                if (result.get() == btnVenda) {
                    pedidoService.atualizarTipoVenda(pedido.getId(), TipoVenda.VENDA_NORMAL);
                } else if (result.get() == btnNotaFiscal) {
                    pedidoService.atualizarTipoVenda(pedido.getId(), TipoVenda.NOTA_FISCAL);
                }
            } catch (Exception e) {
                AlertHelper.showError("Erro ao finalizar pedido", e.getMessage());
            }
            carregarPedidos();
        }
    }

    @FXML
    private void visualizarPedidos() {
        if (pedidosSelecionados.isEmpty()) {
            AlertHelper.showWarning("Nenhum Pedido Selecionado", "Por favor, selecione ao menos um pedido para visualizar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VisualizarPedidos.fxml"));
            Parent root = loader.load();

            VisualizarPedidosController visualizarPedidosController = loader.getController();
            visualizarPedidosController.setPedidos(pedidosSelecionados);

            mainController.setAreaPrincipal(root); // Chama o método no MainController para atualizar a área principal

        } catch (IOException e) {
            AlertHelper.showError("Erro ao carregar a tela de Visualização de Pedidos", e.getMessage());
            e.printStackTrace();
        }
    }
}