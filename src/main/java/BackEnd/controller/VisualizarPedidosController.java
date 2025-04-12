package BackEnd.controller;

import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Dependencia;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.ItemPedido;
import BackEnd.model.entity.Pedido;
import BackEnd.model.service.CategoriaService;
import BackEnd.model.service.DependenciaService;
import BackEnd.model.service.ItemService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VisualizarPedidosController {

    @FXML private TableView<ItemResumo> tabelaResumo;
    @FXML private TableColumn<ItemResumo, Integer> colunaResumoId;
    @FXML private TableColumn<ItemResumo, String> colunaResumoNome;
    @FXML private TableColumn<ItemResumo, Double> colunaResumoPrecoVenda;
    @FXML private TableColumn<ItemResumo, String> colunaResumoUnidadeMedida;
    @FXML private TableColumn<ItemResumo, Double> colunaResumoQuantidade;
    @FXML private TableColumn<ItemResumo, Double> colunaResumoQtdAtual;
    @FXML private TableColumn<ItemResumo, Double> colunaResumoQtdEstoque;
    @FXML private TableColumn<ItemResumo, String> colunaResumoCategoria;
    @FXML private Label labelValorTotal;
    @FXML private Label labelQuantidadeTotal;
    @FXML private VBox containerTabelasIndividuais;
    @FXML private ComboBox<String> cbFiltroCategoria;
    @FXML private TableView<DependenciaResumo> tabelaDependencias;
    @FXML private TableColumn<DependenciaResumo, Integer> colunaDependenciaId;
    @FXML private TableColumn<DependenciaResumo, String> colunaDependenciaNome;
    @FXML private TableColumn<DependenciaResumo, String> colunaDependenteNome;
    @FXML private TableColumn<DependenciaResumo, Double> colunaQtdDependente;
    @FXML private TableColumn<DependenciaResumo, Double> colunaQtdDependencia;
    @FXML private TableColumn<DependenciaResumo, String> colunaDependenciaUnidadeMedida;
    @FXML private TableColumn<DependenciaResumo, String> colunaDependenciaCategoria;
    @FXML private TableView<ItemFaltanteResumo> tabelaResumoFaltantes;
    @FXML private TableColumn<ItemFaltanteResumo, Integer> colunaFaltantesId;
    @FXML private TableColumn<ItemFaltanteResumo, String> colunaFaltantesNome;
    @FXML private TableColumn<ItemFaltanteResumo, Double> colunaFaltantesQuantidade;
    @FXML private TableColumn<ItemFaltanteResumo, String> colunaFaltantesUnidadeMedida;
    @FXML private TableColumn<ItemFaltanteResumo, String> colunaFaltantesCategoria;

    private ObservableList<Pedido> pedidosSelecionados;
    private ObservableList<ItemResumo> itensResumo = FXCollections.observableArrayList();
    private ObservableList<DependenciaResumo> dependenciasResumo = FXCollections.observableArrayList();
    private ObservableList<ItemFaltanteResumo> itensFaltantesResumo = FXCollections.observableArrayList();

    private ItemService itemService;
    private DependenciaService dependenciaService;
    private CategoriaService categoriaService;

    public void initialize() {
        itemService = new ItemService();
        dependenciaService = new DependenciaService();
        categoriaService = new CategoriaService();
        configurarColunasTabelaResumo();
        configurarColunasTabelaDependencias();
        configurarColunasTabelaResumoFaltantes();
        carregarCategorias();
        cbFiltroCategoria.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                atualizarTabelaDependencias();
            }
        });
    }

    public void setPedidos(List<Pedido> pedidos) {
        this.pedidosSelecionados = FXCollections.observableArrayList(pedidos);
        preencherTabelaResumo();
        preencherTabelaResumoFaltantes();
        criarTabelasPedidosIndividuais();
        preencherTabelasPedidosIndividuais();
        calcularTotais();
        cbFiltroCategoria.setValue("Todas as Categorias");
        atualizarTabelaDependencias();
    }

    private void configurarColunasTabelaResumo() {
        colunaResumoId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colunaResumoNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
        colunaResumoPrecoVenda.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrecoVenda()).asObject());
        colunaResumoUnidadeMedida.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUnidadeMedida()));
        colunaResumoQuantidade.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQuantidade()).asObject());
        colunaResumoQtdAtual.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQtdAtual()).asObject());
        colunaResumoQtdEstoque.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQtdEstoque()).asObject());
        colunaResumoCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoria()));
    }

    private void configurarColunasTabelaDependencias() {
        colunaDependenciaId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colunaDependenciaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomeDependencia()));
        colunaDependenteNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomeDependente()));
        colunaQtdDependente.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQtdDependente()).asObject());
        colunaQtdDependencia.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQtdDependencia()).asObject());
        colunaDependenciaUnidadeMedida.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUnidadeMedida()));
        colunaDependenciaCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoria()));
    }

    private void configurarColunasTabelaResumoFaltantes() {
        colunaFaltantesId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colunaFaltantesNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
        colunaFaltantesQuantidade.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQuantidade()).asObject());
        colunaFaltantesUnidadeMedida.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUnidadeMedida()));
        colunaFaltantesCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoria()));
    }

    private void carregarCategorias() {
        try {
            List<Categoria> categorias = categoriaService.listarCategorias();
            List<String> nomesCategorias = categorias.stream()
                    .map(Categoria::getNome)
                    .collect(Collectors.toList());
            nomesCategorias.add(0, "Todas as Categorias");
            cbFiltroCategoria.setItems(FXCollections.observableArrayList(nomesCategorias));
        } catch (Exception e) {
            e.printStackTrace();
            // Tratar a exceção (e.g., mostrar mensagem de erro)
        }
    }

    private void atualizarTabelaDependencias() {
        dependenciasResumo.clear();
        String categoriaSelecionada = cbFiltroCategoria.getValue();
        dependenciasResumo.addAll(calcularDependenciasNecessarias(categoriaSelecionada));
        tabelaDependencias.setItems(dependenciasResumo);
    }

    private List<DependenciaResumo> calcularDependenciasNecessarias(String categoria) {
        Map<Integer, DependenciaResumo> dependenciasMap = new HashMap<>();

        // Usar os itens da tabela de resumo de faltantes
        for (ItemFaltanteResumo itemFaltante : itensFaltantesResumo) {
            try {
                List<Dependencia> dependencias = dependenciaService.buscarPorIdItemDependente(itemFaltante.getId());
                for (Dependencia dependencia : dependencias) {
                    Item itemDependente = itemService.buscarItemPorId(dependencia.getIdItemDependente());
                    Item itemNecessario = itemService.buscarItemPorId(dependencia.getIdItemNecessario());

                    // Verifica se a categoria do item NECESSÁRIO corresponde à categoria selecionada
                    if (categoria.equals("Todas as Categorias") || itemNecessario.getCategoria().getNome().equals(categoria)) {
                        DependenciaResumo depResumo = dependenciasMap.computeIfAbsent(dependencia.getId(),
                                id -> new DependenciaResumo(dependencia, itemDependente, itemNecessario));

                        // Usar a quantidade da tabela de resumo de faltantes
                        depResumo.setQtdDependente(itemFaltante.getQuantidade());
                        depResumo.setQtdDependencia(itemFaltante.getQuantidade() * dependencia.getQuantidade());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Tratar a exceção
            }
        }

        return new ArrayList<>(dependenciasMap.values());
    }

    private void preencherTabelaResumo() {
        Map<Integer, ItemResumo> itensAgrupados = new HashMap<>();

        for (Pedido pedido : pedidosSelecionados) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                int idItem = itemPedido.getItem().getId();
                if (itensAgrupados.containsKey(idItem)) {
                    ItemResumo itemResumo = itensAgrupados.get(idItem);
                    itemResumo.setQuantidade(itemResumo.getQuantidade() + itemPedido.getQuantidade());
                } else {
                    ItemResumo itemResumo = new ItemResumo(itemPedido.getItem());
                    itemResumo.setQuantidade(itemPedido.getQuantidade());
                    itensAgrupados.put(idItem, itemResumo);
                }
            }
        }

        itensResumo.addAll(itensAgrupados.values());
        tabelaResumo.setItems(itensResumo);
    }

    private void preencherTabelaResumoFaltantes() {
        itensFaltantesResumo.clear();
        itensFaltantesResumo.addAll(calcularItensFaltantes());
        tabelaResumoFaltantes.setItems(itensFaltantesResumo);
    }

    private List<ItemFaltanteResumo> calcularItensFaltantes() {
        Map<Integer, ItemResumo> itensAgrupados = new HashMap<>();

        for (Pedido pedido : pedidosSelecionados) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                Item item = itemPedido.getItem();
                int idItem = item.getId();

                if (itensAgrupados.containsKey(idItem)) {
                    ItemResumo itemResumo = itensAgrupados.get(idItem);
                    itemResumo.calcularQuantidade(itemPedido.getQuantidade());
                } else {
                    ItemResumo itemResumo = new ItemResumo(item);
                    itemResumo.calcularQuantidade(itemPedido.getQuantidade());
                    itensAgrupados.put(idItem, itemResumo);
                }
            }
        }

        // Filtrar itens onde a quantidade (soma das quantidades dos pedidos) é maior que a quantidade em estoque
        return itensAgrupados.values().stream()
                .filter(itemResumo -> itemResumo.getQuantidade() > itemResumo.getQtdEstoque())
                .map(ItemFaltanteResumo::new)
                .collect(Collectors.toList());
    }

    private void criarTabelasPedidosIndividuais() {
        for (Pedido pedido : pedidosSelecionados) {
            Label labelCliente = new Label("Cliente: " + pedido.getCliente().getNome() + " (" + pedido.getCliente().getCpfCnpj() + ")");
            labelCliente.getStyleClass().add("form-label");

            TableView<ItemPedido> tabelaPedido = new TableView<>();
            configurarColunasTabelaPedido(tabelaPedido);

            tabelaPedido.setItems(FXCollections.observableArrayList(pedido.getItens()));

            HBox hboxTotaisPedido = new HBox(10);
            hboxTotaisPedido.setAlignment(Pos.CENTER_RIGHT);

            Map<String, Double> totaisPedido = calcularTotaisPedido(pedido);

            Label labelValorTotalPedido = new Label("Valor Total: R$ " + String.format("%.2f", totaisPedido.get("valorTotal")));
            Label labelQuantidadeTotalPedido = new Label("Quantidade Total: " + String.format("%.2f", totaisPedido.get("quantidadeTotal")));
            labelValorTotalPedido.getStyleClass().add("form-label");
            labelQuantidadeTotalPedido.getStyleClass().add("form-label");

            hboxTotaisPedido.getChildren().addAll(labelValorTotalPedido, labelQuantidadeTotalPedido);

            VBox vboxPedido = new VBox(5);
            vboxPedido.getChildren().addAll(labelCliente, tabelaPedido, hboxTotaisPedido);

            containerTabelasIndividuais.getChildren().add(vboxPedido);
        }
    }

    @SuppressWarnings("unchecked")
    private void configurarColunasTabelaPedido(TableView<ItemPedido> tabelaPedido) {
        TableColumn<ItemPedido, Integer> colunaId = new TableColumn<>("ID");
        colunaId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getItem().getId()).asObject());

        TableColumn<ItemPedido, String> colunaNome = new TableColumn<>("Nome");
        colunaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getNome()));

        TableColumn<ItemPedido, Double> colunaPrecoVenda = new TableColumn<>("Preço de Venda");
        colunaPrecoVenda.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrecoVenda()).asObject());

        TableColumn<ItemPedido, String> colunaUnidadeMedida = new TableColumn<>("Unid. de Medida");
        colunaUnidadeMedida.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getUnidadeMedida()));

        TableColumn<ItemPedido, Double> colunaQuantidade = new TableColumn<>("Quantidade");
        colunaQuantidade.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getQuantidade()).asObject());

        TableColumn<ItemPedido, Double> colunaQtdAtual = new TableColumn<>("Qtd. Atual");
        colunaQtdAtual.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getItem().getQuantidadeAtual()).asObject());

        TableColumn<ItemPedido, Double> colunaQtdEstoque = new TableColumn<>("Qtd. Estoque");
        colunaQtdEstoque.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getItem().getQuantidadeEstoque()).asObject());

        TableColumn<ItemPedido, String> colunaCategoria = new TableColumn<>("Categoria");
        colunaCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getCategoria().getNome()));

        tabelaPedido.getColumns().addAll(colunaId, colunaNome, colunaPrecoVenda, colunaUnidadeMedida, colunaQuantidade, colunaQtdAtual, colunaQtdEstoque, colunaCategoria);
        tabelaPedido.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void preencherTabelasPedidosIndividuais() {
        // Já preenchidas em criarTabelasPedidosIndividuais()
    }

    private void calcularTotais() {
        double valorTotal = pedidosSelecionados.stream().mapToDouble(Pedido::getValorTotal).sum();
        double quantidadeTotal = itensResumo.stream().mapToDouble(ItemResumo::getQuantidade).sum();

        labelValorTotal.setText("Valor Total: R$ " + String.format("%.2f", valorTotal));
        labelQuantidadeTotal.setText("Quantidade Total: " + String.format("%.2f", quantidadeTotal));
    }

    private Map<String, Double> calcularTotaisPedido(Pedido pedido) {
        double valorTotal = pedido.getItens().stream()
                .mapToDouble(itemPedido -> itemPedido.getQuantidade() * itemPedido.getPrecoVenda())
                .sum();

        double quantidadeTotal = pedido.getItens().stream()
                .mapToDouble(ItemPedido::getQuantidade)
                .sum();

        Map<String, Double> totais = new HashMap<>();
        totais.put("valorTotal", valorTotal);
        totais.put("quantidadeTotal", quantidadeTotal);
        return totais;
    }

    public static class ItemResumo {
        private int id;
        private String nome;
        private double precoVenda;
        private String unidadeMedida;
        private double quantidade;
        private double qtdAtual;
        private double qtdEstoque;
        private String categoria;

        public ItemResumo(Item item) {
            this.id = item.getId();
            this.nome = item.getNome();
            this.precoVenda = item.getPrecoVenda();
            this.unidadeMedida = item.getUnidadeMedida();
            this.qtdAtual = item.getQuantidadeAtual();
            this.qtdEstoque = item.getQuantidadeEstoque();
            this.categoria = item.getCategoria().getNome();
            this.quantidade = 0;
        }

        // Getters e Setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public double getPrecoVenda() {
            return precoVenda;
        }

        public void setPrecoVenda(double precoVenda) {
            this.precoVenda = precoVenda;
        }

        public String getUnidadeMedida() {
            return unidadeMedida;
        }

        public void setUnidadeMedida(String unidadeMedida) {
            this.unidadeMedida = unidadeMedida;
        }

        public double getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(double quantidade) {
            this.quantidade = quantidade;
        }
        public void calcularQuantidade(double quantidade) {
            this.quantidade += quantidade;
        }

        public double getQtdAtual() {
            return qtdAtual;
        }

        public void setQtdAtual(double qtdAtual) {
            this.qtdAtual = qtdAtual;
        }

        public double getQtdEstoque() {
            return qtdEstoque;
        }

        public void setQtdEstoque(double qtdEstoque) {
            this.qtdEstoque = qtdEstoque;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }
    }

    public static class DependenciaResumo {
        private int id;
        private String nomeDependencia;
        private String nomeDependente;
        private double qtdDependente;
        private double qtdDependencia;
        private String unidadeMedida;
        private String categoria;

        public DependenciaResumo(Dependencia dependencia, Item itemDependente, Item itemNecessario) {
            this.id = dependencia.getId();
            this.nomeDependencia = itemNecessario.getNome();
            this.nomeDependente = itemDependente.getNome();
            this.qtdDependente = 0;
            this.qtdDependencia = 0;
            this.unidadeMedida = itemNecessario.getUnidadeMedida();
            this.categoria = itemNecessario.getCategoria().getNome();
        }

        // Getters e Setters

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNomeDependencia() {
            return nomeDependencia;
        }

        public void setNomeDependencia(String nomeDependencia) {
            this.nomeDependencia = nomeDependencia;
        }

        public String getNomeDependente() {
            return nomeDependente;
        }

        public void setNomeDependente(String nomeDependente) {
            this.nomeDependente = nomeDependente;
        }

        public double getQtdDependente() {
            return qtdDependente;
        }

        public void setQtdDependente(double qtdDependente) {
            this.qtdDependente = qtdDependente;
        }

        public double getQtdDependencia() {
            return qtdDependencia;
        }

        public void setQtdDependencia(double qtdDependencia) {
            this.qtdDependencia = qtdDependencia;
        }

        public String getUnidadeMedida() {
            return unidadeMedida;
        }

        public void setUnidadeMedida(String unidadeMedida) {
            this.unidadeMedida = unidadeMedida;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }
    }

    public static class ItemFaltanteResumo {
        private int id;
        private String nome;
        private double quantidade; // Agora representa a diferença
        private String unidadeMedida;
        private String categoria;

        public ItemFaltanteResumo(ItemResumo itemResumo) {
            this.id = itemResumo.getId();
            this.nome = itemResumo.getNome();
            // Calcula a diferença (absoluta) entre a quantidade e a quantidade em estoque
            this.quantidade = Math.abs(itemResumo.getQuantidade() - itemResumo.getQtdEstoque());
            this.unidadeMedida = itemResumo.getUnidadeMedida();
            this.categoria = itemResumo.getCategoria();
        }

        // Getters e Setters

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public double getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(double quantidade) {
            this.quantidade = quantidade;
        }

        public String getUnidadeMedida() {
            return unidadeMedida;
        }

        public void setUnidadeMedida(String unidadeMedida) {
            this.unidadeMedida = unidadeMedida;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }
    }
}