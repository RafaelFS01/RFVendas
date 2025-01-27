package BackEnd.controller;

import BackEnd.Main;
import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;
import BackEnd.model.service.CategoriaService;
import BackEnd.model.service.DependenciaService;
import BackEnd.model.service.ItemService;
import BackEnd.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CadastrarItemController implements Initializable {

    @FXML private TextField idField;
    @FXML private TextField nomeField;
    @FXML private TextField descricaoField;
    @FXML private TextField precoVendaField;
    @FXML private TextField precoCustoField;
    @FXML private TextField unidadeMedidaField;
    @FXML private TextField quantidadeEstoqueField;
    @FXML private TextField quantidadeMinimaField;
    @FXML private TextField dependencia1Field;
    @FXML private TextField dependencia2Field;
    @FXML private TextField dependencia3Field;
    @FXML private TextField dependencia4Field;
    @FXML private Label dependencia1Label;
    @FXML private Label dependencia2Label;
    @FXML private Label dependencia3Label;
    @FXML private Label dependencia4Label;
    @FXML private Button adicionarDependencia1Button;
    @FXML private Button adicionarDependencia2Button;
    @FXML private Button adicionarDependencia3Button;
    @FXML private Button adicionarDependencia4Button;
    @FXML private Button salvarDependencia;
    @FXML private Button salvarItem;
    @FXML private ComboBox<Categoria> categoriaComboBox;
    @FXML private ComboBox<String> cbTipoProduto;
    @FXML private Label unidadeMedidaLabel;
    @FXML private Label quantidadeEstoqueLabel;
    @FXML private Label quantidadeMinimaLabel;

    private final ItemService itemService = new ItemService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final DependenciaService dependenciaService = new DependenciaService();

    private static int idItemAtual;
    private boolean cadastrandoServico = false;

    public void setIdItemAtual(int id) {
        idItemAtual = id;
    }

    public static int obterIdItem() {
        return idItemAtual;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTipoProduto();
        configurarCampos();
        carregarCategorias();
    }

    private void configurarTipoProduto() {
        cbTipoProduto.getItems().addAll("Item", "Serviço");
        cbTipoProduto.valueProperty().addListener((observable, oldValue, newValue) -> {
            configurarCamposTipoProduto(newValue);
        });
    }

    private void configurarCamposTipoProduto(String tipoProduto) {
        boolean isServico = "Serviço".equals(tipoProduto);
        cadastrandoServico = isServico;

        // Ajustar visibilidade com base no tipo de produto
        unidadeMedidaLabel.setVisible(!isServico);
        unidadeMedidaField.setVisible(!isServico);
        quantidadeEstoqueLabel.setVisible(!isServico);
        quantidadeEstoqueField.setVisible(!isServico);
        quantidadeMinimaLabel.setVisible(!isServico);
        quantidadeMinimaField.setVisible(!isServico);

        // Limpar campos que não são aplicáveis a serviços
        if (isServico) {
            unidadeMedidaField.clear();
            quantidadeEstoqueField.clear();
            quantidadeMinimaField.clear();
        }

        // Ajustar prompt text e outros comportamentos específicos, se necessário
        if (isServico) {
            idField.setPromptText("Digite o código do serviço");
            nomeField.setPromptText("Digite o nome do serviço");
            // ... outros ajustes para campos de serviço
        } else {
            idField.setPromptText("Digite o código do item");
            nomeField.setPromptText("Digite o nome do item");
            // ... outros ajustes para campos de item
        }
    }

    private void configurarCampos() {
        idField.textProperty().addListener((obs, old, novo) -> {
            if (novo != null) {
                idField.setText(novo.toUpperCase());
            }
        });

        nomeField.textProperty().addListener((obs, old, novo) -> {
            if (novo != null) {
                nomeField.setText(novo.toUpperCase());
            }
        });

        precoVendaField.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("\\d*(\\.\\d*)?")) {
                precoVendaField.setText(old);
            }
        });

        precoCustoField.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("\\d*(\\.\\d*)?")) {
                precoCustoField.setText(old);
            }
        });

        quantidadeEstoqueField.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("\\d*")) {
                quantidadeEstoqueField.setText(novo.replaceAll("[^\\d]", ""));
            }
        });

        quantidadeMinimaField.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("\\d*")) {
                quantidadeMinimaField.setText(novo.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void carregarCategorias() {
        try {
            List<Categoria> categoriasList = categoriaService.listarCategorias();
            ObservableList<Categoria> categorias = FXCollections.observableArrayList(categoriasList);
            categoriaComboBox.setItems(categorias);
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar categorias", e.getMessage());
        }
    }

    @FXML
    private void salvarItem(ActionEvent event) {
        try {
            if (cadastrandoServico) {
                Item servico = criarServico();
                itemService.salvarServico(servico);
                setIdItemAtual(servico.getId());
                AlertHelper.showSuccess("Serviço salvo com sucesso!");
            } else {
                Item item = criarItem();
                itemService.salvarItem(item);
                setIdItemAtual(item.getId());
                Optional<ButtonType> result = AlertHelper.showConfirmation(
                        "Item salvo com sucesso!!!",
                        "Deseja adicionar dependências ao produto?",
                        "Serão itens que serão necessários para confecção do produto."
                );

                if (result.isPresent() && result.get() == ButtonType.YES) {
                    idField.setDisable(true);
                    nomeField.setDisable(true);
                    descricaoField.setDisable(true);
                    precoVendaField.setDisable(true);
                    precoCustoField.setDisable(true);
                    unidadeMedidaField.setDisable(true);
                    quantidadeEstoqueField.setDisable(true);
                    quantidadeMinimaField.setDisable(true);
                    categoriaComboBox.setDisable(true);
                    abrirAdicionarDependencia1(event);
                    salvarDependencia.managedProperty().setValue(true);
                    salvarDependencia.visibleProperty().setValue(true);
                }
            }
            limparCampos();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar produto", e.getMessage());
        }
    }

    @FXML
    private void salvarDependencia(ActionEvent event) {
        try {
            AlertHelper.showSuccess("Dependências salvas com sucesso!!!");
            limparCampos();
            configurarBind();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar dependências", e.getMessage());
        }
    }

    private Item criarItem() {
        Item item = new Item("ITEM");
        item.setId(Integer.parseInt(idField.getText().trim()));
        item.setNome(nomeField.getText().trim());
        item.setDescricao(descricaoField.getText().trim());
        item.setPrecoVenda(Double.parseDouble(precoVendaField.getText()));
        item.setPrecoCusto(Double.parseDouble(precoCustoField.getText()));
        item.setUnidadeMedida(unidadeMedidaField.getText().trim());
        item.setQuantidadeEstoque(Double.parseDouble(quantidadeEstoqueField.getText()));
        item.setQuantidadeMinima(Double.parseDouble(quantidadeMinimaField.getText()));
        item.setQuantidadeAtual(Double.parseDouble(quantidadeEstoqueField.getText()));
        item.setCategoria(categoriaComboBox.getValue());
        return item;
    }

    private Item criarServico() {
        Item servico = new Item("SERVICO");
        servico.setId(Integer.parseInt(idField.getText().trim()));
        servico.setNome(nomeField.getText().trim());
        servico.setDescricao(descricaoField.getText().trim());
        servico.setPrecoVenda(Double.parseDouble(precoVendaField.getText()));
        servico.setPrecoCusto(Double.parseDouble(precoCustoField.getText()));
        servico.setCategoria(categoriaComboBox.getValue());
        return servico;
    }

    @FXML
    private void limparCampos() {
        idField.clear();
        nomeField.clear();
        descricaoField.clear();
        precoVendaField.clear();
        precoCustoField.clear();
        unidadeMedidaField.clear();
        quantidadeEstoqueField.clear();
        quantidadeMinimaField.clear();
        categoriaComboBox.getSelectionModel().clearSelection();
        cbTipoProduto.getSelectionModel().clearSelection();

        idField.setDisable(false);
        nomeField.setDisable(false);
        descricaoField.setDisable(false);
        precoVendaField.setDisable(false);
        precoCustoField.setDisable(false);
        unidadeMedidaField.setDisable(false);
        quantidadeEstoqueField.setDisable(false);
        quantidadeMinimaField.setDisable(false);
        categoriaComboBox.setDisable(false);

        idField.setPromptText("Digite o código do item");
        nomeField.setPromptText("Digite o nome do item");

        unidadeMedidaLabel.setVisible(true);
        unidadeMedidaField.setVisible(true);
        quantidadeEstoqueLabel.setVisible(true);
        quantidadeEstoqueField.setVisible(true);
        quantidadeMinimaLabel.setVisible(true);
        quantidadeMinimaField.setVisible(true);

        configurarCamposTipoProduto(cbTipoProduto.getValue());
    }

    private void configurarBind() {
        cbTipoProduto.setDisable(false);

        adicionarDependencia1Button.setVisible(false);
        adicionarDependencia1Button.setManaged(false);
        adicionarDependencia2Button.setVisible(false);
        adicionarDependencia2Button.setManaged(false);
        adicionarDependencia3Button.setVisible(false);
        adicionarDependencia3Button.setManaged(false);
        adicionarDependencia4Button.setVisible(false);
        adicionarDependencia4Button.setManaged(false);

        salvarDependencia.setVisible(false);
        salvarDependencia.setManaged(false);
        salvarItem.setVisible(true);
        salvarItem.setManaged(true);

        dependencia1Label.setVisible(false);
        dependencia1Label.setManaged(false);
        dependencia1Field.setVisible(false);
        dependencia1Field.setManaged(false);

        dependencia2Label.setVisible(false);
        dependencia2Label.setManaged(false);
        dependencia2Field.setVisible(false);
        dependencia2Field.setManaged(false);

        dependencia3Label.setVisible(false);
        dependencia3Label.setManaged(false);
        dependencia3Field.setVisible(false);
        dependencia3Field.setManaged(false);

        dependencia4Label.setVisible(false);
        dependencia4Label.setManaged(false);
        dependencia4Field.setVisible(false);
        dependencia4Field.setManaged(false);
    }

    @FXML
    private void abrirCadastroCategoria(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CadastrarCategoria.fxml"));
            Parent root = loader.load();

            CadastrarCategoriaController controller = loader.getController();
            controller.setCategoriaService(categoriaService);

            Stage stage = new Stage();
            stage.setTitle("Cadastrar Categoria");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();
            carregarCategorias();

        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de cadastro de categoria", e.getMessage());
        }
    }

    @FXML
    private void abrirAdicionarDependencia1(ActionEvent event) {
        if (cadastrandoServico) {
            abrirAdicionarDependenciaParaServico(event, 1);
        } else {
            abrirAdicionarDependenciaParaItem(event, 1);
        }
    }

    @FXML
    private void abrirAdicionarDependencia2(ActionEvent event) {
        if (cadastrandoServico) {
            abrirAdicionarDependenciaParaServico(event, 2);
        } else {
            abrirAdicionarDependenciaParaItem(event, 2);
        }
    }

    @FXML
    private void abrirAdicionarDependencia3(ActionEvent event) {
        if (cadastrandoServico) {
            abrirAdicionarDependenciaParaServico(event, 3);
        } else {
            abrirAdicionarDependenciaParaItem(event, 3);
        }
    }

    @FXML
    private void abrirAdicionarDependencia4(ActionEvent event) {
        if (cadastrandoServico) {
            abrirAdicionarDependenciaParaServico(event, 4);
        } else {
            abrirAdicionarDependenciaParaItem(event, 4);
        }
    }

    private void abrirAdicionarDependenciaParaItem(ActionEvent event, int numeroDependencia) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            int idProdutoDependente = Integer.parseInt(idField.getText().trim());
            controller.setIdItemDependente(idProdutoDependente);

            controller.setOnDependenciaSalva(item -> {
                switch (numeroDependencia) {
                    case 1:
                        dependencia1Field.setText(item.getNome());
                        break;
                    case 2:
                        dependencia2Field.setText(item.getNome());
                        break;
                    case 3:
                        dependencia3Field.setText(item.getNome());
                        break;
                    case 4:
                        dependencia4Field.setText(item.getNome());
                        break;
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();

            switch (numeroDependencia) {
                case 1:
                    dependencia1Field.setVisible(true);
                    dependencia1Field.setManaged(true);
                    dependencia1Label.setVisible(true);
                    dependencia1Label.setManaged(true);
                    adicionarDependencia2Button.setVisible(true);
                    adicionarDependencia2Button.setManaged(true);
                    break;
                case 2:
                    dependencia2Field.setVisible(true);
                    dependencia2Field.setManaged(true);
                    dependencia2Label.setVisible(true);
                    dependencia2Label.setManaged(true);
                    adicionarDependencia3Button.setVisible(true);
                    adicionarDependencia3Button.setManaged(true);
                    break;
                case 3:
                    dependencia3Field.setVisible(true);
                    dependencia3Field.setManaged(true);
                    dependencia3Label.setVisible(true);
                    dependencia3Label.setManaged(true);
                    adicionarDependencia4Button.setVisible(true);
                    adicionarDependencia4Button.setManaged(true);
                    break;
                case 4:
                    dependencia4Field.setVisible(true);
                    dependencia4Field.setManaged(true);
                    dependencia4Label.setVisible(true);
                    dependencia4Label.setManaged(true);
                    break;
            }

            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);

        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }

    private void abrirAdicionarDependenciaParaServico(ActionEvent event, int numeroDependencia) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            int idServicoDependente = Integer.parseInt(idField.getText().trim());
            controller.setIdItemDependente(idServicoDependente);

            controller.setOnDependenciaSalva(item -> {
                switch (numeroDependencia) {
                    case 1:
                        dependencia1Field.setText(item.getNome());
                        break;
                    case 2:
                        dependencia2Field.setText(item.getNome());
                        break;
                    case 3:
                        dependencia3Field.setText(item.getNome());
                        break;
                    case 4:
                        dependencia4Field.setText(item.getNome());
                        break;
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();

            switch (numeroDependencia) {
                case 1:
                    dependencia1Field.setVisible(true);
                    dependencia1Field.setManaged(true);
                    dependencia1Label.setVisible(true);
                    dependencia1Label.setManaged(true);
                    adicionarDependencia2Button.setVisible(true);
                    adicionarDependencia2Button.setManaged(true);
                    break;
                case 2:
                    dependencia2Field.setVisible(true);
                    dependencia2Field.setManaged(true);
                    dependencia2Label.setVisible(true);
                    dependencia2Label.setManaged(true);
                    adicionarDependencia3Button.setVisible(true);
                    adicionarDependencia3Button.setManaged(true);
                    break;
                case 3:
                    dependencia3Field.setVisible(true);
                    dependencia3Field.setManaged(true);
                    dependencia3Label.setVisible(true);
                    dependencia3Label.setManaged(true);
                    adicionarDependencia4Button.setVisible(true);
                    adicionarDependencia4Button.setManaged(true);
                    break;
                case 4:
                    dependencia4Field.setVisible(true);
                    dependencia4Field.setManaged(true);
                    dependencia4Label.setVisible(true);
                    dependencia4Label.setManaged(true);
                    break;
            }

            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);

        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }
}