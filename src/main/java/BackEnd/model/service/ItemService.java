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

    public List<String> lancarItensNoEstoqueComDependencias(Map<Integer, LancamentoItem> itemQuantidadeMap) throws Exception {
        List<String> erros = new ArrayList<>();

        // Etapa 1: Atualizar a quantidade em estoque dos itens lançados
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            double quantidade = entry.getValue().getQuantidade();
            double custo = entry.getValue().getCusto();

            Item item = itemDAO.buscarItemPorId(itemId);
            if (item == null) {
                erros.add("Item com ID " + itemId + " não encontrado.");
                continue;
            }

            // Verifica se o item é realmente um item e não um serviço
            if ("ITEM".equals(item.getTipoProduto())) {
                item.setPrecoCusto((item.getQuantidadeEstoque() * item.getPrecoCusto() + quantidade * custo) / (item.getQuantidadeEstoque() + quantidade));
                item.setQuantidadeEstoque(item.getQuantidadeEstoque() + quantidade);
                item.setQuantidadeAtual(item.getQuantidadeAtual() + quantidade);
                itemDAO.atualizar(item);
            } else {
                erros.add("O ID " + itemId + " refere-se a um serviço e não pode ter seu estoque atualizado.");
            }
        }

        // Etapa 2: Atualizar as dependências
        for (Map.Entry<Integer, LancamentoItem> entry : itemQuantidadeMap.entrySet()) {
            int itemId = entry.getKey();
            double quantidade = entry.getValue().getQuantidade();

            erros.addAll(dependenciaService.atualizarEstoqueItensDependentes(itemId, quantidade));
        }

        return erros;
    }

    public void salvarItem(Item item) throws Exception {
        if (!"ITEM".equals(item.getTipoProduto())) {
            throw new Exception("O produto fornecido não é um Item.");
        }
        validarItem(item);
        itemDAO.salvarItem(item);
    }

    public void salvarServico(Item servico) throws Exception {
        if (!"SERVICO".equals(servico.getTipoProduto())) {
            throw new Exception("O produto fornecido não é um Serviço.");
        }
        validarServico(servico);
        itemDAO.salvarItem(servico); // Ainda sim, usa-se o itemDAO, pois a estrutura é a mesma.
    }

    public void atualizarItem(Item item) throws Exception {
        if ("ITEM".equals(item.getTipoProduto())) {
            validarItem(item);
        } else if ("SERVICO".equals(item.getTipoProduto())) {
            validarServico(item);
        } else {
            throw new Exception("Tipo de produto inválido.");
        }
        itemDAO.atualizar(item);
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

        if (item.getPrecoVenda() == null || item.getPrecoVenda() < 0) {
            erros.append("O preço de venda é obrigatório e não pode ser negativo.\n");
        }

        if (item.getPrecoCusto() == null || item.getPrecoCusto() < 0) {
            erros.append("O preço de custo é obrigatório e não pode ser negativo.\n");
        }

        if (item.getUnidadeMedida() == null || item.getUnidadeMedida().trim().isEmpty()) {
            erros.append("A unidade de medida é obrigatória.\n");
        } else if (item.getUnidadeMedida().length() < 2 || item.getUnidadeMedida().length() > 5) {
            erros.append("A unidade de medida deve ter entre 2 e 5 caracteres.\n");
        }

        if (item.getQuantidadeEstoque() == null || item.getQuantidadeEstoque() < 0) {
            erros.append("A quantidade de estoque é obrigatória e não pode ser negativa.\n");
        }

        if (item.getQuantidadeMinima() == null || item.getQuantidadeMinima() < 0) {
            erros.append("A quantidade mínima é obrigatória e não pode ser negativa.\n");
        }

        if (item.getQuantidadeAtual() == null || item.getQuantidadeAtual() < 0) {
            erros.append("A quantidade atual é obrigatória e não pode ser negativa.\n");
        }

        if (item.getCategoria() == null) {
            erros.append("Selecione uma categoria.\n");
        }

        if (erros.length() > 0) {
            throw new Exception(erros.toString());
        }
    }

    private void validarServico(Item servico) throws Exception {
        StringBuilder erros = new StringBuilder();

        // Validações para Serviço
        if (ValidationHelper.isNullOrEmpty(String.valueOf(servico.getId()))) {
            erros.append("O código do serviço é obrigatório.\n");
        }

        if (ValidationHelper.isNullOrEmpty(servico.getNome())) {
            erros.append("O nome do serviço é obrigatório.\n");
        } else if (servico.getNome().length() < 3 || servico.getNome().length() > 255) {
            erros.append("O nome do serviço deve ter entre 3 e 255 caracteres.\n");
        }

        if (ValidationHelper.isNullOrEmpty(servico.getDescricao())) {
            erros.append("A descrição do serviço é obrigatória.\n");
        } else if (servico.getDescricao().length() < 3 || servico.getDescricao().length() > 255) {
            erros.append("A descrição do serviço deve ter entre 3 e 255 caracteres.\n");
        }

        if (servico.getPrecoVenda() == null || servico.getPrecoVenda() < 0) {
            erros.append("O preço de venda do serviço é obrigatório e não pode ser negativo.\n");
        }

        if (servico.getPrecoCusto() == null || servico.getPrecoCusto() < 0) {
            erros.append("O preço de custo do serviço é obrigatório e não pode ser negativo.\n");
        }

        if (servico.getCategoria() == null) {
            erros.append("Selecione uma categoria para o serviço.\n");
        }

        // Adicione outras validações específicas para serviços aqui

        if (erros.length() > 0) {
            throw new Exception(erros.toString());
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

    public void deletar(int id) throws Exception {
        itemDAO.deletar(id);
    }
}