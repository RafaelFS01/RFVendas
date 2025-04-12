package BackEnd.controller;

import BackEnd.model.entity.*;
import BackEnd.model.service.ClienteService;
import BackEnd.model.service.PedidoService;
import BackEnd.util.AlertHelper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrarPedidoController {

    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<TipoVenda> cbTipoVenda;
    @FXML private DatePicker dpDataPedido;
    @FXML private TextArea txtObservacoes;
    @FXML private Button btnAdicionarProdutos;
    @FXML private TableView<ItemPedido> tvItensPedido;
    @FXML private TableColumn<ItemPedido, Integer> colunaId;
    @FXML private TableColumn<ItemPedido, String> colunaNome;
    @FXML private TableColumn<ItemPedido, Double> colunaPrecoVenda;
    @FXML private TableColumn<ItemPedido, String> colunaUnidadeMedida;
    @FXML private TableColumn<ItemPedido, Double> colunaQuantidade;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdAtual;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdEstoque;
    @FXML private TableColumn<ItemPedido, String> colunaCategoria;
    @FXML private TableColumn<ItemPedido, Void> colunaAcoes;
    @FXML private Label lblValorTotal;
    @FXML private Label lblQuantidadeTotal;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;

    private PedidoService pedidoService;
    private ClienteService clienteService;
    private ObservableList<Cliente> clientes;
    private ObservableList<ItemPedido> itensPedido = FXCollections.observableArrayList();

    // Atributo para armazenar o pedido sendo editado
    private Pedido pedidoSendoEditado;

    public RegistrarPedidoController() {
        this.pedidoService = new PedidoService();
        this.clienteService = new ClienteService();
    }

    @FXML
    private void initialize() {
        carregarClientes();
        configurarComboBoxTipoVenda();
        configurarTableViewItensPedido();
        dpDataPedido.setValue(LocalDate.now());
    }

    private void carregarClientes() {
        try {
            clientes = FXCollections.observableArrayList(clienteService.listarTodos());
            cbCliente.setItems(clientes);
            cbCliente.setConverter(new StringConverter<Cliente>() {
                @Override
                public String toString(Cliente cliente) {
                    return cliente != null ? cliente.getNome() : "";
                }

                @Override
                public Cliente fromString(String string) {
                    return cbCliente.getItems().stream()
                            .filter(cliente -> cliente.getNome().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar clientes", e.getMessage());
        }
    }

    private void configurarComboBoxTipoVenda() {
        cbTipoVenda.setItems(FXCollections.observableArrayList(TipoVenda.values()));
    }

    private void configurarTableViewItensPedido() {
        colunaId.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleIntegerProperty(item.getId()).asObject() : new SimpleObjectProperty<>(null);
        });
        colunaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getNome()));
        colunaPrecoVenda.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colunaUnidadeMedida.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getUnidadeMedida()));
        colunaQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colunaQtdAtual.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getItem().getQuantidadeAtual()).asObject());
        colunaQtdEstoque.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getItem().getQuantidadeEstoque()).asObject());
        colunaCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getCategoria().getNome()));

        // Tornando as colunas de Pre?o de Venda e Quantidade edit?veis
        colunaPrecoVenda.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaPrecoVenda.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            itemPedido.setPrecoVenda(event.getNewValue());
            atualizarTotais();
        });

        colunaQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaQuantidade.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            itemPedido.setQuantidade(event.getNewValue());
            atualizarTotais();
        });

        tvItensPedido.setItems(itensPedido);
        tvItensPedido.setEditable(true);

        // Configurando a coluna de a??es
        colunaAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemover = new Button("Remover");

            {
                btnRemover.setOnAction(event -> {
                    ItemPedido itemPedido = getTableView().getItems().get(getIndex());
                    removerItemPedido(itemPedido);
                });
                btnRemover.getStyleClass().add("btn-delete"); // Adiciona a classe CSS para estiliza??o
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnRemover);
                }
            }
        });

        tvItensPedido.setItems(itensPedido);
    }

    @FXML
    private void abrirJanelaAdicionarProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SelecionarItens.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Sele??o de Itens");
            stage.initModality(Modality.APPLICATION_MODAL);

            SelecionarItensController controller = loader.getController();
            controller.setCallback(this::adicionarItensAoPedido);

            stage.showAndWait();
        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir janela de sele??o de itens", e.getMessage());
        }
    }

    public void adicionarItensAoPedido(List<Item> itensSelecionados) {
        for (Item item : itensSelecionados) {
            // Verifica se o item j? est? na lista
            boolean jaExiste = itensPedido.stream()
                    .anyMatch(ip -> ip.getItem().getId() == item.getId());

            if (!jaExiste) {
                ItemPedido itemPedido = new ItemPedido();
                itemPedido.setItem(item);
                itemPedido.setQuantidade(1.0);
                itemPedido.setPrecoVenda(item.getPrecoVenda());
                itensPedido.add(itemPedido);
            } else {
                // Exibe um aviso se o item j? estiver na lista
                AlertHelper.showWarning("Item j? adicionado", "O item '" + item.getNome() + "' j? foi adicionado ao pedido.");
            }
        }
        atualizarTotais();
    }

    private void removerItemPedido(ItemPedido itemPedido) {
        itensPedido.remove(itemPedido);
        atualizarTotais();
    }

    private void atualizarTotais() {
        double valorTotal = itensPedido.stream()
                .mapToDouble(itemPedido -> itemPedido.getQuantidade() * itemPedido.getPrecoVenda())
                .sum();
        int quantidadeTotal = itensPedido.stream()
                .mapToInt(itemPedido -> (int) itemPedido.getQuantidade())
                .sum();

        lblValorTotal.setText("Valor Total: R$ " + String.format("%.2f", valorTotal));
        lblQuantidadeTotal.setText("Quantidade Total: " + quantidadeTotal);
    }

    @FXML
    private void salvarPedido() {
        try {
            if (cbCliente.getValue() == null || cbTipoVenda.getValue() == null || dpDataPedido.getValue() == null) {
                AlertHelper.showWarning("Campos obrigat?rios", "Preencha todos os campos obrigat?rios.");
                return;
            }
            if (itensPedido.isEmpty()) {
                AlertHelper.showWarning("Nenhum item adicionado", "Adicione pelo menos um item ao pedido.");
                return;
            }
            // Verifica se h? estoque dispon?vel para cada item
            for (ItemPedido itemPedido : itensPedido) {
                if (cbTipoVenda.getValue() == TipoVenda.NOTA_FISCAL || cbTipoVenda.getValue() == TipoVenda.VENDA_NORMAL) {
                    if (itemPedido.getQuantidade() > itemPedido.getItem().getQuantidadeEstoque()) {
                        AlertHelper.showError("Estoque insuficiente", "N?o h? estoque suficiente para o item: " + itemPedido.getItem().getNome());
                        return;
                    }
                }
            }

            // Se pedidoSendoEditado n?o for nulo, atualiza o pedido existente
            if (pedidoSendoEditado != null) {
                pedidoSendoEditado.setCliente(cbCliente.getValue());
                pedidoSendoEditado.setTipoVenda(cbTipoVenda.getValue());
                pedidoSendoEditado.setDataPedido(dpDataPedido.getValue());
                pedidoSendoEditado.setObservacoes(txtObservacoes.getText());
                pedidoSendoEditado.setItens(itensPedido.stream().collect(Collectors.toList()));
                pedidoSendoEditado.setValorTotal(itensPedido.stream().mapToDouble(ip -> ip.getQuantidade() * ip.getPrecoVenda()).sum());
                if (cbTipoVenda.getValue() == TipoVenda.PEDIDO) {
                    pedidoSendoEditado.setStatus(StatusPedido.EM_ANDAMENTO);
                } else {
                    pedidoSendoEditado.setStatus(StatusPedido.CONCLUIDO);
                }

                pedidoService.atualizarPedido(pedidoSendoEditado);
                pedidoService.atualizarItens(pedidoSendoEditado);

                AlertHelper.showSuccess("Pedido atualizado com sucesso!");
            } else {
                // Caso contr?rio, cria um novo pedido
                Pedido pedido = new Pedido();
                pedido.setCliente(cbCliente.getValue());
                pedido.setTipoVenda(cbTipoVenda.getValue());
                pedido.setDataPedido(dpDataPedido.getValue());
                pedido.setObservacoes(txtObservacoes.getText());
                pedido.setItens(itensPedido.stream().collect(Collectors.toList()));
                pedido.setValorTotal(itensPedido.stream().mapToDouble(ip -> ip.getQuantidade() * ip.getPrecoVenda()).sum());

                if (cbTipoVenda.getValue() == TipoVenda.PEDIDO) {
                    pedido.setStatus(StatusPedido.EM_ANDAMENTO);
                } else {
                    pedido.setStatus(StatusPedido.CONCLUIDO);
                }

                pedidoService.salvarPedido(pedido);
                pedidoService.salvarItens(pedido);

                AlertHelper.showSuccess("Pedido salvo com sucesso!");
            }

            limparCampos();

        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar pedido", e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        limparCampos();
    }

    private void limparCampos() {
        cbCliente.setValue(null);
        cbTipoVenda.setValue(null);
        dpDataPedido.setValue(LocalDate.now());
        txtObservacoes.clear();
        itensPedido.clear();
        atualizarTotais();
        // Reseta o pedido sendo editado
        pedidoSendoEditado = null;
    }

    // M?todo para preencher os dados do pedido na tela quando for editar
    public void preencherDadosPedido(Pedido pedido) {
        pedidoSendoEditado = pedido;
        // Preenche os campos com os dados do pedido
        cbCliente.setValue(pedido.getCliente());
        cbTipoVenda.setValue(pedido.getTipoVenda());
        dpDataPedido.setValue(pedido.getDataPedido());
        txtObservacoes.setText(pedido.getObservacoes());

        // Limpa a lista atual de itens do pedido e adiciona os itens do pedido recebido
        itensPedido.clear();
        itensPedido.addAll(pedido.getItens());

        // Atualiza os totais
        atualizarTotais();
    }
}