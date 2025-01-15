package BackEnd.model.service;

import BackEnd.model.dao.impl.CategoriaDAOImpl;
import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.interfaces.CategoriaDAO;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.LancamentoItem;
import BackEnd.util.ValidationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemService {

    private final ItemDAO itemDAO;
    private final CategoriaDAO categoriaDAO;
    private final DependenciaService dependenciaService;

    public ItemService() {
        this.itemDAO = new ItemDAOImpl();
        this.categoriaDAO = new CategoriaDAOImpl();
        this.dependenciaService = new DependenciaService();
    }
    public List<Item> listarItensPorCategoria(int idCategoria) throws Exception {
        return itemDAO.listarItensPorCategoria(idCategoria);
    }

    // Outros métodos da classe...

    public List<String> lancarItensNoEstoqueComDependencias(Map<Integer, LancamentoItem> itemQuantidadeMap) throws Exception {
        List<String> erros = new ArrayList<>();

        // Etapa 1: Atualizar a quantidade em estoque dos itens lançados
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            double quantidade = entry.getValue().getQuantidade();
            //Agora você também tem o custo, se precisar dele aqui
            double custo = entry.getValue().getCusto();
            Item item = itemDAO.buscarItemPorId(itemId);
            if (item == null) {
                erros.add("Item com ID " + itemId + " não encontrado.");
                continue;
            }
            item.setPrecoCusto((item.getQuantidadeEstoque() * item.getPrecoCusto() + quantidade * custo)/ (item.getQuantidadeEstoque() + quantidade));
            item.setQuantidadeEstoque(item.getQuantidadeEstoque() + quantidade);
            item.setQuantidadeAtual(item.getQuantidadeAtual() + quantidade);

            itemDAO.atualizar(item);
        }

        // Etapa 2: Atualizar as dependências
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            double quantidade = entry.getValue().getQuantidade();
            //Agora você também tem o custo, se precisar dele aqui
            double custo = entry.getValue().getCusto();

            erros.addAll(dependenciaService.atualizarEstoqueItensDependentes(itemId, quantidade));
        }

        return erros;
    }

    public void salvarItem(Item item) throws Exception {
        validarItem(item);

        // Garantir que as categorias existam e obter seus IDs
        Categoria categoria = categoriaDAO.salvarCategoria(item.getCategoria());

        // Atualizar o item com as categorias persistidas (com IDs)
        item.setCategoria(categoria);
        itemDAO.salvarItem(item);

        // Exportar o banco de dados, se necessário
        // Comentado, pois não é feito pelo service
        // ConnectionFactory.exportarBancoDeDados("backup_database.sql");
    }

    private void validarItem(Item item) throws Exception {
        StringBuilder erros = new StringBuilder();

        if (ValidationHelper.isNullOrEmpty(String.valueOf(item.getId()))) {
            erros.append("O código é obrigatório.\n");
        }

        if (ValidationHelper.isNullOrEmpty(item.getNome())) {
            erros.append("O nome é obrigatório.\n");
        } else if (item.getNome().length() < 3 || item.getNome().length() > 255) {
            erros.append("O nome deve ter entre 3 e 255 caracteres.\n");
        }

        if (ValidationHelper.isNullOrEmpty(item.getDescricao())) {
            erros.append("A descrição é obrigatória.\n");
        } else if (item.getDescricao().length() < 3 || item.getDescricao().length() > 255) {
            erros.append("A descrição deve ter entre 3 e 255 caracteres.\n");
        }

        if (item.getPrecoVenda() == null) {
            erros.append("O preço de venda é obrigatório.\n");
        } else if (item.getPrecoVenda() < 0) {
            erros.append("O preço de venda não pode ser negativo.\n");
        }

        if (item.getPrecoCusto() == null) {
            erros.append("O preço de custo é obrigatório.\n");
        } else if (item.getPrecoCusto() < 0) {
            erros.append("O preço de custo não pode ser negativo.\n");
        }

        if (ValidationHelper.isNullOrEmpty(item.getUnidadeMedida())) {
            erros.append("A unidade de medida é obrigatória.\n");
        } else if (item.getUnidadeMedida().length() < 2 || item.getUnidadeMedida().length() > 5) {
            erros.append("A unidade de medida deve ter entre 2 e 5 caracteres.\n");
        }

        if (item.getQuantidadeEstoque() == null) {
            erros.append("A quantidade de estoque é obrigatória.\n");
        } else if (item.getQuantidadeEstoque() < 0) {
            erros.append("A quantidade de estoque não pode ser negativa.\n");
        }

        if (item.getQuantidadeMinima() == null) {
            erros.append("A quantidade mínima é obrigatória.\n");
        } else if (item.getQuantidadeMinima() < 0) {
            erros.append("A quantidade mínima não pode ser negativa.\n");
        }

        if (item.getQuantidadeAtual() == null) {
            erros.append("A quantidade atual é obrigatória.\n");
        } else if (item.getQuantidadeAtual() < 0) {
            erros.append("A quantidade atual não pode ser negativa.\n");
        }

        if (item.getCategoria() == null) {
            erros.append("Selecione uma categoria.\n");
        }

        if (erros.length() > 0) {
            throw new Exception(erros.toString());
        }
    }

    public boolean verificarItemExistente(String nome) throws Exception {
        return itemDAO.buscarItemPorNome(nome);
    }

    // Adicione outros métodos conforme necessário, como:
    public Item buscarItemPorId(int id) throws Exception {
        return itemDAO.buscarItemPorId(id);
    }

    public List<Item> listarItens() throws Exception {
        return itemDAO.listarItens();
    }

    public void deletar(int id) throws Exception {
        itemDAO.deletar(id);
    }
}