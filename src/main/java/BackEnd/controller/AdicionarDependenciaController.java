package BackEnd.controller;

import BackEnd.model.entity.Dependencia;
import BackEnd.model.entity.Item;
import BackEnd.model.service.DependenciaService;
import BackEnd.model.service.ItemService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import BackEnd.model.entity.Categoria;
import BackEnd.model.service.CategoriaService;
import BackEnd.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdicionarDependenciaController implements Initializable {

    @FXML private ComboBox<Categoria> categoriaComboBox;
    @FXML private ComboBox<Item> dependenciaComboBox;
    @FXML private TextField quantidadeField;

    private final DependenciaService dependenciaService;
    private final CategoriaService categoriaService;
    private final ItemService itemService;

    private int idItemDependente;
    private Consumer<Item> onDependenciaSalva;

    public void setOnDependenciaSalva(Consumer<Item> onDependenciaSalva) {
        this.onDependenciaSalva = onDependenciaSalva;
    }

    // M?todo para definir o ID do item dependente
    public void setIdItemDependente(int idItemDependente) {
        this.idItemDependente = idItemDependente;
    }

    public AdicionarDependenciaController() {
        this.dependenciaService = new DependenciaService();
        this.categoriaService = new CategoriaService();
        this.itemService = new ItemService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configurarCategoriaComboBox();
            configurarDependenciaComboBox();
            configurarQuantidadeField();
        } catch (Exception e) {
            AlertHelper.showError("Erro", "Falha ao inicializar a tela: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarCategoriaComboBox() throws Exception {
        List<Categoria> categorias = categoriaService.listarCategorias();
        categoriaComboBox.setItems(FXCollections.observableArrayList(categorias));

        categoriaComboBox.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria categoria) {
                return categoria != null ? categoria.getNome() : "";
            }

            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });

        categoriaComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    carregarDependencias(newVal.getId());
                } catch (Exception e) {
                    AlertHelper.showError("Erro", "Falha ao carregar depend?ncias: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                dependenciaComboBox.getItems().clear();
                dependenciaComboBox.getEditor().clear();
            }
        });
    }

    private void configurarDependenciaComboBox() {
        // Configurar o StringConverter para exibir o nome do item
        dependenciaComboBox.setConverter(new StringConverter<Item>() {
            @Override
            public String toString(Item item) {
                return item != null ? item.getNome() : "";
            }

            @Override
            public Item fromString(String string) {
                return dependenciaComboBox.getItems().stream()
                        .filter(item -> item.getNome().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        dependenciaComboBox.setEditable(true);

        // Configurar o comportamento do ComboBox para filtragem
        dependenciaComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                // Se o texto for vazio, reverter para a lista completa de itens
                if (dependenciaComboBox.getValue() != null) {
                    carregarDependencias(dependenciaComboBox.getValue().getId());
                }
            } else {
                // Filtrar os itens com base no texto digitado
                String lowerCaseFilter = newText.toLowerCase();
                List<Item> filteredList = dependenciaComboBox.getItems().stream()
                        .filter(item -> item.getNome().toLowerCase().contains(lowerCaseFilter))
                        .collect(Collectors.toList());
                dependenciaComboBox.setItems(FXCollections.observableArrayList(filteredList));
                if (!filteredList.isEmpty() && !dependenciaComboBox.isShowing()) {
                    dependenciaComboBox.show(); // Mostrar o dropdown se n?o estiver vazio
                }
            }
        });

        dependenciaComboBox.setOnAction(e -> {
            Item selectedItem = dependenciaComboBox.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                // Atualizar o editor com o nome do item selecionado
                dependenciaComboBox.getEditor().setText(selectedItem.getNome());
            }
        });

    }


    private void carregarDependencias(int idCategoria) {
        try {
            List<Item> itens = itemService.listarItensPorCategoria(idCategoria);
            dependenciaComboBox.setItems(FXCollections.observableArrayList(itens));
            dependenciaComboBox.getSelectionModel().clearSelection();
            dependenciaComboBox.getEditor().clear();
        } catch (Exception e) {
            AlertHelper.showError("Erro", "N?o foi poss?vel carregar os itens da categoria selecionada: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarQuantidadeField() {
        quantidadeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                quantidadeField.setText(oldValue);
            }
        });
    }

    @FXML
    private void salvarDependencia(ActionEvent event) {
        try {
            if (categoriaComboBox.getValue() == null || dependenciaComboBox.getValue() == null || quantidadeField.getText().isEmpty()) {
                AlertHelper.showError("Erro ao salvar", "Preencha todos os campos obrigat?rios.");
                return;
            }

            Categoria categoriaSelecionada = categoriaComboBox.getValue();
            Item itemSelecionado = dependenciaComboBox.getValue();
            double quantidade = Double.parseDouble(quantidadeField.getText());

            Dependencia dependencia = new Dependencia();
            dependencia.setIdItemDependente(idItemDependente);
            dependencia.setIdItemNecessario(itemSelecionado.getId());
            dependencia.setIdCategoria(categoriaSelecionada.getId());
            dependencia.setQuantidade(quantidade);

            dependenciaService.salvarDependencia(dependencia);

            if (onDependenciaSalva != null) {
                onDependenciaSalva.accept(itemSelecionado);
            }

            AlertHelper.showSuccess("Depend?ncia salva com sucesso!");
            limparCampos();
            fecharModal(event);

        } catch (NumberFormatException e) {
            AlertHelper.showError("Erro ao salvar", "Quantidade inv?lida.");
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar", "Ocorreu um erro ao salvar a depend?ncia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limparCampos() {
        categoriaComboBox.getSelectionModel().clearSelection();
        dependenciaComboBox.getSelectionModel().clearSelection();
        dependenciaComboBox.getEditor().clear();
        quantidadeField.clear();
    }

    @FXML
    private void fecharModal(ActionEvent event) {
        Stage stage = (Stage) quantidadeField.getScene().getWindow();
        stage.close();
    }
}
