package BackEnd.controller;

import BackEnd.model.service.DependenciaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import BackEnd.Main;
import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;
import BackEnd.model.service.CategoriaService;
import BackEnd.model.service.ItemService;
import BackEnd.util.AlertHelper;
import javafx.scene.image.Image;

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

    private final ItemService itemService;
    private final CategoriaService categoriaService;
    private final DependenciaService dependenciaService;

    // Variável estática para armazenar o ID do último item salvo
    private static int idItemAtual;

    // Método estático para definir o ID do item atual
    public void setIdItemAtual(int id) {
        idItemAtual = id;
    }

    // Método estático para obter o ID do item atual
    public static int obterIdItem() {
        return idItemAtual;
    }

    public CadastrarItemController() {
        this.itemService = new ItemService();
        this.categoriaService = new CategoriaService();
        this.dependenciaService = new DependenciaService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarCampos();
        carregarCategorias();
    }

    private void configurarCampos() {
        // Configurar formatação e validação dos campos

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

        // Para campos numéricos, você pode adicionar listeners semelhantes ao exemplo para restringir a entrada

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
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void salvarItem(ActionEvent event) {
        try {
            Item item = criarItem();
            itemService.salvarItem(item);
            // Atualiza o idItemAtual com o ID do item recém-salvo
            setIdItemAtual(item.getId());

            Optional<ButtonType> result = AlertHelper.showConfirmation(
                    "Item salvo com sucesso!!!",
                    "Deseja adicionar dependências ao produto?",
                    "Serão itens que serão necessários para confecção do produto."
            );

            if (result.isPresent() && result.get() == ButtonType.YES) { // Compare com YES agora
                //Fazer os campos de cadastro do produto ficarem bloqueados
                idField.setDisable(true);
                nomeField.setDisable(true);
                descricaoField.setDisable(true);
                precoVendaField.setDisable(true);
                precoCustoField.setDisable(true);
                unidadeMedidaField.setDisable(true);
                quantidadeEstoqueField.setDisable(true);
                quantidadeMinimaField.setDisable(true);
                categoriaComboBox.setDisable(true);
                // Ação a ser executada se o usuário clicar em "Sim"
                abrirAdicionarDependencia1(event);
                salvarDependencia.managedProperty().setValue(true);
                salvarDependencia.visibleProperty().setValue(true);

            } else {
                // Ação a ser executada se o usuário clicar em "Não" (ou fechar a janela)
                limparCampos();
            }
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar item", e.getMessage());
        }
    }

    @FXML
    private void salvarDependencia(ActionEvent event) {
        try {
            AlertHelper.showSuccess("Dependências salvas com sucesso!!!");
                // Ação a ser executada se o usuário clicar em "Não" (ou fechar a janela)
                limparCampos();
                configurarBind();

        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar item", e.getMessage());
        }
    }

    private Item criarItem() {
        Item item = new Item();
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
        //Fazer os campos de cadastro do produto desbloquearem.
        idField.setDisable(false);
        nomeField.setDisable(false);
        descricaoField.setDisable(false);
        precoVendaField.setDisable(false);
        precoCustoField.setDisable(false);
        unidadeMedidaField.setDisable(false);
        quantidadeEstoqueField.setDisable(false);
        quantidadeMinimaField.setDisable(false);
        categoriaComboBox.setDisable(false);
    }

    private void configurarBind(){
        adicionarDependencia1Button.visibleProperty().setValue(false);
        adicionarDependencia1Button.managedProperty().setValue(false);
        adicionarDependencia2Button.visibleProperty().setValue(false);
        adicionarDependencia2Button.managedProperty().setValue(false);
        adicionarDependencia3Button.visibleProperty().setValue(false);
        adicionarDependencia3Button.managedProperty().setValue(false);
        adicionarDependencia4Button.visibleProperty().setValue(false);
        adicionarDependencia4Button.managedProperty().setValue(false);

        salvarDependencia.visibleProperty().setValue(false);
        salvarDependencia.managedProperty().setValue(false);
        salvarItem.visibleProperty().setValue(true);
        salvarItem.managedProperty().setValue(true);

        dependencia1Label.visibleProperty().setValue(false);
        dependencia1Label.managedProperty().setValue(false);
        dependencia1Field.visibleProperty().setValue(false);
        dependencia1Field.managedProperty().setValue(false);

        dependencia2Label.visibleProperty().setValue(false);
        dependencia2Label.managedProperty().setValue(false);
        dependencia2Field.visibleProperty().setValue(false);
        dependencia2Field.managedProperty().setValue(false);

        dependencia3Label.visibleProperty().setValue(false);
        dependencia3Label.managedProperty().setValue(false);
        dependencia3Field.visibleProperty().setValue(false);
        dependencia3Field.managedProperty().setValue(false);

        dependencia4Label.visibleProperty().setValue(false);
        dependencia4Label.managedProperty().setValue(false);
        dependencia4Field.visibleProperty().setValue(false);
        dependencia4Field.managedProperty().setValue(false);
    }

    @FXML
    private void abrirCadastroCategoria(ActionEvent event)      {
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            // Passe o ID do item dependente para o controller da janela AdicionarDependencia
            int idItemDependente = Integer.parseInt(idField.getText().trim()); // Obtenha o ID do campo idField
            controller.setIdItemDependente(idItemDependente);

            // Passar uma expressão lambda que define a ação a ser executada quando a dependência for salva
            controller.setOnDependenciaSalva(item -> {
                dependencia1Field.setText(item.getNome());
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();
            // Fazer os campos e botão aparecerem
            dependencia1Field.visibleProperty().setValue(true);
            dependencia1Field.managedProperty().setValue(true);
            dependencia1Label.visibleProperty().setValue(true);
            dependencia1Label.managedProperty().setValue(true);
            adicionarDependencia2Button.visibleProperty().setValue(true);
            adicionarDependencia2Button.managedProperty().setValue(true);
            // Fazer o botão desaparecer
            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);

        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }

    @FXML
    private void abrirAdicionarDependencia2(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            // Passe o ID do item dependente para o controller da janela AdicionarDependencia
            int idItemDependente = Integer.parseInt(idField.getText().trim()); // Obtenha o ID do campo idField
            controller.setIdItemDependente(idItemDependente);

            // Passar uma expressão lambda que define a ação a ser executada quando a dependência for salva
            controller.setOnDependenciaSalva(item -> {
                dependencia2Field.setText(item.getNome());
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();
            // Fazer os campos e botão aparecerem
            dependencia2Field.visibleProperty().setValue(true);
            dependencia2Field.managedProperty().setValue(true);
            dependencia2Label.visibleProperty().setValue(true);
            dependencia2Label.managedProperty().setValue(true);
            adicionarDependencia3Button.visibleProperty().setValue(true);
            adicionarDependencia3Button.managedProperty().setValue(true);
            // Fazer o botão desaparecer
            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);
        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }

    @FXML
    private void abrirAdicionarDependencia3(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            // Passe o ID do item dependente para o controller da janela AdicionarDependencia
            int idItemDependente = Integer.parseInt(idField.getText().trim()); // Obtenha o ID do campo idField
            controller.setIdItemDependente(idItemDependente);

            // Passar uma expressão lambda que define a ação a ser executada quando a dependência for salva
            controller.setOnDependenciaSalva(item -> {
                dependencia3Field.setText(item.getNome());
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();
            // Fazer os campos e botão aparecerem
            dependencia3Field.visibleProperty().setValue(true);
            dependencia3Field.managedProperty().setValue(true);
            dependencia3Label.visibleProperty().setValue(true);
            dependencia3Label.managedProperty().setValue(true);
            adicionarDependencia4Button.visibleProperty().setValue(true);
            adicionarDependencia4Button.managedProperty().setValue(true);
            // Fazer o botão desaparecer
            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);
        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }

    @FXML
    private void abrirAdicionarDependencia4(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            Button botaoAdicionarDependencia = (Button) event.getSource();

            AdicionarDependenciaController controller = loader.getController();
            // Passe o ID do item dependente para o controller da janela AdicionarDependencia
            int idItemDependente = Integer.parseInt(idField.getText().trim()); // Obtenha o ID do campo idField
            controller.setIdItemDependente(idItemDependente);

            // Passar uma expressão lambda que define a ação a ser executada quando a dependência for salva
            controller.setOnDependenciaSalva(item -> {
                dependencia4Field.setText(item.getNome());
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            stage.showAndWait();
            // Fazer os campos e botão aparecerem
            dependencia4Field.visibleProperty().setValue(true);
            dependencia4Field.managedProperty().setValue(true);
            dependencia4Label.visibleProperty().setValue(true);
            dependencia4Label.managedProperty().setValue(true);
            // Fazer o botão desaparecer
            botaoAdicionarDependencia.setVisible(false);
            botaoAdicionarDependencia.setManaged(false);

        } catch (IOException e) {
            AlertHelper.showError("Erro ao abrir a janela de adição de dependência.", e.getMessage());
        }
    }
}