package BackEnd.controller;

import BackEnd.model.service.ClienteService;
import BackEnd.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CadastrarGrupoController {

    @FXML
    private TextField nomeGrupoField;

    private final ClienteService clienteService = new ClienteService();

    private CadastrarClienteController cadastrarClienteController;

    public void setCadastroClienteController(CadastrarClienteController cadastrarClienteController) {
        this.cadastrarClienteController = cadastrarClienteController;
    }

    @FXML
    private void salvarGrupo() {
        try {
            String nomeGrupo = nomeGrupoField.getText().trim();
            clienteService.cadastrarGrupo(nomeGrupo);
            AlertHelper.showSuccess("Grupo cadastrado com sucesso!");

            if (cadastrarClienteController != null) {
                cadastrarClienteController.atualizarGrupos();
            }

            Stage stage = (Stage) nomeGrupoField.getScene().getWindow();
            stage.close();
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning("Erro de Validação", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Erro", "Erro ao cadastrar grupo: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarGrupo() {
        Stage stage = (Stage) nomeGrupoField.getScene().getWindow();
        stage.close();
    }
}