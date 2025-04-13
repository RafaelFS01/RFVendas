package BackEnd.controller;

import BackEnd.model.entity.Pedido;
import BackEnd.model.entity.StatusPedido;
// TipoVenda não é mais usado
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ListarPedidosController {

    // --- FXML Elementos ---
    @FXML private TextField campoBusca;
    @FXML private ComboBox<StatusPedido> filtroStatus;
    @FXML private DatePicker dataInicial;
    @FXML private DatePicker dataFinal;
    @FXML private TableView<Pedido> tabelaPedidos;
    @FXML private TableColumn<Pedido, Boolean> colunaSelecao;
    @FXML private TableColumn<Pedido, Integer> colunaId;
    @FXML private TableColumn<Pedido, String> colunaCliente;
    @FXML private TableColumn<Pedido, LocalDate> colunaData;
    @FXML private TableColumn<Pedido, LocalDate> colunaDataRetorno;
    @FXML private TableColumn<Pedido, Double> colunaValor;
    @FXML private TableColumn<Pedido, StatusPedido> colunaStatus;
    @FXML private TableColumn<Pedido, Void> colunaAcoes;
    @FXML private Button btnVisualizarPedido;

    // --- Serviços e Dados ---
    private PedidoService pedidoService;
    private ObservableList<Pedido> pedidos;
    private ObservableList<Pedido> pedidosSelecionados = FXCollections.observableArrayList();
    private FilteredList<Pedido> filteredData;
    private MainController mainController;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public ListarPedidosController() {
        this.pedidoService = new PedidoService();
    }

    @FXML
    private void initialize() {
        try {
            pedidos = FXCollections.observableArrayList();
            filteredData = new FilteredList<>(pedidos, p -> true);
            tabelaPedidos.setItems(filteredData);

            configurarFiltros();
            configurarColunas();
            carregarPedidos();
        } catch (Exception e) {
            AlertHelper.showError("Erro Fatal na Inicialização", "Não foi possível iniciar a tela de listagem de pedidos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarFiltros() {
        // Configuração do Filtro de Status
        filtroStatus.setItems(FXCollections.observableArrayList(StatusPedido.values()));
        filtroStatus.getItems().add(0, null);
        filtroStatus.setConverter(new javafx.util.StringConverter<StatusPedido>() {
            @Override public String toString(StatusPedido status) { return status == null ? "Todos" : formatarStatus(status); }
            @Override public StatusPedido fromString(String string) {
                if ("Todos".equals(string) || string == null) return null;
                for (StatusPedido sp : StatusPedido.values()) { if (formatarStatus(sp).equals(string)) return sp; }
                return null;
            }
        });
        filtroStatus.setValue(null);

        // Listeners para Reaplicar Filtros
        campoBusca.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        filtroStatus.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        dataInicial.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        dataFinal.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
    }

    private String formatarStatus(StatusPedido status) {
        if (status == null) return "";
        switch (status) {
            case EM_ANDAMENTO: return "Em Andamento";
            case CONCLUIDO: return "Concluído";
            case CANCELADO: return "Cancelado";
            default: return status.name();
        }
    }

    private void configurarColunas() {
        colunaId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colunaCliente.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCliente() != null ? cellData.getValue().getCliente().getNome() : "<Inválido>"
        ));
        colunaData.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataPedido()));
        colunaData.setCellFactory(formatarDataCellFactory());

        colunaDataRetorno.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataRetorno()));
        colunaDataRetorno.setCellFactory(formatarDataCellFactory());

        colunaValor.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorTotal()));
        colunaValor.setCellFactory(formatarMoedaCellFactory());

        colunaStatus.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));
        colunaStatus.setCellFactory(formatarStatusCellFactory());

        colunaAcoes.setCellFactory(createActionCellFactory()); // Modificado para incluir "Concluir"

        tabelaPedidos.setEditable(true);
    }

    private Callback<TableColumn<Pedido, LocalDate>, TableCell<Pedido, LocalDate>> formatarDataCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        };
    }

    private Callback<TableColumn<Pedido, Double>, TableCell<Pedido, Double>> formatarMoedaCellFactory() {
        return tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("R$ %.2f", price));
            }
        };
    }

    private Callback<TableColumn<Pedido, StatusPedido>, TableCell<Pedido, StatusPedido>> formatarStatusCellFactory() {
        return tc -> new TableCell<>() {
            @Override
            protected void updateItem(StatusPedido status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatarStatus(status));
                    String style = "-fx-font-weight: bold; ";
                    switch (status) {
                        case EM_ANDAMENTO: style += "-fx-text-fill: #e67e22;"; break;
                        case CONCLUIDO:    style += "-fx-text-fill: #2ecc71;"; break;
                        case CANCELADO:    style += "-fx-text-fill: #e74c3c;"; break;
                        default:           style = ""; break;
                    }
                    setStyle(style);
                }
            }
        };
    }

    /**
     * Cria um CellFactory para a coluna de Ações (botões).
     * Inclui agora o botão "Concluir".
     * @return Callback para o CellFactory.
     */
    private Callback<TableColumn<Pedido, Void>, TableCell<Pedido, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            // Botões de ação
            private final Button btnCancelar = new Button("Cancelar");
            private final Button btnEditar = new Button("Editar");
            private final Button btnConcluir = new Button("Concluir"); // NOVO BOTÃO
            private final HBox pane = new HBox(5); // Espaçamento 5

            { // Bloco de inicialização
                // Adiciona estilos CSS
                btnCancelar.getStyleClass().addAll("btn-delete", "action-button");
                btnEditar.getStyleClass().addAll("btn-edit", "action-button");
                btnConcluir.getStyleClass().addAll("btn-avaria", "action-button"); // Estilo para concluir

                // Define as ações usando a referência de método da classe externa correta
                btnCancelar.setOnAction(event -> handleAction(ListarPedidosController.this::cancelarPedido));
                btnEditar.setOnAction(event -> handleAction(ListarPedidosController.this::editarPedido));
                btnConcluir.setOnAction(event -> handleAction(ListarPedidosController.this::chamarConcluirPedido)); // Ação para concluir

                // Adiciona os botões ao HBox (incluindo o novo)
                pane.getChildren().addAll(btnCancelar, btnEditar, btnConcluir);
                pane.setAlignment(javafx.geometry.Pos.CENTER);
            }

            // Método auxiliar para obter o pedido e chamar a ação
            private void handleAction(java.util.function.Consumer<Pedido> action) {
                Pedido pedido = getTableView().getItems().get(getIndex());
                if (pedido != null) {
                    action.accept(pedido);
                }
            }

            // Atualiza a célula (habilita/desabilita botões)
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Pedido pedido = getTableView().getItems().get(getIndex());
                    if (pedido != null) {
                        boolean isConcluido = pedido.getStatus() == StatusPedido.CONCLUIDO;
                        boolean isCancelado = pedido.getStatus() == StatusPedido.CANCELADO;
                        boolean isEmAndamento = pedido.getStatus() == StatusPedido.EM_ANDAMENTO;

                        btnEditar.setDisable(isConcluido || isCancelado); // Não edita concluído ou cancelado
                        btnCancelar.setDisable(isConcluido || isCancelado); // Não cancela concluído ou cancelado
                        btnConcluir.setDisable(!isEmAndamento); // SÓ habilita concluir se estiver Em Andamento

                        setGraphic(pane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
    }

    private void carregarPedidos() {
        try {
            pedidosSelecionados.clear();
            pedidos.setAll(pedidoService.listarPedidos());
            tabelaPedidos.sort();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Carregar Pedidos", "Falha ao buscar dados: " + e.getMessage());
            e.printStackTrace();
            pedidos.clear();
        }
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(pedido -> {
            if (pedido == null) return false;

            String textoBusca = campoBusca.getText() == null ? "" : campoBusca.getText().toLowerCase().trim();
            boolean matchBusca = textoBusca.isEmpty() ||
                    (pedido.getCliente() != null && pedido.getCliente().getNome().toLowerCase().contains(textoBusca)) ||
                    String.valueOf(pedido.getId()).contains(textoBusca);

            StatusPedido status = filtroStatus.getValue();
            boolean matchStatus = (status == null || pedido.getStatus().equals(status));

            boolean matchData = true;
            LocalDate dataPed = pedido.getDataPedido();
            LocalDate dataIni = dataInicial.getValue();
            LocalDate dataFim = dataFinal.getValue();
            if (dataPed != null) {
                if (dataIni != null && dataPed.isBefore(dataIni)) matchData = false;
                if (matchData && dataFim != null && dataPed.isAfter(dataFim)) matchData = false;
            } else {
                matchData = (dataIni == null && dataFim == null);
            }

            return matchBusca && matchStatus && matchData;
        });
        pedidosSelecionados.clear();
        tabelaPedidos.refresh();
    }

    private void cancelarPedido(Pedido pedido) {
        if (pedido.getStatus() == StatusPedido.CONCLUIDO || pedido.getStatus() == StatusPedido.CANCELADO) {
            AlertHelper.showWarning("Ação Inválida", "Pedido Concluído ou Cancelado não pode ser cancelado novamente.");
            return;
        }
        Optional<ButtonType> result = AlertHelper.showConfirmation("Confirmar Cancelamento",
                "Deseja realmente cancelar o Pedido ID " + pedido.getId() + "?",
                "Atenção: O estoque NÃO será devolvido automaticamente.");
        if (result.isPresent() && (result.get() == ButtonType.OK || result.get() == ButtonType.YES)) {
            try {
                pedidoService.cancelarPedido(pedido.getId());
                AlertHelper.showSuccess( "Pedido ID " + pedido.getId() + " foi cancelado.");
                carregarPedidos();
            } catch (Exception e) {
                AlertHelper.showError("Erro ao Cancelar", "Falha: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void editarPedido(Pedido pedido) {
        if (pedido.getStatus() == StatusPedido.CONCLUIDO || pedido.getStatus() == StatusPedido.CANCELADO) {
            AlertHelper.showWarning("Ação Inválida", "Pedido Concluído ou Cancelado não pode ser editado.");
            return;
        }
        if (mainController == null) {
            AlertHelper.showError("Erro de Configuração", "MainController não definido.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegistrarPedido.fxml"));
            Parent root = loader.load();
            RegistrarPedidoController controller = loader.getController();
            Pedido pedidoAtualizado = pedidoService.buscarPedidoPorId(pedido.getId());
            if (pedidoAtualizado == null) {
                AlertHelper.showError("Erro", "Pedido ID " + pedido.getId() + " não encontrado.");
                carregarPedidos();
                return;
            }
            controller.preencherDadosPedido(pedidoAtualizado);
            mainController.setAreaPrincipal(root); // Abre na área principal
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Abrir Edição", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método chamado pela ação do botão "Concluir".
     * Pede confirmação e chama o serviço para concluir o pedido.
     * @param pedido O pedido selecionado na linha.
     */
    private void chamarConcluirPedido(Pedido pedido) {
        // Não precisa verificar status aqui, pois o botão já deve estar desabilitado se não for EM_ANDAMENTO
        Optional<ButtonType> result = AlertHelper.showConfirmation(
                "Confirmar Conclusão",
                "Deseja marcar o Pedido ID " + pedido.getId() + " como Concluído?",
                "O status será alterado para 'CONCLUIDO' e a data de retorno será definida como hoje."
        );

        if (result.isPresent() && (result.get() == ButtonType.OK || result.get() == ButtonType.YES)) {
            try {
                // Chama o novo método no serviço
                pedidoService.concluirPedido(pedido.getId());
                AlertHelper.showSuccess("Pedido ID " + pedido.getId() + " marcado como Concluído.");
                carregarPedidos(); // Atualiza a tabela para refletir a mudança
            } catch (Exception e) {
                AlertHelper.showError("Erro ao Concluir", "Não foi possível concluir o pedido: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void visualizarPedidos() {
        if (pedidosSelecionados.isEmpty()) {
            AlertHelper.showWarning("Nenhum Pedido Selecionado", "Selecione um ou mais pedidos na tabela.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VisualizarPedidos.fxml"));
            Parent root = loader.load();
            VisualizarPedidosController visualizarController = loader.getController();
            visualizarController.setPedidos(FXCollections.observableArrayList(pedidosSelecionados));

            if (mainController != null) {
                mainController.setAreaPrincipal(root);
            } else {
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Visualização de " + pedidosSelecionados.size() + " Pedido(s)");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
            }
            pedidosSelecionados.clear();
            tabelaPedidos.refresh();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Visualizar Pedidos", e.getMessage());
            e.printStackTrace();
        }
    }
}