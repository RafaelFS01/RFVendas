
package BackEnd.controller;

import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.SugestaoCompra;
import BackEnd.model.service.CategoriaService;
import BackEnd.model.service.CompraService;
import BackEnd.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class ListarComprasController {

    @FXML
    private ComboBox<Categoria> cbFiltroCategoria;

    @FXML
    private VBox containerItensCompra;

    @FXML
    private Label labelCategoriaSelecionada;

    private CompraService compraService;
    private CategoriaService categoriaService;

    @FXML
    public void initialize() {
        compraService = new CompraService();
        categoriaService = new CategoriaService();
        inicializarComboBoxCategorias();
        atualizarListagem();
        cbFiltroCategoria.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> atualizarListagem());
    }

    private void inicializarComboBoxCategorias() {
        try {
            List<Categoria> categorias = categoriaService.listarCategorias();
            ObservableList<Categoria> obsCategorias = FXCollections.observableArrayList(categorias);
            cbFiltroCategoria.setItems(obsCategorias);
        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar categorias", e.getMessage());
        }
    }

    private void atualizarListagem() {
        containerItensCompra.getChildren().clear();
        Categoria categoriaSelecionada = cbFiltroCategoria.getSelectionModel().getSelectedItem();

        // Atualiza a label da categoria selecionada
        if (categoriaSelecionada != null) {
            labelCategoriaSelecionada.setText("Categoria: " + categoriaSelecionada.getNome());
        } else {
            labelCategoriaSelecionada.setText("");
        }

        try {
            List<SugestaoCompra> sugestoes = compraService.calcularSugestoesCompra(categoriaSelecionada);
            for (SugestaoCompra sugestao : sugestoes) {
                Label nomeItemLabel = new Label("Item: " + sugestao.getItem().getNome());
                nomeItemLabel.getStyleClass().add("form-label");

                Label quantidadeLabel = new Label("Comprar: " + sugestao.getQuantidadeSugerida() + " " + sugestao.getItem().getUnidadeMedida());
                quantidadeLabel.getStyleClass().add("form-label");

                containerItensCompra.getChildren().addAll(nomeItemLabel, quantidadeLabel);
            }
        } catch (Exception e) {
            AlertHelper.showError("Erro ao calcular sugestões de compra", e.getMessage());
        }
    }
}