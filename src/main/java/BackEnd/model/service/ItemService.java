package BackEnd.model.service;

import BackEnd.model.dao.impl.CategoriaDAOImpl;
import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.interfaces.CategoriaDAO;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.LancamentoItem;
import BackEnd.util.ValidationHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemService {

    private final ItemDAO itemDAO;
    private final CategoriaDAO categoriaDAO;
    private final DependenciaService dependenciaService;

    // Diretório base consistente com o Controller
    private static final String IMAGE_BASE_DIRECTORY = System.getProperty("user.home") + "/.RFVendasData/images/items";

    public ItemService() {
        this.itemDAO = new ItemDAOImpl();
        this.categoriaDAO = new CategoriaDAOImpl();
        this.dependenciaService = new DependenciaService();
    }

    public List<Item> listarItensPorCategoria(int idCategoria) throws Exception {
        return itemDAO.listarItensPorCategoria(idCategoria);
    }

    /**
     * Lança itens no estoque, atualizando quantidades e custo médio.
     * Atualiza também o estoque das dependências.
     * @param itemQuantidadeMap Mapa {ID do Item -> LancamentoItem(quantidade, custo)}
     * @return Lista de erros ocorridos.
     * @throws Exception Em caso de erro geral.
     */
    public List<String> lancarItensNoEstoqueComDependencias(Map<Integer, LancamentoItem> itemQuantidadeMap) throws Exception {
        List<String> erros = new ArrayList<>();

        // Etapa 1: Atualizar estoque/custo dos itens lançados
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            LancamentoItem lancamento = entry.getValue();
            double quantidade = lancamento.getQuantidade();
            double custo = lancamento.getCusto(); // Custo desta entrada específica
            Item item = itemDAO.buscarItemPorId(itemId);

            if (item == null) {
                erros.add("Lançamento: Item ID " + itemId + " não encontrado."); continue;
            }
            if (quantidade <= 0) {
                erros.add("Lançamento: Quantidade para item ID " + itemId + " deve ser positiva."); continue;
            }

            // Cálculo Custo Médio Ponderado
            double estoqueAtual = item.getQuantidadeEstoque() != null ? item.getQuantidadeEstoque() : 0;
            double custoAtualMedio = item.getPrecoCusto() != null ? item.getPrecoCusto() : 0;
            double custoAtualTotal = estoqueAtual * custoAtualMedio;
            double novoCustoTotal = custoAtualTotal + (quantidade * custo);
            double novoEstoqueTotal = estoqueAtual + quantidade;
            double novoPrecoCustoMedio = (novoEstoqueTotal > 0) ? novoCustoTotal / novoEstoqueTotal : custo;

            // Atualiza o item
            item.setPrecoCusto(novoPrecoCustoMedio);
            item.setQuantidadeEstoque(novoEstoqueTotal);
            item.setQuantidadeAtual((item.getQuantidadeAtual() != null ? item.getQuantidadeAtual() : 0) + quantidade);

            try {
                itemDAO.atualizar(item);
            } catch (Exception e) {
                erros.add("Erro ao atualizar item ID " + itemId + " no lançamento: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Etapa 2: Atualizar dependências
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            boolean itemExiste = erros.stream().noneMatch(err -> err.contains("Item ID " + itemId + " não encontrado"));
            if (itemExiste) {
                double quantidade = entry.getValue().getQuantidade();
                try {
                    erros.addAll(dependenciaService.atualizarEstoqueItensDependentes(itemId, quantidade));
                } catch (Exception e) {
                    erros.add("Erro ao processar dependências do item ID " + itemId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return erros;
    }

    /**
     * Salva um novo item após validação.
     * @param item O Item a ser salvo (pode conter imagemPath).
     * @throws Exception Se validação falhar ou erro no DAO.
     */
    public void salvarItem(Item item) throws Exception {
        validarItem(item); // Valida os dados

        // Validação extra: verificar se ID já existe ao *salvar* um novo
        if (itemDAO.buscarItemPorId(item.getId()) != null) {
            throw new Exception("Já existe um item cadastrado com o código " + item.getId() + ".");
        }

        // Garante que categoria é válida
        if (item.getCategoria() == null || item.getCategoria().getId() <= 0) {
            throw new Exception("Categoria inválida ou não selecionada.");
        }
        // (Removida a chamada a categoriaDAO.salvarCategoria, assumindo que a categoria selecionada já existe)

        itemDAO.salvarItem(item); // DAO salva o item (incluindo imagemPath)
    }

    /**
     * Atualiza os dados de um item existente após validação.
     * @param item O objeto Item com os dados atualizados.
     * @throws Exception Se a validação falhar ou ocorrer erro no DAO.
     */
    public void atualizarItem(Item item) throws Exception {
        if (item == null || item.getId() <= 0) {
            throw new Exception("Item inválido para atualização.");
        }
        // Validação pode ser adaptada para atualização (ex: nome único exceto ele mesmo)
        validarItem(item); // Reutiliza a validação básica por enquanto

        // Validação extra: garantir que o item realmente existe antes de tentar atualizar
        if (itemDAO.buscarItemPorId(item.getId()) == null) {
            throw new Exception("Item com código " + item.getId() + " não encontrado para atualização.");
        }

        itemDAO.atualizar(item); // Chama o DAO para atualizar todos os campos
    }


    /**
     * Valida os campos obrigatórios e formatos do objeto Item.
     * @param item O Item a ser validado.
     * @throws Exception Contendo a lista de erros de validação.
     */
    private void validarItem(Item item) throws Exception {
        StringBuilder erros = new StringBuilder();

        if (item == null) throw new Exception("Item não pode ser nulo.");

        // Validações (mantidas e refinadas)
        if (ValidationHelper.isNullOrEmpty(String.valueOf(item.getId())) || item.getId() <= 0) erros.append("O código é obrigatório e deve ser positivo.\n");

        if (ValidationHelper.isNullOrEmpty(item.getNome())) erros.append("O nome é obrigatório.\n");
        else if (item.getNome().length() < 2 || item.getNome().length() > 255) erros.append("O nome deve ter entre 2 e 255 caracteres.\n");
        // Poderia adicionar validação de nome único aqui, chamando itemDAO.buscarItemPorNome,
        // mas precisa diferenciar entre salvar (nome não pode existir) e atualizar (nome pode existir se for o próprio item).

        if (ValidationHelper.isNullOrEmpty(item.getDescricao())) erros.append("A descrição é obrigatória.\n");
        else if (item.getDescricao().length() > 500) erros.append("A descrição excede 500 caracteres.\n");

        if (item.getPrecoVenda() == null) erros.append("Preço de venda é obrigatório.\n");
        else if (item.getPrecoVenda() < 0) erros.append("Preço de venda não pode ser negativo.\n");

        if (item.getPrecoCusto() == null) erros.append("Preço de custo é obrigatório.\n");
        else if (item.getPrecoCusto() < 0) erros.append("Preço de custo não pode ser negativo.\n");

        if (ValidationHelper.isNullOrEmpty(item.getUnidadeMedida())) erros.append("Unidade de medida é obrigatória.\n");
        else if (item.getUnidadeMedida().length() < 1 || item.getUnidadeMedida().length() > 10) erros.append("Unidade de medida inválida (1-10 caracteres).\n");

        if (item.getQuantidadeEstoque() == null) erros.append("Quantidade de estoque é obrigatória.\n");
        else if (item.getQuantidadeEstoque() < 0) erros.append("Quantidade de estoque não pode ser negativa.\n");

        if (item.getQuantidadeMinima() == null) erros.append("Quantidade mínima é obrigatória.\n");
        else if (item.getQuantidadeMinima() < 0) erros.append("Quantidade mínima não pode ser negativa.\n");

        // Validação quantidadeAtual: Não negativa.
        if (item.getQuantidadeAtual() != null && item.getQuantidadeAtual() < 0) {
            erros.append("Quantidade atual não pode ser negativa.\n");
        }
        // Nota: Não valida se qtdAtual > qtdEstoque, pois pode haver saídas.

        if (item.getCategoria() == null || item.getCategoria().getId() <= 0) erros.append("Selecione uma categoria válida.\n");

        if (item.getImagemPath() != null && item.getImagemPath().length() > 512) {
            erros.append("Nome/caminho da imagem excede 512 caracteres.\n");
        }

        if (erros.length() > 0) {
            throw new Exception("Erros de validação:\n" + erros.toString());
        }
    }

    public boolean verificarItemExistente(String nome) throws Exception {
        return itemDAO.buscarItemPorNome(nome);
    }

    public Item buscarItemPorId(int id) throws Exception {
        return itemDAO.buscarItemPorId(id);
    }

    public List<Item> listarItens() throws Exception {
        return itemDAO.listarItens();
    }

    /**
     * Deleta um item e tenta excluir o arquivo de imagem associado.
     * @param id O ID do item a ser deletado.
     * @throws Exception Se ocorrer erro.
     */
    public void deletar(int id) throws Exception {
        Item itemParaDeletar = itemDAO.buscarItemPorId(id);

        if (itemParaDeletar == null) {
            // Item já não existe? Considerar logar ou retornar silenciosamente.
            System.out.println("Item ID " + id + " já não existe no banco para exclusão.");
            return; // Ou throw new Exception("Item com ID " + id + " não encontrado.");
        }

        // Tenta deletar o arquivo de imagem associado
        if (itemParaDeletar.getImagemPath() != null && !itemParaDeletar.getImagemPath().trim().isEmpty()) {
            try {
                Path targetDirectory = Paths.get(IMAGE_BASE_DIRECTORY);
                Path imagePath = targetDirectory.resolve(itemParaDeletar.getImagemPath());
                Files.deleteIfExists(imagePath);
                System.out.println("Arquivo de imagem deletado (ou não encontrado): " + imagePath);
            } catch (NoSuchFileException e) {
                System.out.println("Arquivo de imagem não encontrado para deletar: " + itemParaDeletar.getImagemPath());
            } catch (IOException | SecurityException e) { // Captura ambos
                System.err.println("Erro ao tentar deletar arquivo de imagem " + itemParaDeletar.getImagemPath() + ": " + e.getMessage());
                // Não impedir a exclusão do item do DB por causa disso, a menos que seja regra de negócio.
            }
        }

        // Deleta o item do banco de dados
        try {
            itemDAO.deletar(id);
        } catch (Exception e) {
            // Poderia tratar erro de FK aqui se item estiver em um pedido, etc.
            throw new Exception("Erro ao deletar item do banco de dados: " + e.getMessage(), e);
        }
    }
}