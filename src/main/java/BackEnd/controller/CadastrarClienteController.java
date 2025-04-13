package BackEnd.controller;

import BackEnd.model.entity.Cliente;
import BackEnd.model.entity.Grupo;
import BackEnd.model.service.ClienteService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;
import BackEnd.util.ValidationHelper;
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
import java.util.Optional;

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

    private Cliente clienteSendoEditado = null;
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
            // Cria ou atualiza o objeto Cliente com dados da UI
            Cliente cliente = criarOuAtualizarCliente();

            if (clienteSendoEditado == null) {
                // --- MODO CADASTRO ---
                clienteService.cadastrarCliente(cliente); // Usa o método existente
                ConnectionFactory.exportarBancoDeDados("BACKUP.2024"); // Mantido se necessário
                AlertHelper.showSuccess( "Cliente '" + cliente.getNome() + "' cadastrado com sucesso!");
                limparFormulario();
            } else {
                // --- MODO EDIÇÃO ---
                clienteService.atualizarCliente(cliente); // Chama NOVO método no Service
                ConnectionFactory.exportarBancoDeDados("BACKUP.2024"); // Mantido se necessário
                AlertHelper.showSuccess("Cliente '" + cliente.getNome() + "' atualizado com sucesso!");
                limparFormulario(); // Limpa e volta ao modo cadastro
                // Navegar de volta para a lista (requer MainController)
                // Ex: if (mainController != null) mainController.mostrarListaFuncionarios();
            }

        } catch (IllegalArgumentException e) { // Erro de validação
            AlertHelper.showWarning("Erro de Validação", e.getMessage());
        } catch (Exception e) { // Outros erros (DAO, etc.)
            String acao = (clienteSendoEditado == null) ? "cadastrar" : "atualizar";
            AlertHelper.showError("Erro", "Erro ao " + acao + " cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cancelar() {
        // Limpa o formulário e volta ao modo de cadastro (ou navega de volta)
        limparFormulario();

        // Navega de volta para a lista de clientes (REQUER MainController)
        // Precisa injetar a referência do MainController neste controller também
        // Exemplo:
        // if (mainController != null) {
        //     mainController.mostrarListaFuncionarios(); // Chama o método no MainController
        // } else {
        //     // Fallback se não houver MainController (ex: fechar se for modal - improvável agora)
        //     Stage stage = (Stage) idField.getScene().getWindow();
        //     if (stage.getModality() == Modality.APPLICATION_MODAL) { // Verifica se era modal
        //          stage.close();
        //      }
        // }
        System.out.println("Operação cancelada. Navegação de volta não implementada sem MainController."); // Placeholder
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
        this.clienteSendoEditado = null;
        ajustarUiParaModoEdicao(false); // Garante que UI volte ao modo cadastro
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

    /**
     * Carrega dados de um cliente existente para edição.
     * @param clienteId O ID (String) do cliente a ser editado.
     */
    public void carregarClienteParaEdicao(String clienteId) {
        try {
            this.clienteSendoEditado = clienteService.buscarPorId(clienteId); // Assume que serviço tem buscarPorId

            if (this.clienteSendoEditado == null) {
                AlertHelper.showError("Erro", "Cliente com ID " + clienteId + " não encontrado.");
                limparFormulario();
                return;
            }

            // Preenche os campos da UI
            idField.setText(clienteSendoEditado.getId());
            nomeField.setText(clienteSendoEditado.getNome());
            tipoClienteComboBox.setValue(clienteSendoEditado.getTipoCliente()); // Seleciona o Enum
            cpfCnpjField.setText(clienteSendoEditado.getCpfCnpj());
            logradouroField.setText(clienteSendoEditado.getLogradouro());
            bairroField.setText(clienteSendoEditado.getBairro());
            cidadeField.setText(clienteSendoEditado.getCidade());
            numeroField.setText(clienteSendoEditado.getNumero());
            complementoField.setText(clienteSendoEditado.getComplemento());
            telefoneCelularField.setText(clienteSendoEditado.getTelefoneCelular());
            emailField.setText(clienteSendoEditado.getEmail());
            compradorField.setText(clienteSendoEditado.getComprador());

            // Seleciona o Grupo no ComboBox
            if (clienteSendoEditado.getGrupo() != null) {
                Optional<Grupo> grupoOpt = grupoComboBox.getItems().stream()
                        .filter(g -> g.getId() == clienteSendoEditado.getGrupo().getId())
                        .findFirst();
                grupoOpt.ifPresent(grupoComboBox::setValue);
            } else {
                grupoComboBox.getSelectionModel().clearSelection();
            }

            // Ajusta a UI para modo de edição
            ajustarUiParaModoEdicao(true);

        } catch (Exception e) {
            AlertHelper.showError("Erro ao Carregar Cliente", "Não foi possível carregar dados: " + e.getMessage());
            e.printStackTrace();
            limparFormulario();
        }
    }

    /**
     * Ajusta a interface para o modo de edição ou cadastro.
     * @param editando True se editando, False se cadastrando.
     */
    private void ajustarUiParaModoEdicao(boolean editando) {
        // Desabilita campos chave na edição
        idField.setDisable(editando);
        cpfCnpjField.setDisable(editando); // CPF/CNPJ geralmente não muda
        tipoClienteComboBox.setDisable(editando); // Tipo de cliente também não costuma mudar

        // Alterar título (requer um Label com fx:id="tituloLabel" no FXML)
        // if (tituloLabel != null) {
        //     tituloLabel.setText(editando ? "Editar Cliente" : "Cadastrar Novo Cliente");
        // }

        // Alterar texto do botão Salvar
        // Precisa adicionar fx:id="btnSalvarCliente" ao botão Salvar no FXML
        // if (btnSalvarCliente != null) {
        //    btnSalvarCliente.setText(editando ? "Salvar Alterações" : "Salvar");
        // }
    }

    /**
     * Cria um novo objeto Cliente ou atualiza um existente com dados da UI.
     * @return O objeto Cliente populado/atualizado.
     * @throws IllegalArgumentException Se algum campo estiver inválido.
     */
    private Cliente criarOuAtualizarCliente() throws IllegalArgumentException {
        Cliente cliente = (clienteSendoEditado == null) ? new Cliente() : clienteSendoEditado;

        // Pega os dados dos campos (com trim())
        // ID e CPF/CNPJ só são definidos se for novo (pois estão desabilitados na edição)
        if (clienteSendoEditado == null) {
            cliente.setId(idField.getText().trim());
            cliente.setCpfCnpj(cpfCnpjField.getText().trim());
            cliente.setTipoCliente(tipoClienteComboBox.getValue());
        }
        cliente.setNome(nomeField.getText().trim());
        cliente.setLogradouro(logradouroField.getText().trim());
        cliente.setBairro(bairroField.getText().trim());
        cliente.setCidade(cidadeField.getText().trim());
        cliente.setNumero(numeroField.getText().trim());
        cliente.setComplemento(complementoField.getText().trim());
        cliente.setTelefoneCelular(telefoneCelularField.getText().trim());
        cliente.setEmail(emailField.getText().trim());
        cliente.setComprador(compradorField.getText().trim());
        cliente.setGrupo(grupoComboBox.getValue()); // Pega o objeto Grupo selecionado

        // A validação detalhada é feita no Service, mas podemos fazer uma básica aqui
        if (ValidationHelper.isNullOrEmpty(cliente.getId()) && clienteSendoEditado == null) throw new IllegalArgumentException("ID é obrigatório.");
        if (ValidationHelper.isNullOrEmpty(cliente.getNome())) throw new IllegalArgumentException("Nome é obrigatório.");
        // ... (outras validações básicas se desejar)

        return cliente;
    }
}