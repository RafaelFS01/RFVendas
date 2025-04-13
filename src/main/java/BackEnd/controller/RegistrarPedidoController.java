package BackEnd.controller;

import BackEnd.model.entity.*;
import BackEnd.model.service.ClienteService;
import BackEnd.model.service.ItemService; // Manter se usado para buscar Item
import BackEnd.model.service.PedidoService;
import BackEnd.util.AlertHelper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrarPedidoController {

    @FXML private ComboBox<Cliente> cbCliente;
    // REMOVIDO: @FXML private ComboBox<TipoVenda> cbTipoVenda;
    @FXML private DatePicker dpDataPedido;
    @FXML private DatePicker dpDataRetorno; // ADICIONADO
    @FXML private TextArea txtObservacoes;
    @FXML private Button btnAdicionarProdutos;
    @FXML private TableView<ItemPedido> tvItensPedido;
    @FXML private TableColumn<ItemPedido, Integer> colunaId;
    @FXML private TableColumn<ItemPedido, String> colunaNome;
    @FXML private TableColumn<ItemPedido, Double> colunaPrecoVenda;
    @FXML private TableColumn<ItemPedido, String> colunaUnidadeMedida;
    @FXML private TableColumn<ItemPedido, Double> colunaQuantidade;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdAtual;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdEstoque; // Mantido para exibição, se desejado no FXML
    @FXML private TableColumn<ItemPedido, String> colunaCategoria;
    @FXML private TableColumn<ItemPedido, Void> colunaAcoes;
    @FXML private Label lblValorTotal;
    @FXML private Label lblQuantidadeTotal;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;

    private PedidoService pedidoService;
    private ClienteService clienteService;
    // ItemService pode ser necessário se buscar itens individualmente em algum ponto,
    // mas não diretamente para o fluxo principal modificado.
    // private ItemService itemService;

    private ObservableList<Cliente> clientes;
    private ObservableList<ItemPedido> itensPedido = FXCollections.observableArrayList();

    // Atributo para armazenar o pedido sendo editado
    private Pedido pedidoSendoEditado;

    public RegistrarPedidoController() {
        this.pedidoService = new PedidoService();
        this.clienteService = new ClienteService();
        // this.itemService = new ItemService(); // Instanciar se necessário
    }

    @FXML
    private void initialize() {
        carregarClientes();
        // REMOVIDO: configurarComboBoxTipoVenda();
        configurarTableViewItensPedido();
        dpDataPedido.setValue(LocalDate.now());
        dpDataRetorno.setValue(null); // Inicializa data de retorno como nula
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
                    // Busca pelo nome - pode ser otimizado se IDs forem exibidos ou usados
                    return cbCliente.getItems().stream()
                            .filter(cliente -> cliente.getNome().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar clientes", e.getMessage());
            e.printStackTrace(); // Log do erro completo para depuração
        }
    }

    // REMOVIDO: Método configurarComboBoxTipoVenda()
    // private void configurarComboBoxTipoVenda() { ... }

    private void configurarTableViewItensPedido() {
        // Configuração das colunas da tabela (sem alterações significativas aqui)
        colunaId.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleIntegerProperty(item.getId()).asObject() : new SimpleObjectProperty<>(null);
        });
        colunaNome.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getNome() : "<Item Inválido>");
        });
        colunaPrecoVenda.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colunaUnidadeMedida.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getUnidadeMedida() : "");
        });
        colunaQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colunaQtdAtual.setCellValueFactory(cellData -> { // Exibe a quantidade atual do item
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleDoubleProperty(item.getQuantidadeAtual()).asObject() : new SimpleObjectProperty<>(0.0);
        });
        colunaQtdEstoque.setCellValueFactory(cellData -> { // Exibe a quantidade em estoque (se mantida no FXML)
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleDoubleProperty(item.getQuantidadeEstoque()).asObject() : new SimpleObjectProperty<>(0.0);
        });
        colunaCategoria.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            Categoria categoria = (item != null) ? item.getCategoria() : null;
            return new SimpleStringProperty(categoria != null ? categoria.getNome() : "");
        });

        // Tornando as colunas de Preço de Venda e Quantidade editáveis
        colunaPrecoVenda.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaPrecoVenda.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            if (itemPedido != null) {
                itemPedido.setPrecoVenda(event.getNewValue() != null ? event.getNewValue() : 0.0);
                atualizarTotais();
            }
        });

        colunaQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaQuantidade.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            Double novaQuantidade = event.getNewValue();

            if (itemPedido != null && itemPedido.getItem() != null && novaQuantidade != null && novaQuantidade > 0) {
                // Validação de quantidade disponível (Atual) ANTES de commitar a edição
                if (novaQuantidade > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade indisponível",
                            "A quantidade solicitada (" + novaQuantidade + ") para o item '" +
                                    itemPedido.getItem().getNome() + "' excede a quantidade atual disponível (" +
                                    itemPedido.getItem().getQuantidadeAtual() + ").");
                    // Reverte para o valor antigo ou força a recarregar
                    tvItensPedido.refresh(); // Atualiza a célula para o valor antigo
                } else {
                    itemPedido.setQuantidade(novaQuantidade);
                    atualizarTotais();
                }
            } else {
                // Se valor inválido ou item nulo, reverte
                AlertHelper.showWarning("Entrada inválida", "A quantidade deve ser um número positivo.");
                tvItensPedido.refresh();
            }
        });

        tvItensPedido.setItems(itensPedido);
        tvItensPedido.setEditable(true);

        // Configurando a coluna de ações (Remover)
        colunaAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemover = new Button("Remover");

            {
                btnRemover.setOnAction(event -> {
                    ItemPedido itemPedido = getTableView().getItems().get(getIndex());
                    if (itemPedido != null) {
                        removerItemPedido(itemPedido);
                    }
                });
                btnRemover.getStyleClass().add("btn-delete"); // Adiciona a classe CSS para estilização
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
            stage.setTitle("Seleção de Itens");
            stage.initModality(Modality.APPLICATION_MODAL);

            SelecionarItensController controller = loader.getController();
            controller.setCallback(this::adicionarItensAoPedido);

            stage.showAndWait();
        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir janela de seleção de itens", e.getMessage());
            e.printStackTrace();
        }
    }

    public void adicionarItensAoPedido(List<Item> itensSelecionados) {
        for (Item item : itensSelecionados) {
            if (item == null) continue; // Pula itens nulos

            // Verifica se o item já está na lista pelo ID
            boolean jaExiste = itensPedido.stream()
                    .anyMatch(ip -> ip.getItem() != null && ip.getItem().getId() == item.getId());

            if (!jaExiste) {
                // Verifica se há quantidade atual > 0 para adicionar
                if (item.getQuantidadeAtual() <= 0) {
                    AlertHelper.showWarning("Item sem quantidade", "O item '" + item.getNome() + "' não possui quantidade disponível para ser adicionado.");
                    continue; // Pula para o próximo item
                }

                ItemPedido itemPedido = new ItemPedido();
                itemPedido.setItem(item);
                itemPedido.setQuantidade(1.0); // Quantidade inicial padrão
                itemPedido.setPrecoVenda(item.getPrecoVenda()); // Preço de venda padrão do item

                // Verifica se a quantidade inicial (1.0) excede a disponível
                if (itemPedido.getQuantidade() > item.getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade indisponível", "Não há quantidade suficiente para adicionar o item '" + item.getNome() + "'. Disponível: " + item.getQuantidadeAtual());
                    continue; // Pula para o próximo item
                }

                itensPedido.add(itemPedido);
            } else {
                AlertHelper.showWarning("Item já adicionado", "O item '" + item.getNome() + "' já foi adicionado ao pedido.");
            }
        }
        atualizarTotais();
    }

    private void removerItemPedido(ItemPedido itemPedido) {
        itensPedido.remove(itemPedido);
        atualizarTotais();
    }

    private void atualizarTotais() {
        double valorTotal = calcularValorTotalPedido();
        // Usando double para quantidade total para consistência com ItemPedido.quantidade
        double quantidadeTotal = itensPedido.stream()
                .mapToDouble(ItemPedido::getQuantidade)
                .sum();

        lblValorTotal.setText("Valor Total: R$ " + String.format("%.2f", valorTotal));
        // Formata a quantidade total para exibição (ex: 2 casas decimais)
        lblQuantidadeTotal.setText("Quantidade Total: " + String.format("%.2f", quantidadeTotal));
    }

    // Helper para calcular valor total
    private double calcularValorTotalPedido() {
        return itensPedido.stream()
                .mapToDouble(ip -> ip.getQuantidade() * ip.getPrecoVenda())
                .sum();
    }

    @FXML
    private void salvarPedido() {
        try {
            // --- Validações Iniciais ---
            if (cbCliente.getValue() == null || dpDataPedido.getValue() == null) {
                AlertHelper.showWarning("Campos obrigatórios", "Cliente e Data do Pedido são obrigatórios.");
                return;
            }
            if (itensPedido.isEmpty()) {
                AlertHelper.showWarning("Nenhum item adicionado", "Adicione pelo menos um item ao pedido.");
                return;
            }

            // Obter data de retorno (pode ser nula)
            LocalDate dataRetorno = dpDataRetorno.getValue();

            // --- Verificação de Quantidade Atual Disponível ---
            // Verifica ANTES de tentar salvar/atualizar
            for (ItemPedido itemPedido : itensPedido) {
                if (itemPedido.getItem() == null) {
                    AlertHelper.showError("Erro interno", "Pedido contém um item inválido.");
                    return;
                }
                // Verifica se a quantidade no pedido excede a quantidade atual DO ITEM
                if (itemPedido.getQuantidade() > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showError("Quantidade insuficiente",
                            "Não há quantidade atual suficiente para o item: " + itemPedido.getItem().getNome() +
                                    " (Pedido: " + String.format("%.2f", itemPedido.getQuantidade()) +
                                    ", Disponível: " + String.format("%.2f", itemPedido.getItem().getQuantidadeAtual()) + ")");
                    return; // Impede o salvamento
                }
                // Validação adicional: Quantidade e Preço devem ser positivos
                if (itemPedido.getQuantidade() <= 0 || itemPedido.getPrecoVenda() <= 0) {
                    AlertHelper.showError("Valores inválidos",
                            "A quantidade e o preço de venda para o item '" + itemPedido.getItem().getNome() +
                                    "' devem ser maiores que zero.");
                    return;
                }
            }

            // --- Lógica de Salvar (Novo) ou Atualizar (Edição) ---
            if (pedidoSendoEditado != null) {
                // Atualizar Pedido Existente
                pedidoSendoEditado.setCliente(cbCliente.getValue());
                pedidoSendoEditado.setDataPedido(dpDataPedido.getValue());
                pedidoSendoEditado.setDataRetorno(dataRetorno); // Atualiza data de retorno
                pedidoSendoEditado.setObservacoes(txtObservacoes.getText());
                // Garante que a lista de itens no objeto Pedido esteja atualizada
                pedidoSendoEditado.setItens(itensPedido.stream().collect(Collectors.toList()));
                pedidoSendoEditado.setValorTotal(calcularValorTotalPedido());
                // Status: Não mexe no status ao atualizar por aqui, a menos que haja regra específica
                //         A alteração de status (ex: para CONCLUIDO) seria em outra funcionalidade.

                // Chama os serviços para persistir
                pedidoService.atualizarPedido(pedidoSendoEditado); // Atualiza dados do pedido
                pedidoService.atualizarItens(pedidoSendoEditado);  // Atualiza itens (Service deduzirá QtdAtual)

                AlertHelper.showSuccess("Pedido atualizado com sucesso!");

            } else {
                // Criar Novo Pedido
                Pedido pedido = new Pedido();
                pedido.setCliente(cbCliente.getValue());
                pedido.setDataPedido(dpDataPedido.getValue());
                pedido.setDataRetorno(dataRetorno); // Define data de retorno
                pedido.setObservacoes(txtObservacoes.getText());
                pedido.setItens(itensPedido.stream().collect(Collectors.toList()));
                pedido.setValorTotal(calcularValorTotalPedido());
                pedido.setStatus(StatusPedido.EM_ANDAMENTO); // Status padrão para novos pedidos

                // Chama os serviços para persistir
                pedidoService.salvarPedido(pedido); // Salva o pedido (obtém ID)
                pedidoService.salvarItens(pedido);  // Salva os itens E deduz QtdAtual (Service faz isso)

                AlertHelper.showSuccess("Pedido salvo com sucesso!");
            }

            // Limpa a tela após sucesso
            limparCampos();

        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar pedido", "Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace(); // Log detalhado do erro
        }
    }

    @FXML
    private void cancelar() {
        // Apenas limpa os campos e reseta o estado de edição
        limparCampos();
    }

    private void limparCampos() {
        cbCliente.setValue(null);
        // REMOVIDO: cbTipoVenda.setValue(null);
        dpDataPedido.setValue(LocalDate.now());
        dpDataRetorno.setValue(null); // Limpa data de retorno
        txtObservacoes.clear();
        itensPedido.clear(); // Limpa a lista observável (limpa a tabela)
        atualizarTotais(); // Reseta os labels de total
        pedidoSendoEditado = null; // Garante que a próxima ação de salvar seja "novo"
    }

    // Método para preencher os dados do pedido na tela quando for editar
    public void preencherDadosPedido(Pedido pedido) {
        if (pedido == null) {
            limparCampos();
            return;
        }
        pedidoSendoEditado = pedido;

        // Preenche os campos com os dados do pedido
        cbCliente.setValue(pedido.getCliente()); // Assumindo que o Cliente está carregado no pedido
        // REMOVIDO: cbTipoVenda.setValue(pedido.getTipoVenda());
        dpDataPedido.setValue(pedido.getDataPedido());
        dpDataRetorno.setValue(pedido.getDataRetorno()); // Preenche data de retorno
        txtObservacoes.setText(pedido.getObservacoes());

        // Limpa a lista atual e adiciona os itens do pedido recebido
        itensPedido.clear();
        if (pedido.getItens() != null) {
            // IMPORTANTE: Adicionar cópias ou garantir que os itens carregados
            // contenham a informação atualizada de QtdAtual, etc.
            // A abordagem mais segura pode ser recarregar os 'Item' para cada 'ItemPedido'
            // ou garantir que o 'buscarPorId' do PedidoService já faça isso.
            // Por simplicidade aqui, adicionamos diretamente:
            itensPedido.addAll(pedido.getItens());
        } else {
            // Log ou aviso se a lista de itens estiver nula, pode indicar problema no carregamento
            System.err.println("Aviso: Lista de itens nula ao preencher pedido ID: " + pedido.getId());
        }


        // Atualiza os totais exibidos
        atualizarTotais();
    }
}