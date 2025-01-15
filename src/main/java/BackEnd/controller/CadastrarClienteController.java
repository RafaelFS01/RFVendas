package BackEnd.controller;

import BackEnd.model.entity.Cliente;
import BackEnd.model.entity.Grupo;
import BackEnd.model.service.ClienteService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;

public class CadastrarClienteController {

    @FXML
    private TextField idField;
    @FXML
    private TextField nomeField;
    @FXML
    private ComboBox<Cliente.TipoCliente> tipoClienteComboBox;
    @FXML
    private TextField cpfCnpjField;
    @FXML
    private TextField logradouroField;
    @FXML
    private TextField bairroField;
    @FXML
    private TextField cidadeField;
    @FXML
    private TextField numeroField;
    @FXML
    private TextField complementoField;
    @FXML
    private TextField telefoneCelularField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField compradorField;
    @FXML
    private ComboBox<Grupo> grupoComboBox;

    private final ClienteService clienteService = new ClienteService();

    @FXML
    private void initialize() {
        tipoClienteComboBox.setItems(FXCollections.observableArrayList(Cliente.TipoCliente.values()));
        configurarTipoClienteListener();
        carregarGrupos();

        idField.textProperty().addListener((obs, old, novo) -> {
            if (!novo.matches("[A-Za-z0-9-]*")) {
                idField.setText(old);
            }
        });

        configurarGrupoComboBox();
    }

    private void configurarGrupoComboBox() {
        grupoComboBox.setConverter(new StringConverter<Grupo>() {
            @Override
            public String toString(Grupo grupo) {
                return (grupo == null) ? null : grupo.getNome();
            }

            @Override
            public Grupo fromString(String string) {
                // Não é necessário para este caso, pois o usuário não digita o nome do grupo
                return null;
            }
        });
    }

    private void configurarTipoClienteListener() {
        tipoClienteComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            cpfCnpjField.clear();
            if (novo != null) {
                if (novo == Cliente.TipoCliente.PESSOA_FISICA) {
                    cpfCnpjField.setPromptText("Digite o CPF (11 dígitos)");
                    cpfCnpjField.textProperty().addListener((obsCpf, oldCpf, novoCpf) -> {
                        if (!novoCpf.matches("\\d*")) {
                            cpfCnpjField.setText(oldCpf);
                        } else if (novoCpf.length() > 11) {
                            cpfCnpjField.setText(oldCpf);
                        }
                    });
                } else {
                    cpfCnpjField.setPromptText("Digite o CNPJ (14 dígitos)");
                    cpfCnpjField.textProperty().addListener((obsCnpj, oldCnpj, novoCnpj) -> {
                        if (!novoCnpj.matches("\\d*")) {
                            cpfCnpjField.setText(oldCnpj);
                        } else if (novoCnpj.length() > 14) {
                            cpfCnpjField.setText(oldCnpj);
                        }
                    });
                }
            } else {
                cpfCnpjField.setPromptText("Digite o CPF ou CNPJ");
            }
        });
    }

    @FXML
    private void salvarCliente() {
        try {
            Cliente cliente = new Cliente();
            cliente.setId(idField.getText().trim());
            cliente.setNome(nomeField.getText().trim());
            cliente.setTipoCliente(tipoClienteComboBox.getValue());
            cliente.setCpfCnpj(cpfCnpjField.getText().trim());
            cliente.setLogradouro(logradouroField.getText().trim());
            cliente.setBairro(bairroField.getText().trim());
            cliente.setCidade(cidadeField.getText().trim());
            cliente.setNumero(numeroField.getText().trim());
            cliente.setComplemento(complementoField.getText().trim());
            cliente.setTelefoneCelular(telefoneCelularField.getText().trim());
            cliente.setEmail(emailField.getText().trim());
            cliente.setComprador(compradorField.getText().trim());
            cliente.setGrupo(grupoComboBox.getValue());

            clienteService.cadastrarCliente(cliente);
            ConnectionFactory.exportarBancoDeDados("BACKUP.2024");
            AlertHelper.showSuccess("Cliente cadastrado com sucesso!");
            limparFormulario();
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning("Erro de Validação", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Erro", "Erro ao cadastrar cliente: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        Stage stage = (Stage) idField.getScene().getWindow();
        stage.close();
    }

    private void limparFormulario() {
        idField.clear();
        nomeField.clear();
        tipoClienteComboBox.getSelectionModel().clearSelection();
        cpfCnpjField.clear();
        logradouroField.clear();
        bairroField.clear();
        cidadeField.clear();
        numeroField.clear();
        complementoField.clear();
        telefoneCelularField.clear();
        emailField.clear();
        compradorField.clear();
        grupoComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void abrirCadastroGrupo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CadastrarGrupo.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Cadastrar Grupo");
            stage.setScene(new Scene(loader.load()));

            CadastrarGrupoController controller = loader.getController();
            controller.setCadastroClienteController(this);

            stage.showAndWait();
        } catch (IOException e) {
            AlertHelper.showError("Erro", "Erro ao abrir a janela de cadastro de grupo: " + e.getMessage());
        }
    }

    public void carregarGrupos() {
        try {
            List<Grupo> grupos = clienteService.listarGrupos();
            grupoComboBox.setItems(FXCollections.observableArrayList(grupos));
        } catch (Exception e) {
            AlertHelper.showError("Erro", "Erro ao carregar grupos: " + e.getMessage());
        }
    }

    public void atualizarGrupos() {
        carregarGrupos();
    }
}