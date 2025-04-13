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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import BackEnd.Main;
import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;
import BackEnd.model.service.CategoriaService;
import BackEnd.model.service.ItemService;
import BackEnd.util.AlertHelper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CadastrarItemController implements Initializable {

    // --- Campos FXML Existentes ---
    @FXML private TextField idField;
    @FXML private TextField nomeField;
    @FXML private TextField descricaoField;
    @FXML private TextField precoVendaField;
    @FXML private TextField precoCustoField;
    @FXML private TextField unidadeMedidaField;
    @FXML private TextField quantidadeEstoqueField;
    @FXML private TextField quantidadeMinimaField;
    @FXML private ComboBox<Categoria> categoriaComboBox;
    @FXML private Label tituloLabel; // Assumindo que existe um Label para o título no FXML com este fx:id

    // --- Campos FXML para Dependências ---
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
    @FXML private Button salvarItem; // Botão principal (Salvar Item / Salvar Alterações)

    // --- Campos FXML para Imagem ---
    @FXML private ImageView imageViewItem;
    @FXML private Button btnSelecionarImagem;
    @FXML private Label lblCaminhoImagem;

    // --- Serviços ---
    private final ItemService itemService;
    private final CategoriaService categoriaService;
    private final DependenciaService dependenciaService;

    // --- Estado Interno ---
    private static int idItemAtual; // Mantido para lógica de dependência original
    private File imagemSelecionadaFile; // Arquivo de imagem recém selecionado
    private Item itemSendoEditado = null; // Guarda o item em modo de edição (null = cadastro)

    // Diretório base para imagens (deve ser consistente com ItemService)
    private static final String IMAGE_BASE_DIRECTORY = System.getProperty("user.home") + "/.RFVendasData/images/items";

    // Método estático para definir o ID do item atual (mantido)
    public void setIdItemAtual(int id) {
        idItemAtual = id;
    }

    // Método estático para obter o ID do item atual (mantido)
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
        limparCampos(); // Garante estado inicial limpo (inclui limpar imagem e setar modo cadastro)
    }

    // Método público chamado pela ListarItensController para carregar dados para edição
    public void carregarItemParaEdicao(int itemId) {
        try {
            this.itemSendoEditado = itemService.buscarItemPorId(itemId);

            if (this.itemSendoEditado == null) {
                AlertHelper.showError("Erro", "Item com ID " + itemId + " não encontrado.");
                limparCampos(); // Volta para o modo de cadastro se item não for encontrado
                return;
            }

            // Preenche os campos da UI
            idField.setText(String.valueOf(itemSendoEditado.getId()));
            nomeField.setText(itemSendoEditado.getNome());
            descricaoField.setText(itemSendoEditado.getDescricao());
            // Formata números para exibição (ex: usando vírgula como separador decimal)
            precoVendaField.setText(itemSendoEditado.getPrecoVenda() != null ? String.format("%.2f", itemSendoEditado.getPrecoVenda()).replace('.', ',') : "");
            precoCustoField.setText(itemSendoEditado.getPrecoCusto() != null ? String.format("%.2f", itemSendoEditado.getPrecoCusto()).replace('.', ',') : "");
            unidadeMedidaField.setText(itemSendoEditado.getUnidadeMedida());
            // Formatar quantidade (considerar se deve ter decimais ou não)
            quantidadeEstoqueField.setText(itemSendoEditado.getQuantidadeEstoque() != null ? String.format("%.2f", itemSendoEditado.getQuantidadeEstoque()).replace('.', ',') : "");
            quantidadeMinimaField.setText(itemSendoEditado.getQuantidadeMinima() != null ? String.format("%.2f", itemSendoEditado.getQuantidadeMinima()).replace('.', ',') : "");

            // Seleciona a categoria
            if (itemSendoEditado.getCategoria() != null) {
                Optional<Categoria> cat = categoriaComboBox.getItems().stream()
                        .filter(c -> c.getId() == itemSendoEditado.getCategoria().getId())
                        .findFirst();
                cat.ifPresent(categoriaComboBox::setValue);
            } else {
                categoriaComboBox.getSelectionModel().clearSelection();
            }

            // Carrega a imagem existente
            carregarImagemExistente(itemSendoEditado.getImagemPath());

            // Ajusta a UI para o modo de edição
            ajustarUiParaModoEdicao(true);

        } catch (Exception e) {
            AlertHelper.showError("Erro ao Carregar Item", "Não foi possível carregar os dados do item: " + e.getMessage());
            e.printStackTrace();
            limparCampos();
        }
    }


    private void configurarCampos() {
        // Listeners para validação/formatação (mantidos e ajustados)
        idField.textProperty().addListener((obs, old, novo) -> { if (novo != null && !novo.matches("\\d*")) idField.setText(novo.replaceAll("[^\\d]", "")); });
        nomeField.textProperty().addListener((obs, old, novo) -> { if (novo != null) nomeField.setText(novo.toUpperCase()); });
        // Permitir ponto ou vírgula para decimais
        String decimalRegex = "\\d*([.,]\\d*)?";
        precoVendaField.textProperty().addListener((obs, old, novo) -> { if (novo != null && !novo.matches(decimalRegex)) precoVendaField.setText(old); });
        precoCustoField.textProperty().addListener((obs, old, novo) -> { if (novo != null && !novo.matches(decimalRegex)) precoCustoField.setText(old); });
        quantidadeEstoqueField.textProperty().addListener((obs, old, novo) -> { if (novo != null && !novo.matches(decimalRegex)) quantidadeEstoqueField.setText(old); });
        quantidadeMinimaField.textProperty().addListener((obs, old, novo) -> { if (novo != null && !novo.matches(decimalRegex)) quantidadeMinimaField.setText(old); });
    }

    private void carregarCategorias() {
        try {
            List<Categoria> categoriasList = categoriaService.listarCategorias();
            ObservableList<Categoria> categorias = FXCollections.observableArrayList(categoriasList);
            categoriaComboBox.setItems(categorias);
            categoriaComboBox.setConverter(new javafx.util.StringConverter<Categoria>() {
                @Override public String toString(Categoria c) { return c == null ? null : c.getNome(); }
                @Override public Categoria fromString(String s) { return null; }
            });
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Carregar Categorias", e.getMessage()); e.printStackTrace();
        }
    }

    /**
     * Ação principal do botão Salvar. Diferencia entre adicionar novo ou atualizar existente.
     */
    @FXML
    private void salvarItem(ActionEvent event) {
        try {
            if (itemSendoEditado == null) {
                // --- MODO CADASTRO ---
                Item novoItem = criarOuAtualizarItem(null); // Cria novo item
                itemService.salvarItem(novoItem);
                setIdItemAtual(novoItem.getId()); // Para dependências

                Optional<ButtonType> result = AlertHelper.showConfirmation(
                        "Item Salvo!",
                        "Item '" + novoItem.getNome() + "' salvo com sucesso.\nDeseja adicionar dependências?",
                        "(Itens necessários para a produção)"
                );

                if (result.isPresent() && result.get() == ButtonType.YES) {
                    prepararUiParaDependencias(true);
                    openAdicionarDependencia(1, null); // Abre primeira janela de dependência
                } else {
                    limparCampos(); // Limpa para próximo cadastro
                }
            } else {
                // --- MODO EDIÇÃO ---
                String imagemAntigaPath = itemSendoEditado.getImagemPath(); // Guarda path antigo
                Item itemAtualizado = criarOuAtualizarItem(itemSendoEditado); // Atualiza objeto existente

                itemService.atualizarItem(itemAtualizado); // Chama serviço de atualização

                // Deleta imagem antiga SE uma nova foi salva com sucesso
                if (imagemSelecionadaFile != null && // Nova imagem foi selecionada
                        itemAtualizado.getImagemPath() != null && // Cópia da nova imagem deu certo
                        !itemAtualizado.getImagemPath().equals(imagemAntigaPath) && // É diferente da antiga
                        imagemAntigaPath != null) { // E existia uma imagem antiga
                    deletarArquivoImagemSilenciosamente(imagemAntigaPath);
                }

                AlertHelper.showSuccess( "Alterações salvas para o item '" + itemAtualizado.getNome() + "'.");
                limparCampos(); // Volta para o modo de cadastro
                // Idealmente, a navegação de volta à lista seria tratada aqui ou sinalizada ao MainController
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Erro de Formato", "Verifique se os campos numéricos estão corretos (use vírgula para decimais).");
        } catch (IOException e) {
            AlertHelper.showError("Erro de Imagem", "Não foi possível processar o arquivo de imagem: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Salvar", "Não foi possível salvar o item: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Popula um objeto Item (novo ou existente) com dados da interface.
     * Processa a imagem selecionada, se houver, copiando-a e definindo o path.
     * @param itemParaAtualizar O item a ser atualizado (se for edição) ou null (se for novo).
     * @return O objeto Item populado/atualizado.
     * @throws NumberFormatException Se campos numéricos forem inválidos.
     * @throws IOException Se ocorrer erro ao copiar a imagem.
     */
    private Item criarOuAtualizarItem(Item itemParaAtualizar) throws NumberFormatException, IOException {
        Item item = (itemParaAtualizar == null) ? new Item() : itemParaAtualizar;
        String pathAntigo = item.getImagemPath(); // Guarda o path existente (pode ser null)

        // Popula com dados da UI
        if (itemParaAtualizar == null) { // Só pega ID se for novo
            item.setId(Integer.parseInt(idField.getText().trim()));
        }
        item.setNome(nomeField.getText().trim().toUpperCase());
        item.setDescricao(descricaoField.getText().trim());
        item.setPrecoVenda(parseDoubleWithComma(precoVendaField.getText()));
        item.setPrecoCusto(parseDoubleWithComma(precoCustoField.getText()));
        item.setUnidadeMedida(unidadeMedidaField.getText().trim().toUpperCase());
        double qtdEstoque = parseDoubleWithComma(quantidadeEstoqueField.getText());
        item.setQuantidadeEstoque(qtdEstoque);
        item.setQuantidadeMinima(parseDoubleWithComma(quantidadeMinimaField.getText()));
        item.setCategoria(categoriaComboBox.getValue());

        // Quantidade Atual: Só define se for novo item
        if (itemParaAtualizar == null) {
            item.setQuantidadeAtual(qtdEstoque);
        }
        // Se for edição, quantidadeAtual não é alterada aqui.

        // Processamento da Imagem: Só copia se uma *nova* imagem foi selecionada
        if (this.imagemSelecionadaFile != null) {
            try {
                Path targetDirectory = Paths.get(IMAGE_BASE_DIRECTORY);
                Files.createDirectories(targetDirectory);

                String originalFileName = imagemSelecionadaFile.getName();
                String fileExtension = "";
                int lastDotIndex = originalFileName.lastIndexOf('.');
                if (lastDotIndex >= 0) fileExtension = originalFileName.substring(lastDotIndex);
                String uniqueFileName = item.getId() + "_" + System.currentTimeMillis() + fileExtension;
                Path targetPath = targetDirectory.resolve(uniqueFileName);

                Files.copy(imagemSelecionadaFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                item.setImagemPath(uniqueFileName); // Define o NOVO path
                System.out.println("Nova imagem copiada para: " + targetPath);

            } catch (IOException e) {
                item.setImagemPath(pathAntigo); // Falha na cópia, reverte para o path antigo
                limparSelecaoImagem(); // Limpa a seleção da UI
                carregarImagemExistente(pathAntigo); // Recarrega a imagem antiga na UI (se existia)
                throw new IOException("Não foi possível processar a nova imagem selecionada.", e); // Relança para o método salvarItem tratar
            }
        } else {
            // Nenhuma nova imagem selecionada, mantém o path existente (que pode ser null)
            item.setImagemPath(pathAntigo);
        }

        return item;
    }

    // Helper para parsear Double permitindo vírgula
    private Double parseDoubleWithComma(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            // Considerar se campos numéricos podem ser nulos ou devem ser 0.0
            // Lançar exceção se for obrigatório, ou retornar null/0.0
            // throw new NumberFormatException("Campo numérico não pode estar vazio.");
            return 0.0; // Ou null, dependendo da regra de negócio
        }
        return Double.parseDouble(value.trim().replace(',', '.'));
    }


    @FXML
    private void salvarDependencia(ActionEvent event) {
        try {
            AlertHelper.showSuccess( "Item e dependências (se adicionadas) foram processados.");
            limparCampos();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Finalizar", e.getMessage());
            e.printStackTrace();
        }
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
        limparSelecaoImagem();

        dependencia1Field.clear(); dependencia2Field.clear();
        dependencia3Field.clear(); dependencia4Field.clear();

        this.itemSendoEditado = null; // Reseta o estado de edição
        this.imagemSelecionadaFile = null; // Reseta arquivo selecionado

        ajustarUiParaModoEdicao(false); // Volta a UI para o modo de cadastro
        prepararUiParaDependencias(false); // Esconde UI de dependências
    }

    private void limparSelecaoImagem() {
        imageViewItem.setImage(null);
        imagemSelecionadaFile = null;
        if (lblCaminhoImagem != null) lblCaminhoImagem.setText("Nenhuma imagem");
    }

    private void carregarImagemExistente(String imagemPath) {
        limparSelecaoImagem(); // Garante que seleção anterior seja limpa
        if (imagemPath != null && !imagemPath.isEmpty()) {
            try {
                Path targetDirectory = Paths.get(IMAGE_BASE_DIRECTORY);
                Path fullPath = targetDirectory.resolve(imagemPath);

                if (Files.exists(fullPath)) {
                    // Carrega a imagem permitindo cache (false) para refletir mudanças se necessário
                    Image image = new Image(fullPath.toUri().toString(), 120, 120, true, false);
                    if (image.isError()) { // Verifica se houve erro ao carregar
                        throw image.getException();
                    }
                    imageViewItem.setImage(image);
                    if (lblCaminhoImagem != null) lblCaminhoImagem.setText(imagemPath);
                } else {
                    System.err.println("Arquivo de imagem não encontrado: " + fullPath);
                    if (lblCaminhoImagem != null) lblCaminhoImagem.setText("Imagem não encontrada");
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar imagem existente '" + imagemPath + "': " + e.getMessage());
                // Não mostrar AlertHelper aqui para não interromper o carregamento do resto
                if (lblCaminhoImagem != null) lblCaminhoImagem.setText("Erro ao carregar");
                // Considerar logar e.printStackTrace();
            }
        }
    }

    /**
     * Ajusta a interface para o modo de edição ou cadastro.
     * @param editando True se estiver editando, False se cadastrando.
     */
    private void ajustarUiParaModoEdicao(boolean editando) {
        // if (tituloLabel != null) tituloLabel.setText(editando ? "Editar Item" : "Cadastrar Novo Item"); // Se tiver label de título
        idField.setDisable(editando); // Desabilita ID na edição
        salvarItem.setText(editando ? "Salvar Alterações" : "Salvar Item");

        // Desabilita adicionar dependências durante a edição (simplificação inicial)
        boolean dependenciasHabilitadas = !editando;
        adicionarDependencia1Button.setDisable(!dependenciasHabilitadas);
        adicionarDependencia2Button.setDisable(!dependenciasHabilitadas);
        adicionarDependencia3Button.setDisable(!dependenciasHabilitadas);
        adicionarDependencia4Button.setDisable(!dependenciasHabilitadas);
        salvarDependencia.setDisable(!dependenciasHabilitadas); // Também o botão de finalizar dependências

        // Garante que a UI de dependência esteja oculta ao mudar de modo
        if (!editando) {
            prepararUiParaDependencias(false);
        }
    }


    private void prepararUiParaDependencias(boolean adicionarDependencias) {
        boolean camposItemDesabilitados = adicionarDependencias;
        idField.setDisable(camposItemDesabilitados);
        nomeField.setDisable(camposItemDesabilitados);
        descricaoField.setDisable(camposItemDesabilitados);
        precoVendaField.setDisable(camposItemDesabilitados);
        precoCustoField.setDisable(camposItemDesabilitados);
        unidadeMedidaField.setDisable(camposItemDesabilitados);
        quantidadeEstoqueField.setDisable(camposItemDesabilitados);
        quantidadeMinimaField.setDisable(camposItemDesabilitados);
        categoriaComboBox.setDisable(camposItemDesabilitados);
        btnSelecionarImagem.setDisable(camposItemDesabilitados);

        salvarItem.setVisible(!adicionarDependencias);
        salvarItem.setManaged(!adicionarDependencias);
        salvarDependencia.setVisible(adicionarDependencias);
        salvarDependencia.setManaged(adicionarDependencias);

        adicionarDependencia1Button.setVisible(adicionarDependencias);
        adicionarDependencia1Button.setManaged(adicionarDependencias);
        adicionarDependencia1Button.setDisable(false); // Habilita o primeiro botão

        // Garante que outros botões/campos de dependência estejam ocultos/desabilitados inicialmente
        if (!adicionarDependencias) {
            configurarBindInicial();
        } else {
            // Oculta os botões 2, 3, 4 ao entrar no modo dependência pela primeira vez
            adicionarDependencia2Button.setVisible(false); adicionarDependencia2Button.setManaged(false);
            adicionarDependencia3Button.setVisible(false); adicionarDependencia3Button.setManaged(false);
            adicionarDependencia4Button.setVisible(false); adicionarDependencia4Button.setManaged(false);
            // Oculta campos/labels 2, 3, 4
            hideDependenciaUI(dependencia2Label, dependencia2Field);
            hideDependenciaUI(dependencia3Label, dependencia3Field);
            hideDependenciaUI(dependencia4Label, dependencia4Field);
            // Se a dependência 1 já foi preenchida antes, mostra o campo/label 1
            if (!dependencia1Field.getText().isEmpty()) {
                showDependenciaUI(dependencia1Label, dependencia1Field);
                adicionarDependencia1Button.setVisible(false); // Esconde botão 1 se já preenchido
                adicionarDependencia1Button.setManaged(false);
                adicionarDependencia2Button.setVisible(true); // Mostra botão 2
                adicionarDependencia2Button.setManaged(true);
            } else {
                hideDependenciaUI(dependencia1Label, dependencia1Field);
            }

        }
    }


    private void configurarBindInicial(){
        adicionarDependencia1Button.setVisible(false); adicionarDependencia1Button.setManaged(false);
        adicionarDependencia2Button.setVisible(false); adicionarDependencia2Button.setManaged(false);
        adicionarDependencia3Button.setVisible(false); adicionarDependencia3Button.setManaged(false);
        adicionarDependencia4Button.setVisible(false); adicionarDependencia4Button.setManaged(false);

        salvarDependencia.setVisible(false); salvarDependencia.setManaged(false);
        salvarItem.setVisible(true); salvarItem.setManaged(true);

        hideDependenciaUI(dependencia1Label, dependencia1Field);
        hideDependenciaUI(dependencia2Label, dependencia2Field);
        hideDependenciaUI(dependencia3Label, dependencia3Field);
        hideDependenciaUI(dependencia4Label, dependencia4Field);
    }

    private void hideDependenciaUI(Label label, TextField field) {
        label.setVisible(false); label.setManaged(false);
        field.setVisible(false); field.setManaged(false);
        field.clear();
    }

    private void showDependenciaUI(Label label, TextField field) {
        label.setVisible(true); label.setManaged(true);
        field.setVisible(true); field.setManaged(true);
    }

    /**
     * Tenta deletar um arquivo de imagem no diretório padrão, silenciosamente.
     * @param nomeArquivoImagem O nome do arquivo a ser deletado.
     */
    private void deletarArquivoImagemSilenciosamente(String nomeArquivoImagem) {
        if (nomeArquivoImagem == null || nomeArquivoImagem.isEmpty()) return;
        try {
            Path targetDirectory = Paths.get(IMAGE_BASE_DIRECTORY);
            Path imagePath = targetDirectory.resolve(nomeArquivoImagem);
            Files.deleteIfExists(imagePath);
            System.out.println("Arquivo de imagem antigo deletado (se existia): " + imagePath);
        } catch (Exception e) {
            System.err.println("Não foi possível deletar arquivo de imagem antigo " + nomeArquivoImagem + ": " + e.getMessage());
        }
    }

    @FXML
    private void abrirCadastroCategoria(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CadastrarCategoria.fxml"));
            Parent root = loader.load();
            CadastrarCategoriaController controller = loader.getController();
            controller.setCategoriaService(categoriaService);

            Stage stage = new Stage();
            stage.setTitle("Cadastrar Nova Categoria");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            if (Main.class.getResourceAsStream("/images/icon.png") != null) {
                stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            }
            stage.showAndWait();
            carregarCategorias();
        } catch (Exception e) {
            AlertHelper.showError("Erro ao Abrir Cadastro", e.getMessage()); e.printStackTrace();
        }
    }

    @FXML
    private void selecionarImagem(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Imagem do Item");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos os Arquivos", "*.*")
        );
        Stage stage = (Stage) btnSelecionarImagem.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                if (selectedFile.length() > 5 * 1024 * 1024) { // Limite de 5MB
                    AlertHelper.showWarning("Arquivo Grande", "Imagem excede 5MB."); return;
                }
                this.imagemSelecionadaFile = selectedFile; // Armazena o ARQUIVO selecionado
                Image image = new Image(selectedFile.toURI().toString(), 120, 120, true, true);
                if (image.isError()) throw image.getException(); // Verifica erro no carregamento da imagem
                imageViewItem.setImage(image);
                if (lblCaminhoImagem != null) lblCaminhoImagem.setText(selectedFile.getName());
            } catch (Exception e) {
                AlertHelper.showError("Erro ao Carregar Imagem", e.getMessage());
                limparSelecaoImagem(); e.printStackTrace();
            }
        }
    }

    // --- Métodos para Abrir Janela de Adicionar Dependência (Refatorados) ---
    @FXML private void abrirAdicionarDependencia1(ActionEvent event) { openAdicionarDependencia(1, event); }
    @FXML private void abrirAdicionarDependencia2(ActionEvent event) { openAdicionarDependencia(2, event); }
    @FXML private void abrirAdicionarDependencia3(ActionEvent event) { openAdicionarDependencia(3, event); }
    @FXML private void abrirAdicionarDependencia4(ActionEvent event) { openAdicionarDependencia(4, event); }

    private void openAdicionarDependencia(int dependenciaIndex, ActionEvent event) {
        try {
            if (idField.getText().trim().isEmpty()) {
                AlertHelper.showWarning("Código Necessário", "Digite o Código do item principal."); return;
            }
            int idItemDependente = Integer.parseInt(idField.getText().trim());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdicionarDependencia.fxml"));
            Parent root = loader.load();
            AdicionarDependenciaController controller = loader.getController();
            controller.setIdItemDependente(idItemDependente);

            controller.setOnDependenciaSalva(itemSelecionado -> {
                switch (dependenciaIndex) {
                    case 1: dependencia1Field.setText(itemSelecionado.getNome()); break;
                    case 2: dependencia2Field.setText(itemSelecionado.getNome()); break;
                    case 3: dependencia3Field.setText(itemSelecionado.getNome()); break;
                    case 4: dependencia4Field.setText(itemSelecionado.getNome()); break;
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Adicionar Dependência " + dependenciaIndex);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            if (Main.class.getResourceAsStream("/images/icon.png") != null) {
                stage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));
            }
            stage.showAndWait();

            Button botaoClicado = (event != null) ? (Button) event.getSource() : null;
            if(botaoClicado != null) {
                botaoClicado.setVisible(false); // Esconde o botão que foi clicado
                botaoClicado.setManaged(false);
            }

            // Mostra o campo/label correspondente e o botão da *próxima* dependência
            switch (dependenciaIndex) {
                case 1:
                    showDependenciaUI(dependencia1Label, dependencia1Field);
                    adicionarDependencia2Button.setVisible(true); adicionarDependencia2Button.setManaged(true);
                    break;
                case 2:
                    showDependenciaUI(dependencia2Label, dependencia2Field);
                    adicionarDependencia3Button.setVisible(true); adicionarDependencia3Button.setManaged(true);
                    break;
                case 3:
                    showDependenciaUI(dependencia3Label, dependencia3Field);
                    adicionarDependencia4Button.setVisible(true); adicionarDependencia4Button.setManaged(true);
                    break;
                case 4:
                    showDependenciaUI(dependencia4Label, dependencia4Field);
                    // Não há botão 5
                    break;
            }

        } catch (NumberFormatException e) {
            AlertHelper.showError("Código Inválido", "Código do item deve ser numérico.");
        } catch (IOException e) {
            AlertHelper.showError("Erro ao Abrir Janela", e.getMessage()); e.printStackTrace();
        } catch (Exception e) {
            AlertHelper.showError("Erro Inesperado", e.getMessage()); e.printStackTrace();
        }
    }
}