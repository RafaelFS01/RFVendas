package BackEnd.controller;

import BackEnd.model.entity.*; // Importa todas as entidades necessárias
import BackEnd.model.service.ClienteService;
import BackEnd.model.service.ItemService; // Manter se usado para buscar Item em algum ponto futuro
import BackEnd.model.service.PedidoService;
import BackEnd.util.AlertHelper;
import BackEnd.util.PedidoPdfGenerator; // <<< IMPORT ADICIONADO para o gerador de PDF
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
import javafx.stage.FileChooser; // <<< IMPORT ADICIONADO para salvar arquivo
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter; // Import necessário para o converter do ComboBox
import javafx.util.converter.DoubleStringConverter;

import java.io.File; // <<< IMPORT ADICIONADO para File
import java.io.IOException; // <<< IMPORT ADICIONADO para IOException
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrarPedidoController {

    // --- FXML Element Declarations ---
    @FXML private ComboBox<Cliente> cbCliente;
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
            return item != null ? new SimpleIntegerProperty(item.getId()).asObject() : new SimpleObjectProperty<>(null);
        });

        // Coluna Nome do Item
        colunaNome.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getNome() : "<Item Inválido>");
        });

        // Coluna Preço de Venda (Editável)
        colunaPrecoVenda.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colunaPrecoVenda.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaPrecoVenda.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            if (itemPedido != null) {
                double novoPreco = event.getNewValue() != null ? event.getNewValue() : 0.0;
                if (novoPreco > 0) {
                    itemPedido.setPrecoVenda(novoPreco);
                    atualizarTotais();
                } else {
                    AlertHelper.showWarning("Preço Inválido", "O preço de venda deve ser maior que zero.");
                    tvItensPedido.refresh();
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
        colunaQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colunaQuantidade.setOnEditCommit(event -> {
            ItemPedido itemPedido = event.getRowValue();
            Double novaQuantidade = event.getNewValue();

            if (itemPedido != null && itemPedido.getItem() != null && novaQuantidade != null && novaQuantidade > 0) {
                if (novaQuantidade > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade Indisponível",
                            "A quantidade solicitada (" + String.format("%.2f", novaQuantidade) + ") para o item '" +
                                    itemPedido.getItem().getNome() + "' excede a quantidade atual disponível (" +
                                    String.format("%.2f", itemPedido.getItem().getQuantidadeAtual()) + ").");
                    tvItensPedido.refresh();
                } else {
                    itemPedido.setQuantidade(novaQuantidade);
                    atualizarTotais();
                }
            } else {
                AlertHelper.showWarning("Entrada Inválida", "A quantidade deve ser um número positivo.");
                tvItensPedido.refresh();
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
                btnRemover.setOnAction(event -> {
                    ItemPedido itemPedidoParaRemover = getTableView().getItems().get(getIndex());
                    if (itemPedidoParaRemover != null) {
                        removerItemPedido(itemPedidoParaRemover);
                    }
                });
                btnRemover.getStyleClass().add("btn-delete");
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

        // --- Configurações Gerais da Tabela ---
        tvItensPedido.setItems(itensPedido);
        tvItensPedido.setEditable(true);
        tvItensPedido.setPlaceholder(new Label("Nenhum item adicionado ao pedido."));
    }

    // --- Event Handlers (Ações de Botões e Controles) ---

    /**
     * Abre uma janela modal para selecionar itens a serem adicionados ao pedido.
     */
    @FXML
    private void abrirJanelaAdicionarProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SelecionarItens.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Seleção de Itens para o Pedido");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(btnAdicionarProdutos.getScene().getWindow());

            SelecionarItensController controller = loader.getController();
            controller.setCallback(this::adicionarItensAoPedido);

            stage.showAndWait();
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
        if (itensSelecionados == null) return;

        for (Item item : itensSelecionados) {
            if (item == null) continue;

            final int itemId = item.getId();
            boolean jaExiste = itensPedido.stream()
                    .anyMatch(ip -> ip.getItem() != null && ip.getItem().getId() == itemId);

            if (!jaExiste) {
                if (item.getQuantidadeAtual() <= 0) {
                    AlertHelper.showWarning("Item Sem Quantidade", "O item '" + item.getNome() + "' não possui quantidade disponível (Qtd Atual: " + String.format("%.2f",item.getQuantidadeAtual()) + ") e não pode ser adicionado.");
                    continue;
                }

                ItemPedido novoItemPedido = new ItemPedido();
                novoItemPedido.setItem(item);
                novoItemPedido.setQuantidade(1.0);
                novoItemPedido.setPrecoVenda(item.getPrecoVenda());

                if (novoItemPedido.getQuantidade() > item.getQuantidadeAtual()) {
                    AlertHelper.showWarning("Quantidade Insuficiente",
                            "Não há quantidade suficiente para adicionar o item '" + item.getNome() +
                                    "'. Disponível: " + String.format("%.2f", item.getQuantidadeAtual()) + ", Necessário: 1.00");
                    continue;
                }

                itensPedido.add(novoItemPedido);

            } else {
                AlertHelper.showInfo("Item Já Adicionado", "O item '" + item.getNome() + "' já está na lista deste pedido.");
            }
        }
        atualizarTotais();
    }

    /**
     * Remove um ItemPedido da lista observável e atualiza os totais.
     * @param itemPedido O ItemPedido a ser removido.
     */
    private void removerItemPedido(ItemPedido itemPedido) {
        if (itemPedido != null) {
            itensPedido.remove(itemPedido);
            atualizarTotais();
        }
    }

    /**
     * Calcula e atualiza os labels de Quantidade Total e Valor Total do pedido.
     */
    private void atualizarTotais() {
        double valorTotal = calcularValorTotalPedido();
        double quantidadeTotal = itensPedido.stream()
                .mapToDouble(ItemPedido::getQuantidade)
                .sum();

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
        return itensPedido.stream()
                .mapToDouble(ip -> {
                    if (ip != null) {
                        return ip.getQuantidade() * ip.getPrecoVenda();
                    }
                    return 0.0;
                })
                .sum();
    }


    /**
     * Valida os dados, monta o objeto Pedido, chama o PedidoService para salvar/atualizar,
     * e então dispara a geração do PDF do pedido salvo/atualizado.
     */
    @FXML
    private void salvarPedido() {
        Pedido novoPedido = null; // <<< Variável para guardar referência do novo pedido para o PDF

        try {
            // --- Validações de Campos Obrigatórios ---
            if (cbCliente.getValue() == null) {
                AlertHelper.showWarning("Campo Obrigatório", "Por favor, selecione um Cliente.");
                cbCliente.requestFocus();
                return;
            }
            if (dpDataPedido.getValue() == null) {
                AlertHelper.showWarning("Campo Obrigatório", "Por favor, selecione a Data do Pedido.");
                dpDataPedido.requestFocus();
                return;
            }
            // Validação opcional para Tipo de Pagamento:
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
            LocalDate dataRetornoSelecionada = dpDataRetorno.getValue();
            TipoPagamento tipoPagamentoSelecionado = cbTipoPagamento.getValue();
            String observacoesDigitadas = txtObservacoes.getText();
            List<ItemPedido> itensAtuaisDoPedido = itensPedido.stream().collect(Collectors.toList());

            // --- Validações de Negócio (Itens) ---
            for (ItemPedido itemPedido : itensAtuaisDoPedido) {
                if (itemPedido == null || itemPedido.getItem() == null) {
                    AlertHelper.showError("Erro Interno", "O pedido contém um item inválido (nulo). Verifique os itens adicionados.");
                    return;
                }
                if (itemPedido.getQuantidade() > itemPedido.getItem().getQuantidadeAtual()) {
                    AlertHelper.showError("Quantidade Insuficiente em Estoque",
                            "Não há quantidade atual suficiente para o item: '" + itemPedido.getItem().getNome() +
                                    "'.\nPedido: " + String.format("%.2f", itemPedido.getQuantidade()) +
                                    ", Disponível: " + String.format("%.2f", itemPedido.getItem().getQuantidadeAtual()));
                    tvItensPedido.requestFocus();
                    return;
                }
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
            double valorTotalCalculado = calcularValorTotalPedido();

            if (pedidoSendoEditado != null) {
                // --- Modo Edição: Atualizar Pedido Existente ---
                pedidoSendoEditado.setCliente(clienteSelecionado);
                pedidoSendoEditado.setDataPedido(dataPedidoSelecionada);
                pedidoSendoEditado.setDataRetorno(dataRetornoSelecionada);
                pedidoSendoEditado.setTipoPagamento(tipoPagamentoSelecionado);
                pedidoSendoEditado.setObservacoes(observacoesDigitadas);
                pedidoSendoEditado.setItens(itensAtuaisDoPedido);
                pedidoSendoEditado.setValorTotal(valorTotalCalculado);
                // Status é mantido

                pedidoService.atualizarPedido(pedidoSendoEditado);
                pedidoService.atualizarItens(pedidoSendoEditado); // Serviço deduz estoque ao atualizar

                AlertHelper.showSuccess("O pedido ID " + pedidoSendoEditado.getId() + " foi atualizado com sucesso.");

            } else {
                // --- Modo Novo: Criar e Salvar Novo Pedido ---
                novoPedido = new Pedido(); // <<< Atribui à variável local
                novoPedido.setCliente(clienteSelecionado);
                novoPedido.setDataPedido(dataPedidoSelecionada);
                novoPedido.setDataRetorno(dataRetornoSelecionada);
                novoPedido.setTipoPagamento(tipoPagamentoSelecionado);
                novoPedido.setObservacoes(observacoesDigitadas);
                novoPedido.setItens(itensAtuaisDoPedido);
                novoPedido.setValorTotal(valorTotalCalculado);
                novoPedido.setStatus(StatusPedido.EM_ANDAMENTO);

                pedidoService.salvarPedido(novoPedido); // Salva dados principais (obtém ID)
                pedidoService.salvarItens(novoPedido);  // Salva itens e deduz estoque

                AlertHelper.showSuccess("O novo pedido foi registrado com sucesso com ID " + novoPedido.getId() + ".");
            }

            // ---- Geração do PDF ----
            // Garante que temos um pedido válido (seja o editado ou o recém-criado)
            if (pedidoSendoEditado != null || novoPedido != null) {
                Pedido pedidoParaPdf = (pedidoSendoEditado != null) ? pedidoSendoEditado : novoPedido;

                // 1. Escolher o local para salvar
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Salvar PDF do Pedido");
                // Sugerir nome de arquivo seguro
                String initialFileName = String.format("Pedido_%d_%s.pdf",
                        pedidoParaPdf.getId(),
                        pedidoParaPdf.getCliente().getNome().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_")); // Remove caracteres inválidos
                fileChooser.setInitialFileName(initialFileName);
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

                // Mostrar diálogo para salvar (precisa de uma referência ao Stage atual)
                Stage stage = (Stage) btnSalvar.getScene().getWindow(); // Obtem o Stage do botão
                File selectedFile = fileChooser.showSaveDialog(stage);

                if (selectedFile != null) {
                    String filePath = selectedFile.getAbsolutePath();
                    try {
                        PedidoPdfGenerator pdfGenerator = new PedidoPdfGenerator();
                        // Chama o gerador passando o pedido e o caminho do arquivo
                        pdfGenerator.generatePdf(pedidoParaPdf, filePath);

                        AlertHelper.showSuccess("PDF do pedido gerado com sucesso em:\n" + filePath);

                        // Opcional: Abrir o PDF gerado (pode precisar de tratamento de exceção específico)
                        // try {
                        //     if (Desktop.isDesktopSupported()) {
                        //         Desktop.getDesktop().open(selectedFile);
                        //     } else {
                        //         AlertHelper.showWarning("Ação não suportada", "Não foi possível abrir o arquivo PDF automaticamente.");
                        //     }
                        // } catch (IOException openEx) {
                        //      AlertHelper.showError("Erro ao Abrir PDF", "Não foi possível abrir o arquivo PDF gerado.\n" + openEx.getMessage());
                        // }

                    } catch (IOException ioEx) {
                        AlertHelper.showError("Erro ao Gerar PDF", "Não foi possível gerar ou salvar o arquivo PDF.\nVerifique as permissões e o caminho.\nDetalhes: " + ioEx.getMessage());
                        ioEx.printStackTrace();
                    } catch (IllegalArgumentException argEx) {
                        AlertHelper.showError("Erro ao Gerar PDF", "Dados inválidos para gerar o PDF.\nDetalhes: " + argEx.getMessage());
                        argEx.printStackTrace();
                    } catch (Exception pdfEx) {
                        AlertHelper.showError("Erro Inesperado no PDF", "Ocorreu um erro inesperado ao gerar o PDF.\nDetalhes: " + pdfEx.getMessage());
                        pdfEx.printStackTrace();
                    }
                } else {
                    // Usuário cancelou a seleção do arquivo
                    AlertHelper.showInfo("Geração de PDF Cancelada", "A geração do PDF foi cancelada pelo usuário.");
                }
            }
            // ---- Fim da Geração do PDF ----


            // Limpa a tela após salvar/atualizar com sucesso E gerar/cancelar PDF
            limparCampos();

        } catch (Exception e) {
            // Captura qualquer exceção inesperada durante o processo de salvar pedido ou itens
            AlertHelper.showError("Erro ao Salvar Pedido", "Ocorreu um erro inesperado ao tentar salvar o pedido:\n" + e.getMessage());
            e.printStackTrace(); // Log detalhado do erro para o console/arquivo de log
        }
    }

    /**
     * Ação do botão Cancelar. Limpa os campos e reseta o estado da tela.
     */
    @FXML
    private void cancelar() {
        // Opcional: Adicionar confirmação
        // boolean confirmar = AlertHelper.showConfirmation("Cancelar Alterações", "Deseja realmente cancelar e limpar os campos?", "").orElse(ButtonType.NO) == ButtonType.YES;
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
        dpDataPedido.setValue(LocalDate.now());
        dpDataRetorno.setValue(null);
        cbTipoPagamento.setValue(null); // <<< LIMPA TIPO DE PAGAMENTO
        txtObservacoes.clear();
        itensPedido.clear();
        atualizarTotais();
        pedidoSendoEditado = null;
        btnSalvar.setText("Salvar Pedido"); // Garante que o botão volte ao texto original
        cbCliente.requestFocus();
    }

    /**
     * Preenche os campos da interface com os dados de um Pedido existente.
     * Usado quando a tela é aberta em modo de edição.
     * @param pedido O objeto Pedido cujos dados serão exibidos para edição.
     */
    public void preencherDadosPedido(Pedido pedido) {
        if (pedido == null) {
            System.err.println("Aviso: Tentativa de preencher dados com um pedido nulo.");
            limparCampos();
            return;
        }
        pedidoSendoEditado = pedido;

        // --- Preenche os campos da interface ---
        cbCliente.setValue(pedido.getCliente());
        dpDataPedido.setValue(pedido.getDataPedido());
        dpDataRetorno.setValue(pedido.getDataRetorno());
        cbTipoPagamento.setValue(pedido.getTipoPagamento()); // <<< PREENCHE TIPO DE PAGAMENTO
        txtObservacoes.setText(pedido.getObservacoes());

        // --- Preenche a tabela de itens ---
        itensPedido.clear();
        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            // IMPORTANTE: Garanta que os objetos Item dentro de ItemPedido
            // contenham a 'quantidadeAtual' CORRETA do estoque no momento do carregamento.
            itensPedido.addAll(pedido.getItens());
        } else {
            System.out.println("Info: Pedido ID " + pedido.getId() + " carregado para edição sem itens.");
        }

        atualizarTotais();

        btnSalvar.setText("Atualizar Pedido"); // Muda o texto do botão para indicar edição
    }

}