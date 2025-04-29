package BackEnd.controller;

import BackEnd.model.entity.*; // Importa todas as entidades necessárias
import BackEnd.model.service.ClienteService;
import BackEnd.model.service.ItemService; // Manter se usado para buscar Item em algum ponto futuro
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
import javafx.util.StringConverter; // Import necessário para o converter do ComboBox
import javafx.util.converter.DoubleStringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrarPedidoController {

    // --- FXML Element Declarations ---
    @FXML private ComboBox<Cliente> cbCliente;
    // REMOVIDO: @FXML private ComboBox<TipoVenda> cbTipoVenda; // Comentário mantido para clareza histórica
    @FXML private DatePicker dpDataPedido;
    @FXML private DatePicker dpDataRetorno;         // ADICIONADO ANTERIORMENTE
    @FXML private ComboBox<TipoPagamento> cbTipoPagamento; // <<< NOVO CAMPO ADICIONADO
    @FXML private TextArea txtObservacoes;
    @FXML private Button btnAdicionarProdutos;
    @FXML private TableView<ItemPedido> tvItensPedido;
    @FXML private TableColumn<ItemPedido, Integer> colunaId;
    @FXML private TableColumn<ItemPedido, String> colunaNome;
    @FXML private TableColumn<ItemPedido, Double> colunaPrecoVenda;
    @FXML private TableColumn<ItemPedido, String> colunaUnidadeMedida;
    @FXML private TableColumn<ItemPedido, Double> colunaQuantidade;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdAtual;
    @FXML private TableColumn<ItemPedido, Double> colunaQtdEstoque; // Mantido se presente no FXML
    @FXML private TableColumn<ItemPedido, String> colunaCategoria;
    @FXML private TableColumn<ItemPedido, Void> colunaAcoes;
    @FXML private Label lblValorTotal;
    @FXML private Label lblQuantidadeTotal;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;

    // --- Service Layer Dependencies ---
    private PedidoService pedidoService;
    private ClienteService clienteService;
    // private ItemService itemService; // Instanciar se/quando necessário

    // --- Data Collections ---
    private ObservableList<Cliente> clientes;
    private ObservableList<ItemPedido> itensPedido = FXCollections.observableArrayList();

    // --- State Variable ---
    private Pedido pedidoSendoEditado; // Armazena o pedido em modo de edição

    // --- Constructor ---
    public RegistrarPedidoController() {
        this.pedidoService = new PedidoService();
        this.clienteService = new ClienteService();
        // this.itemService = new ItemService();
    }

    // --- Initialization Method (Called by FXMLLoader) ---
    @FXML
    private void initialize() {
        carregarClientes();
        // REMOVIDO: configurarComboBoxTipoVenda(); // Comentário mantido
        configurarComboBoxTipoPagamento(); // <<< NOVO MÉTODO CHAMADO
        configurarTableViewItensPedido();
        dpDataPedido.setValue(LocalDate.now()); // Define data do pedido como hoje
        dpDataRetorno.setValue(null);          // Inicializa data de retorno como nula
        cbTipoPagamento.setValue(null);        // Inicializa tipo de pagamento como nulo
        // Assegura que os labels de totais comecem zerados
        atualizarTotais();
    }

    // --- Configuration Methods ---

    /**
     * Carrega os clientes do banco de dados e configura o ComboBox de Clientes.
     */
    private void carregarClientes() {
        try {
            clientes = FXCollections.observableArrayList(clienteService.listarTodos());
            cbCliente.setItems(clientes);
            // Configura como o nome do cliente será exibido no ComboBox
            cbCliente.setConverter(new StringConverter<Cliente>() {
                @Override
                public String toString(Cliente cliente) {
                    return cliente != null ? cliente.getNome() : "";
                }

                @Override
                public Cliente fromString(String string) {
                    // Permite buscar o objeto Cliente pelo nome exibido (útil se o ComboBox fosse editável)
                    return cbCliente.getItems().stream()
                            .filter(cliente -> cliente.getNome().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar clientes", "Não foi possível buscar os clientes do banco de dados.\nDetalhes: " + e.getMessage());
            e.printStackTrace(); // Log do erro para depuração
        }
    }

    /**
     * Configura o ComboBox para seleção do Tipo de Pagamento.
     * Popula o ComboBox com os valores do Enum TipoPagamento.
     */
    private void configurarComboBoxTipoPagamento() {
        // Adiciona todos os valores definidos no Enum TipoPagamento ao ComboBox
        cbTipoPagamento.getItems().setAll(TipoPagamento.values());

        // Como o Enum TipoPagamento sobrescreve o método toString() para retornar
        // a descrição amigável, não é estritamente necessário um StringConverter customizado
        // para exibir essa descrição no ComboBox (a menos que precise de lógica 'fromString').
        // Se quisesse usar getDescricao() explicitamente ou o Enum não tivesse toString() sobrescrito:
        /*
        cbTipoPagamento.setConverter(new StringConverter<TipoPagamento>() {
            @Override
            public String toString(TipoPagamento tipo) {
                // Retorna a descrição se o tipo não for nulo, senão retorna null
                return tipo == null ? null : tipo.getDescricao();
            }

            @Override
            public TipoPagamento fromString(String string) {
                // Implementação para encontrar o Enum pela String (útil se o ComboBox fosse editável)
                // Busca na lista de itens do ComboBox pelo tipo cuja descrição corresponde à string fornecida.
                return cbTipoPagamento.getItems().stream()
                        .filter(tp -> tp.getDescricao().equalsIgnoreCase(string)) // Comparação ignorando maiúsculas/minúsculas
                        .findFirst()
                        .orElse(null); // Retorna null se não encontrar correspondência
            }
        });
        */
    }


    /**
     * Configura a TableView para exibir os itens do pedido, define as CellValueFactories,
     * habilita a edição para Preço e Quantidade, e configura a coluna de Ações (Remover).
     */
    private void configurarTableViewItensPedido() {
        // --- Configuração das Colunas ---

        // Coluna ID do Item
        colunaId.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            // Retorna a propriedade do ID do item ou null se o item for inválido
            return item != null ? new SimpleIntegerProperty(item.getId()).asObject() : new SimpleObjectProperty<>(null);
        });

        // Coluna Nome do Item
        colunaNome.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            // Retorna a propriedade do nome do item ou uma mensagem de erro se inválido
            return new SimpleStringProperty(item != null ? item.getNome() : "<Item Inválido>");
        });

        // Coluna Preço de Venda (Editável)
        colunaPrecoVenda.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colunaPrecoVenda.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter())); // Usa TextField para edição
        colunaPrecoVenda.setOnEditCommit(event -> { // Ação ao confirmar a edição
            ItemPedido itemPedido = event.getRowValue();
            if (itemPedido != null) {
                double novoPreco = event.getNewValue() != null ? event.getNewValue() : 0.0;
                if (novoPreco > 0) {
                    itemPedido.setPrecoVenda(novoPreco);
                    atualizarTotais(); // Recalcula totais após mudança de preço
                } else {
                    AlertHelper.showWarning("Preço Inválido", "O preço de venda deve ser maior que zero.");
                    tvItensPedido.refresh(); // Reverte a edição visualmente
                }
            }
        });

        // Coluna Unidade de Medida
        colunaUnidadeMedida.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getUnidadeMedida() : "");
        });

        // Coluna Quantidade (Editável)
        colunaQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colunaQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter())); // Usa TextField para edição
        colunaQuantidade.setOnEditCommit(event -> { // Ação ao confirmar a edição
            ItemPedido itemPedido = event.getRowValue();
            Double novaQuantidade = event.getNewValue();

            if (itemPedido != null && itemPedido.getItem() != null && novaQuantidade != null && novaQuantidade > 0) {
                // Validação de quantidade disponível ANTES de commitar
                if (novaQuantidade > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade Indisponível",
                            "A quantidade solicitada (" + String.format("%.2f", novaQuantidade) + ") para o item '" +
                                    itemPedido.getItem().getNome() + "' excede a quantidade atual disponível (" +
                                    String.format("%.2f", itemPedido.getItem().getQuantidadeAtual()) + ").");
                    tvItensPedido.refresh(); // Reverte a edição visualmente
                } else {
                    itemPedido.setQuantidade(novaQuantidade);
                    atualizarTotais(); // Recalcula totais após mudança de quantidade
                }
            } else {
                // Se valor inválido, item nulo, ou quantidade <= 0
                AlertHelper.showWarning("Entrada Inválida", "A quantidade deve ser um número positivo.");
                tvItensPedido.refresh(); // Reverte a edição visualmente
            }
        });

        // Coluna Quantidade Atual Disponível (Apenas Leitura)
        colunaQtdAtual.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleDoubleProperty(item.getQuantidadeAtual()).asObject() : new SimpleObjectProperty<>(0.0);
        });

        // Coluna Quantidade Estoque Total (Apenas Leitura - Se mantida no FXML)
        colunaQtdEstoque.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return item != null ? new SimpleDoubleProperty(item.getQuantidadeEstoque()).asObject() : new SimpleObjectProperty<>(0.0);
        });

        // Coluna Categoria do Item
        colunaCategoria.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            Categoria categoria = (item != null) ? item.getCategoria() : null;
            return new SimpleStringProperty(categoria != null ? categoria.getNome() : "");
        });

        // Coluna Ações (Botão Remover)
        colunaAcoes.setCellFactory(col -> new TableCell<ItemPedido, Void>() {
            private final Button btnRemover = new Button("Remover");
            {
                // Ação do botão remover
                btnRemover.setOnAction(event -> {
                    // Obtém o ItemPedido da linha atual
                    ItemPedido itemPedidoParaRemover = getTableView().getItems().get(getIndex());
                    if (itemPedidoParaRemover != null) {
                        removerItemPedido(itemPedidoParaRemover); // Chama o método para remover
                    }
                });
                btnRemover.getStyleClass().add("btn-delete"); // Aplica estilo CSS ao botão
                // Considerar adicionar um ícone gráfico se desejar
                // btnRemover.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/delete.png"))));
                // btnRemover.setText(""); // Remover texto se usar ícone
            }

            // Atualiza a célula para exibir o botão ou nada se a linha estiver vazia
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null); // Não mostra nada em linhas vazias
                } else {
                    setGraphic(btnRemover); // Mostra o botão na célula
                }
            }
        });

        // --- Configurações Gerais da Tabela ---
        tvItensPedido.setItems(itensPedido); // Vincula a lista observável à tabela
        tvItensPedido.setEditable(true);     // Permite a edição das células configuradas
        // Define um texto para ser exibido quando a tabela está vazia
        tvItensPedido.setPlaceholder(new Label("Nenhum item adicionado ao pedido."));
    }

    // --- Event Handlers (Ações de Botões e Controles) ---

    /**
     * Abre uma janela modal para selecionar itens a serem adicionados ao pedido.
     */
    @FXML
    private void abrirJanelaAdicionarProdutos() {
        try {
            // Carrega o FXML da janela de seleção de itens
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SelecionarItens.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load())); // Cria a cena com o conteúdo carregado
            stage.setTitle("Seleção de Itens para o Pedido");
            stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia interação com a janela principal
            stage.initOwner(btnAdicionarProdutos.getScene().getWindow()); // Define a janela pai (opcional)

            // Obtém o controlador da janela de seleção
            SelecionarItensController controller = loader.getController();
            // Passa um método de callback para receber os itens selecionados
            controller.setCallback(this::adicionarItensAoPedido);

            stage.showAndWait(); // Exibe a janela e espera ela ser fechada
        } catch (IOException e) {
            AlertHelper.showError("Erro ao Abrir Janela", "Não foi possível carregar a janela de seleção de itens.\nDetalhes: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            AlertHelper.showError("Erro ao Abrir Janela", "Não foi possível encontrar o arquivo FXML '/fxml/SelecionarItens.fxml'. Verifique o caminho.");
            e.printStackTrace();
        }
    }

    /**
     * Adiciona os itens selecionados na janela 'SelecionarItens' à lista de itens do pedido atual.
     * Valida se o item já existe e se há quantidade disponível.
     * @param itensSelecionados A lista de objetos Item que foram selecionados.
     */
    public void adicionarItensAoPedido(List<Item> itensSelecionados) {
        if (itensSelecionados == null) return; // Segurança extra

        for (Item item : itensSelecionados) {
            if (item == null) continue; // Pula itens nulos na lista (pouco provável, mas seguro)

            // Verifica se um ItemPedido com o mesmo Item (baseado no ID) já está na lista 'itensPedido'
            final int itemId = item.getId(); // Para usar dentro da lambda
            boolean jaExiste = itensPedido.stream()
                    .anyMatch(ip -> ip.getItem() != null && ip.getItem().getId() == itemId);

            if (!jaExiste) {
                // Verifica se o item possui quantidade atual disponível para ser adicionado
                if (item.getQuantidadeAtual() <= 0) {
                    AlertHelper.showWarning("Item Sem Quantidade", "O item '" + item.getNome() + "' não possui quantidade disponível (Qtd Atual: " + String.format("%.2f",item.getQuantidadeAtual()) + ") e não pode ser adicionado.");
                    continue; // Pula para o próximo item da seleção
                }

                // Cria um novo ItemPedido
                ItemPedido novoItemPedido = new ItemPedido();
                novoItemPedido.setItem(item); // Associa o Item selecionado
                // Define quantidade inicial como 1 (ou outra lógica, se necessário)
                // Poderia ser Math.min(1.0, item.getQuantidadeAtual()) se quisesse limitar à qtd disponível já aqui
                novoItemPedido.setQuantidade(1.0);
                // Define o preço de venda padrão do item (pode ser editado depois na tabela)
                novoItemPedido.setPrecoVenda(item.getPrecoVenda());

                // Validação final: A quantidade inicial (1.0) não pode exceder a disponível
                if (novoItemPedido.getQuantidade() > item.getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade Insuficiente",
                            "Não há quantidade suficiente para adicionar o item '" + item.getNome() +
                                    "'. Disponível: " + String.format("%.2f", item.getQuantidadeAtual()) + ", Necessário: 1.00");
                    continue; // Pula para o próximo item
                }

                itensPedido.add(novoItemPedido); // Adiciona o novo item à lista observável (atualiza a tabela)

            } else {
                // Informa ao usuário que o item já foi adicionado
                AlertHelper.showInfo("Item Já Adicionado", "O item '" + item.getNome() + "' já está na lista deste pedido.");
            }
        }
        atualizarTotais(); // Recalcula e atualiza os labels de totais
    }

    /**
     * Remove um ItemPedido da lista observável e atualiza os totais.
     * @param itemPedido O ItemPedido a ser removido.
     */
    private void removerItemPedido(ItemPedido itemPedido) {
        if (itemPedido != null) {
            itensPedido.remove(itemPedido); // Remove da lista observável (atualiza a tabela)
            atualizarTotais(); // Recalcula e atualiza os labels de totais
        }
    }

    /**
     * Calcula e atualiza os labels de Quantidade Total e Valor Total do pedido.
     */
    private void atualizarTotais() {
        double valorTotal = calcularValorTotalPedido();
        // Usa double para quantidade total para consistência, já que ItemPedido.quantidade é double
        double quantidadeTotal = itensPedido.stream()
                .mapToDouble(ItemPedido::getQuantidade)
                .sum();

        // Atualiza os textos dos Labels formatando os números
        lblValorTotal.setText("Valor Total: R$ " + String.format("%.2f", valorTotal));
        lblQuantidadeTotal.setText("Quantidade Total: " + String.format("%.2f", quantidadeTotal));
    }

    /**
     * Calcula o valor total do pedido somando (quantidade * precoVenda) de cada ItemPedido.
     * @return O valor total calculado.
     */
    private double calcularValorTotalPedido() {
        if (itensPedido == null) {
            return 0.0;
        }
        // Stream para processar a lista de itens do pedido
        return itensPedido.stream()
                // Mapeia cada ItemPedido para o seu subtotal (quantidade * preço de venda)
                .mapToDouble(ip -> {
                    // Verificação de segurança para evitar NullPointerException
                    if (ip != null) {
                        return ip.getQuantidade() * ip.getPrecoVenda();
                    }
                    return 0.0; // Retorna 0 se o ItemPedido for nulo
                })
                // Soma todos os subtotais calculados
                .sum();
    }


    /**
     * Valida os dados, monta o objeto Pedido e chama o PedidoService para salvar (novo)
     * ou atualizar (edição) o pedido no banco de dados.
     * Realiza validações de campos obrigatórios e de quantidade disponível.
     */
    @FXML
    private void salvarPedido() {
        try {
            // --- Validações de Campos Obrigatórios ---
            if (cbCliente.getValue() == null) {
                AlertHelper.showWarning("Campo Obrigatório", "Por favor, selecione um Cliente.");
                cbCliente.requestFocus(); // Foca no campo inválido
                return;
            }
            if (dpDataPedido.getValue() == null) {
                AlertHelper.showWarning("Campo Obrigatório", "Por favor, selecione a Data do Pedido.");
                dpDataPedido.requestFocus();
                return;
            }
            // Adicionar validação para Tipo de Pagamento se for obrigatório
            // if (cbTipoPagamento.getValue() == null) {
            //    AlertHelper.showWarning("Campo Obrigatório", "Por favor, selecione a Forma de Pagamento.");
            //    cbTipoPagamento.requestFocus();
            //    return;
            // }
            if (itensPedido.isEmpty()) {
                AlertHelper.showWarning("Pedido Vazio", "Adicione pelo menos um item ao pedido antes de salvar.");
                btnAdicionarProdutos.requestFocus();
                return;
            }

            // --- Obtenção dos Dados da Interface ---
            Cliente clienteSelecionado = cbCliente.getValue();
            LocalDate dataPedidoSelecionada = dpDataPedido.getValue();
            LocalDate dataRetornoSelecionada = dpDataRetorno.getValue(); // Pode ser null
            TipoPagamento tipoPagamentoSelecionado = cbTipoPagamento.getValue(); // <<< OBTÉM O VALOR
            String observacoesDigitadas = txtObservacoes.getText();
            // Cria uma cópia da lista de itens atual para evitar problemas com concorrência se a lista for modificada externamente
            List<ItemPedido> itensAtuaisDoPedido = itensPedido.stream().collect(Collectors.toList());


            // --- Validações de Negócio (Itens) ---
            for (ItemPedido itemPedido : itensAtuaisDoPedido) {
                if (itemPedido == null || itemPedido.getItem() == null) {
                    AlertHelper.showError("Erro Interno", "O pedido contém um item inválido (nulo). Verifique os itens adicionados.");
                    return; // Impede o salvamento
                }
                // Verifica se a quantidade no pedido excede a quantidade atual DO ITEM NO ESTOQUE
                // Esta validação é crucial para garantir consistência
                if (itemPedido.getQuantidade() > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showError("Quantidade Insuficiente em Estoque",
                            "Não há quantidade atual suficiente para o item: '" + itemPedido.getItem().getNome() +
                                    "'.\nPedido: " + String.format("%.2f", itemPedido.getQuantidade()) +
                                    ", Disponível: " + String.format("%.2f", itemPedido.getItem().getQuantidadeAtual()));
                    // Poderia focar na tabela e na linha/célula problemática se a API permitir facilmente
                    tvItensPedido.requestFocus();
                    return; // Impede o salvamento
                }
                // Validação adicional: Quantidade e Preço devem ser positivos
                if (itemPedido.getQuantidade() <= 0) {
                    AlertHelper.showError("Quantidade Inválida",
                            "A quantidade para o item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
                    tvItensPedido.requestFocus();
                    return;
                }
                if (itemPedido.getPrecoVenda() <= 0) {
                    AlertHelper.showError("Preço Inválido",
                            "O preço de venda para o item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
                    tvItensPedido.requestFocus();
                    return;
                }
            }

            // --- Lógica de Persistência: Atualizar ou Salvar Novo ---
            double valorTotalCalculado = calcularValorTotalPedido(); // Recalcula por segurança

            if (pedidoSendoEditado != null) {
                // --- Modo Edição: Atualizar Pedido Existente ---
                pedidoSendoEditado.setCliente(clienteSelecionado);
                pedidoSendoEditado.setDataPedido(dataPedidoSelecionada);
                pedidoSendoEditado.setDataRetorno(dataRetornoSelecionada);
                pedidoSendoEditado.setTipoPagamento(tipoPagamentoSelecionado); // <<< DEFINE O VALOR
                pedidoSendoEditado.setObservacoes(observacoesDigitadas);
                // Garante que a lista de itens no objeto Pedido a ser salvo esteja atualizada
                pedidoSendoEditado.setItens(itensAtuaisDoPedido);
                pedidoSendoEditado.setValorTotal(valorTotalCalculado);
                // Nota: O Status do pedido geralmente não é modificado nesta tela de registro/edição,
                // a menos que haja uma regra de negócio específica. A conclusão ou cancelamento
                // seriam feitos em outra funcionalidade. O status atual é mantido.

                // Chama os serviços para persistir as alterações
                pedidoService.atualizarPedido(pedidoSendoEditado); // Atualiza os dados principais do pedido
                pedidoService.atualizarItens(pedidoSendoEditado);  // Atualiza a lista de itens (Serviço lida com a lógica de estoque)

                AlertHelper.showSuccess( "O pedido ID " + pedidoSendoEditado.getId() + " foi atualizado com sucesso.");

            } else {
                // --- Modo Novo: Criar e Salvar Novo Pedido ---
                Pedido novoPedido = new Pedido();
                novoPedido.setCliente(clienteSelecionado);
                novoPedido.setDataPedido(dataPedidoSelecionada);
                novoPedido.setDataRetorno(dataRetornoSelecionada); // Define data de retorno (pode ser null)
                novoPedido.setTipoPagamento(tipoPagamentoSelecionado); // <<< DEFINE O VALOR
                novoPedido.setObservacoes(observacoesDigitadas);
                novoPedido.setItens(itensAtuaisDoPedido); // Define a lista de itens
                novoPedido.setValorTotal(valorTotalCalculado); // Define o valor total calculado
                novoPedido.setStatus(StatusPedido.EM_ANDAMENTO); // Define o status inicial padrão

                // Chama os serviços para persistir o novo pedido
                pedidoService.salvarPedido(novoPedido); // Salva o pedido (obtém o ID gerado)
                pedidoService.salvarItens(novoPedido);  // Salva os itens associados e deduz a quantidade atual (Serviço faz isso)

                AlertHelper.showSuccess( "O novo pedido foi registrado com sucesso com ID " + novoPedido.getId() + ".");
            }

            // Limpa a tela após salvar/atualizar com sucesso
            limparCampos();

        } catch (Exception e) {
            // Captura qualquer exceção inesperada durante o processo
            AlertHelper.showError("Erro ao Salvar Pedido", "Ocorreu um erro inesperado ao tentar salvar o pedido:\n" + e.getMessage());
            e.printStackTrace(); // Log detalhado do erro para o console/arquivo de log
        }
    }

    /**
     * Ação do botão Cancelar. Limpa os campos e reseta o estado da tela.
     */
    @FXML
    private void cancelar() {
        // Pergunta ao usuário se realmente deseja cancelar, especialmente se houver dados não salvos (opcional)
        // boolean confirmar = AlertHelper.showConfirmation("Cancelar Alterações", "Deseja realmente cancelar e limpar os campos?");
        // if (confirmar) {
        limparCampos();
        // }
    }

    // --- Métodos Auxiliares ---

    /**
     * Limpa todos os campos de entrada da interface, a lista de itens
     * e reseta a variável de estado de edição.
     */
    private void limparCampos() {
        cbCliente.setValue(null);
        dpDataPedido.setValue(LocalDate.now()); // Reseta para data atual
        dpDataRetorno.setValue(null);          // Limpa data de retorno
        cbTipoPagamento.setValue(null);        // <<< LIMPA TIPO DE PAGAMENTO
        txtObservacoes.clear();
        itensPedido.clear();                   // Limpa a lista observável (limpa a tabela)
        atualizarTotais();                     // Reseta os labels de total para R$ 0,00 e Qtd 0.00
        pedidoSendoEditado = null;             // Garante que a próxima ação de salvar seja "novo", não "edição"
        cbCliente.requestFocus();              // Devolve o foco para o primeiro campo (opcional)
    }

    /**
     * Preenche os campos da interface com os dados de um Pedido existente.
     * Usado quando a tela é aberta em modo de edição.
     * @param pedido O objeto Pedido cujos dados serão exibidos para edição.
     */
    public void preencherDadosPedido(Pedido pedido) {
        if (pedido == null) {
            // Se um pedido nulo for passado, apenas limpa a tela
            System.err.println("Aviso: Tentativa de preencher dados com um pedido nulo.");
            limparCampos();
            return;
        }
        // Armazena a referência ao pedido que está sendo editado
        pedidoSendoEditado = pedido;

        // --- Preenche os campos da interface ---
        // É importante que o objeto Cliente dentro do Pedido esteja completamente carregado.
        // Se o ComboBox usa objetos Cliente, o objeto exato precisa estar na lista do ComboBox
        // ou a seleção por objeto pode falhar visualmente. A busca por ID seria mais robusta se necessário.
        cbCliente.setValue(pedido.getCliente());
        dpDataPedido.setValue(pedido.getDataPedido());
        dpDataRetorno.setValue(pedido.getDataRetorno());         // Preenche data de retorno
        cbTipoPagamento.setValue(pedido.getTipoPagamento());     // <<< PREENCHE TIPO DE PAGAMENTO
        txtObservacoes.setText(pedido.getObservacoes());

        // --- Preenche a tabela de itens ---
        itensPedido.clear(); // Limpa itens possivelmente existentes de uma operação anterior
        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            // Adiciona todos os itens do pedido à lista observável.
            // IMPORTANTE: Garanta que os objetos 'Item' dentro de 'ItemPedido'
            // contenham a 'quantidadeAtual' CORRETA do estoque no momento do carregamento.
            // O PedidoDAOImpl e ItemPedidoDAOImpl devem garantir isso ao buscar o pedido.
            itensPedido.addAll(pedido.getItens());
        } else {
            // Log ou aviso se a lista de itens do pedido estiver vazia ou nula.
            System.out.println("Info: Pedido ID " + pedido.getId() + " carregado para edição sem itens ou com lista de itens nula.");
        }

        // Atualiza os labels de totais com base nos itens carregados
        atualizarTotais();

        // (Opcional) Mudar o texto do botão salvar para "Atualizar Pedido"
        btnSalvar.setText("Atualizar Pedido");
    }

    // Poderiam existir outros métodos conforme a necessidade,
    // como validações específicas, formatações, etc.

}