package BackEnd.controller;

import javafx.application.Platform; // Import necessário para Platform.runLater
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task; // Import necessário para Task
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos; // Para alinhamento no HBox
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser; // Import necessário para FileChooser
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window; // Para obter o Stage de forma mais genérica

import BackEnd.model.entity.Usuario;
import BackEnd.model.service.UsuarioService;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;
import BackEnd.exception.BackupRestoreException; // Import da exceção customizada

import java.io.File; // Import necessário para File
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException; // Pode ser necessário
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class GerenciamentoPermissoesController implements Initializable {

    @FXML private TextField campoBusca;
    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, String> colunaUsername;
    @FXML private TableColumn<Usuario, String> colunaNome;
    @FXML private TableColumn<Usuario, String> colunaEmail;
    @FXML private TableColumn<Usuario, String> colunaNivel;
    @FXML private TableColumn<Usuario, String> colunaStatus;
    @FXML private TableColumn<Usuario, Void> colunaAcoes;
    @FXML private Label statusLabel;

    // Injeção dos novos botões
    @FXML private Button btnExportarBackup;
    @FXML private Button btnImportarBackup;
    @FXML private Button btnNovoUsuario; // Injetado para poder desabilitar
    @FXML private Button btnAtualizarLista; // Injetado para poder desabilitar

    private final UsuarioService usuarioService;
    private ObservableList<Usuario> usuarios; // A lista base, carregada do banco
    private FilteredList<Usuario> dadosFiltrados; // A lista filtrada para a tabela

    public GerenciamentoPermissoesController() {
        this.usuarioService = new UsuarioService();
        // Inicializa a lista base vazia para evitar NullPointerException antes da primeira carga
        this.usuarios = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarPesquisa(); // Configura o listener da busca
        carregarUsuarios(); // Carrega os dados iniciais (isso vai criar o FilteredList e setar na tabela)
        // O atualizarStatusLabel é chamado dentro de carregarUsuarios e filtrarTabela
    }

    private void configurarColunas() {
        // Configuração das CellValueFactory (como antes)
        colunaUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        colunaNome.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNome()));
        colunaEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        colunaNivel.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getNivelAcesso())));
        colunaStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().isAtivo() ? "Ativo" : "Inativo"));

        // Estilização da coluna Status
        colunaStatus.setCellFactory(column -> new TableCell<Usuario, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-ativo", "status-inativo"); // Limpa classes CSS
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Ativo")) {
                        getStyleClass().add("status-ativo");
                        // setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;"); // Fallback
                    } else {
                        getStyleClass().add("status-inativo");
                        // setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;"); // Fallback
                    }
                }
            }
        });

        // Configuração da coluna Ações
        colunaAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnAlterarStatus = new Button("Alterar Status");
            // HBox para agrupar os botões
            private final HBox box = new HBox(5, btnEditar, btnAlterarStatus);

            {
                // Aplica estilos CSS
                btnEditar.getStyleClass().addAll("btn-edit", "btn-sm");
                btnAlterarStatus.getStyleClass().addAll("btn-warning", "btn-sm"); // Usando btn-warning como exemplo
                box.setAlignment(Pos.CENTER); // Centraliza os botões na célula

                // Ações dos botões
                btnEditar.setOnAction(event -> {
                    Usuario usuario = getTableRow().getItem();
                    if (usuario != null) {
                        editarUsuario(usuario);
                    }
                });

                btnAlterarStatus.setOnAction(event -> {
                    Usuario usuario = getTableRow().getItem();
                    if (usuario != null) {
                        alterarStatusUsuario(usuario);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // Configura apenas o listener do campo de busca
    private void configurarPesquisa() {
        campoBusca.textProperty().addListener((obs, old, novo) -> {
            // Verifica se dadosFiltrados não é null antes de usar
            if (dadosFiltrados != null) {
                filtrarTabela(novo);
            }
        });
    }

    // Carrega/recarrega os usuários do banco e atualiza a tabela
    private void carregarUsuarios() {
        try {
            Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
            String termoBusca = campoBusca.getText();

            // 1. Carrega a NOVA lista base
            usuarios = FXCollections.observableArrayList(usuarioService.listarTodos());

            // 2. RECRIE o FilteredList para usar a nova lista base
            dadosFiltrados = new FilteredList<>(usuarios, p -> true);

            // 3. Define o NOVO FilteredList na TableView
            tabelaUsuarios.setItems(dadosFiltrados);

            // 4. Reaplica o filtro
            filtrarTabela(termoBusca == null ? "" : termoBusca);

            // 5. Tenta restaurar a seleção (procurando por ID)
            restaurarSelecao(selecionado);

            // 6. Atualiza o status (já chamado por filtrarTabela)
            // atualizarStatusLabel();

        } catch (Exception e) {
            handleCarregarUsuariosError(e);
        }
    }

    // Método auxiliar para tratar erros ao carregar usuários
    private void handleCarregarUsuariosError(Exception e) {
        AlertHelper.showError("Erro ao Carregar Usuários", "Não foi possível carregar a lista de usuários: " + e.getMessage());
        e.printStackTrace();
        // Limpa a tabela de forma segura
        if (usuarios != null) usuarios.clear();
        if (tabelaUsuarios != null) tabelaUsuarios.getItems().clear();
        if (dadosFiltrados == null) { // Garante que não seja null
            dadosFiltrados = new FilteredList<>(usuarios, p -> true);
            tabelaUsuarios.setItems(dadosFiltrados);
        }
        atualizarStatusLabel();
    }

    // Método auxiliar para reaplicar o filtro
    private void filtrarTabela(String filtro) {
        String lowerCaseFilter = filtro == null ? "" : filtro.toLowerCase().trim();

        if (dadosFiltrados == null) {
            // Proteção caso carregarUsuarios falhe antes de inicializar dadosFiltrados
            dadosFiltrados = new FilteredList<>(usuarios, p -> true);
            tabelaUsuarios.setItems(dadosFiltrados);
        }

        dadosFiltrados.setPredicate(usuario -> {
            if (lowerCaseFilter.isEmpty()) {
                return true;
            }
            // Adiciona verificações de null para os campos do usuário
            boolean usernameMatch = usuario.getUsername() != null && usuario.getUsername().toLowerCase().contains(lowerCaseFilter);
            boolean nomeMatch = usuario.getNome() != null && usuario.getNome().toLowerCase().contains(lowerCaseFilter);
            boolean emailMatch = usuario.getEmail() != null && usuario.getEmail().toLowerCase().contains(lowerCaseFilter);
            return usernameMatch || nomeMatch || emailMatch;
        });
        atualizarStatusLabel(); // Atualiza contagem após filtrar
    }

    // Método auxiliar para tentar restaurar a seleção após recarregar
    private void restaurarSelecao(Usuario usuarioSelecionadoAnteriormente) {
        if (usuarioSelecionadoAnteriormente != null && usuarios != null) {
            // Tenta encontrar o usuário na nova lista pelo ID (mais confiável)
            Usuario usuarioEquivalente = usuarios.stream()
                    .filter(u -> u.getId() == usuarioSelecionadoAnteriormente.getId()) // Precisa de getId() no Usuario
                    .findFirst()
                    .orElse(null);

            if (usuarioEquivalente != null && dadosFiltrados.contains(usuarioEquivalente)) {
                // Seleciona apenas se encontrado e ainda visível após filtro
                tabelaUsuarios.getSelectionModel().select(usuarioEquivalente);
                tabelaUsuarios.scrollTo(tabelaUsuarios.getSelectionModel().getSelectedIndex());
            } else {
                tabelaUsuarios.getSelectionModel().clearSelection();
            }
        } else {
            tabelaUsuarios.getSelectionModel().clearSelection();
        }
    }


    @FXML
    private void novoUsuario() {
        abrirDialogoUsuario(null);
    }

    @FXML
    private void atualizarLista() {
        campoBusca.clear(); // Limpa o campo, o que dispara o listener para remover o filtro
        carregarUsuarios(); // Recarrega do banco e atualiza a tabela
    }

    private void editarUsuario(Usuario usuario) {
        abrirDialogoUsuario(usuario);
    }

    private void abrirDialogoUsuario(Usuario usuario) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = getStage(); // Tenta obter a janela dona
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CadastrarUsuario.fxml"));
            Scene scene = new Scene(loader.load());

            URL cssResource = getClass().getResource("/styles/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("AVISO: Arquivo CSS /styles/styles.css não encontrado.");
            }

            CadastrarUsuarioController controller = loader.getController();
            dialogStage.setTitle(usuario == null ? "Novo Usuário" : "Editar Usuário");
            if (usuario != null) {
                controller.setUsuario(usuario);
            }

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            carregarUsuarios(); // Atualiza a lista após fechar o diálogo

        } catch (IOException e) {
            AlertHelper.showError("Erro ao Abrir Formulário", "Não foi possível carregar o formulário: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Erro Inesperado", "Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void alterarStatusUsuario(Usuario usuario) {
        String acao = usuario.isAtivo() ? "desativar" : "ativar";
        String novoStatus = usuario.isAtivo() ? "Inativo" : "Ativo";

        Optional<ButtonType> confirmacao = AlertHelper.showConfirmation(
                "Confirmar Alteração de Status",
                "Tem certeza que deseja " + acao + " o usuário '" + usuario.getUsername() + "'?",
                "O status será alterado para " + novoStatus + "."
        );

        if (confirmacao.isPresent() && confirmacao.get() == ButtonType.YES) {
            try {
                usuario.setAtivo(!usuario.isAtivo());
                usuarioService.atualizarStatus(usuario);
                // Backup automático foi removido daqui
                carregarUsuarios(); // Recarrega para refletir a mudança
                AlertHelper.showSuccess( "Status do usuário '" + usuario.getUsername() + "' alterado para " + novoStatus + ".");
            } catch (Exception e) {
                AlertHelper.showError("Erro ao Alterar Status", "Não foi possível alterar o status: " + e.getMessage());
                e.printStackTrace();
                // Reverte a mudança no objeto local se a persistência falhou
                usuario.setAtivo(!usuario.isAtivo());
                // Opcional: recarregar para garantir consistência visual
                carregarUsuarios();
            }
        }
    }

    private void atualizarStatusLabel() {
        int totalVisivel = (dadosFiltrados != null) ? dadosFiltrados.size() : 0;
        long totalAtivos = (dadosFiltrados != null) ? dadosFiltrados.stream().filter(Usuario::isAtivo).count() : 0;
        long totalInativos = totalVisivel - totalAtivos;
        long totalGeral = (usuarios != null) ? usuarios.size() : 0;

        statusLabel.setText(String.format(
                "Exibindo: %d | Ativos: %d | Inativos: %d (Total Geral: %d)",
                totalVisivel, totalAtivos, totalInativos, totalGeral
        ));
    }

    // --- Métodos para Backup/Restore ---

    @FXML
    private void handleExportarBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Backup do Banco de Dados");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setInitialFileName("backup_rfvendas_" + timestamp + ".sql");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos SQL (*.sql)", "*.sql")
        );

        Stage stage = getStage();
        if (stage == null) return;

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            executarTarefaBackupRestore(true, file.getAbsolutePath()); // true para exportar
        } else {
            statusLabel.setText("Exportação de backup cancelada.");
        }
    }

    @FXML
    private void handleImportarBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Arquivo SQL para Importar");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos SQL (*.sql)", "*.sql")
        );

        Stage stage = getStage();
        if (stage == null) return;

        File file = fileChooser.showOpenDialog(stage);

        if (file != null && file.exists()) {
            Optional<ButtonType> confirmacao = AlertHelper.showConfirmation(
                    "Confirmar Importação de Backup",
                    "ATENÇÃO: Importar este backup substituirá TODOS os dados atuais!",
                    "Deseja realmente importar o arquivo '" + file.getName() + "'? Esta ação NÃO PODE ser desfeita."
            );

            if (confirmacao.isPresent() && confirmacao.get() == ButtonType.YES) {
                executarTarefaBackupRestore(false, file.getAbsolutePath()); // false para importar
            } else {
                statusLabel.setText("Importação de backup cancelada.");
            }
        } else if (file != null) {
            AlertHelper.showWarning("Arquivo Não Encontrado", "Arquivo selecionado não encontrado: " + file.getAbsolutePath());
            statusLabel.setText("Importação cancelada: arquivo não encontrado.");
        } else {
            statusLabel.setText("Importação de backup cancelada.");
        }
    }

    private void executarTarefaBackupRestore(boolean isExport, String filePath) {
        String actionVerb = isExport ? "Exportando" : "Importando";
        String actionNoun = isExport ? "exportação" : "importação";

        setControlsDisabled(true); // Desabilita controles durante a operação
        statusLabel.setText(actionVerb + " banco de dados...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception { // Permite lançar exceções
                updateMessage(actionVerb + " para/de: " + filePath + "...");
                try {
                    if (isExport) {
                        ConnectionFactory.exportarBancoDeDados(filePath);
                    } else {
                        ConnectionFactory.importarBancoDeDados(filePath);
                    }
                    updateMessage(actionNoun.substring(0, 1).toUpperCase() + actionNoun.substring(1) + " concluída com sucesso!");
                } catch (IOException | InterruptedException | BackupRestoreException e) {
                    // Captura as exceções esperadas e definidas
                    updateMessage("Falha na " + actionNoun + "."); // Mensagem mais curta para UI
                    // Log detalhado no console será feito no setOnFailed
                    throw e; // Relança para setOnFailed
                } catch (Exception e) {
                    updateMessage("Erro inesperado durante a " + actionNoun + ".");
                    throw new BackupRestoreException("Erro inesperado: " + e.getMessage(), e); // Encapsula erro inesperado
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                statusLabel.setText(task.getMessage()); // Pega a última mensagem do updateMessage
                AlertHelper.showSuccess(actionNoun.substring(0, 1).toUpperCase() + actionNoun.substring(1) + " Concluída " +
                        "O banco de dados foi " + (isExport ? "exportado" : "importado") + " com sucesso.");
                setControlsDisabled(false);
                if (!isExport) {
                    carregarUsuarios(); // Recarrega tudo após importar
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                Throwable exception = task.getException();
                String errorTitle = "Erro na " + actionNoun.substring(0, 1).toUpperCase() + actionNoun.substring(1);
                String errorMsg = "Ocorreu um erro durante a " + actionNoun + ".";
                if (exception != null) {
                    // Usa a mensagem da exceção capturada (que já formatamos na ConnectionFactory ou no catch do call)
                    errorMsg += "\nDetalhes: " + exception.getMessage();
                    System.err.println(errorTitle + ":"); // Log no console
                    exception.printStackTrace();
                }
                statusLabel.setText("Falha na " + actionNoun + ".");
                AlertHelper.showError(errorTitle, errorMsg);
                setControlsDisabled(false);
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // Método auxiliar para habilitar/desabilitar controles durante operações longas
    private void setControlsDisabled(boolean disabled) {
        // Desabilita/habilita botões principais da toolbar
        if (btnNovoUsuario != null) btnNovoUsuario.setDisable(disabled);
        if (btnAtualizarLista != null) btnAtualizarLista.setDisable(disabled);
        if (btnExportarBackup != null) btnExportarBackup.setDisable(disabled);
        if (btnImportarBackup != null) btnImportarBackup.setDisable(disabled);
        // Desabilita/habilita campo de busca e tabela
        if (campoBusca != null) campoBusca.setDisable(disabled);
        if (tabelaUsuarios != null) tabelaUsuarios.setDisable(disabled);
    }

    // Método auxiliar para obter o Stage (janela) atual
    private Stage getStage() {
        // Tenta obter do componente mais provável de estar na cena
        Node node = tabelaUsuarios != null ? tabelaUsuarios : statusLabel;
        if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) {
            return (Stage) node.getScene().getWindow();
        } else {
            System.err.println("Não foi possível obter o Stage atual para exibir o FileChooser/Dialog.");
            // Pode retornar null ou tentar uma abordagem mais global se tiver uma referência estática ao Stage principal
            return null;
        }
    }
}