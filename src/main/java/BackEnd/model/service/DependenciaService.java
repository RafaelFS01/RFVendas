package BackEnd.model.service;

import BackEnd.model.dao.impl.DependenciaDAOImpl;
import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.interfaces.DependenciaDAO;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.entity.Dependencia;
import BackEnd.model.entity.Item;

import java.util.ArrayList;
import java.util.List;

public class DependenciaService {

    private DependenciaDAO dependenciaDAO;
    private ItemDAO itemDAO;

    public DependenciaService() {
        this.dependenciaDAO = new DependenciaDAOImpl();
        this.itemDAO = new ItemDAOImpl();
    }

    // Construtor para Injeção de Dependência (Opcional)
    public DependenciaService(DependenciaDAO dependenciaDAO, ItemDAO itemDAO) {
        this.dependenciaDAO = dependenciaDAO;
        this.itemDAO = itemDAO;
    }

    public void salvarDependencia(Dependencia dependencia) throws Exception {
        validarDependencia(dependencia);
        dependenciaDAO.salvarDependencia(dependencia);
    }

    public Dependencia buscarDependenciaPorId(int id) throws Exception {
        return dependenciaDAO.buscarPorId(id);
    }

    public List<Dependencia> listarDependenciasPorProduto(int idProduto) throws Exception {
        return dependenciaDAO.buscarPorIdProdutoDependente(idProduto);
    }

    public void atualizarDependencia(Dependencia dependencia) throws Exception {
        validarDependencia(dependencia);
        dependenciaDAO.atualizar(dependencia);
    }

    public void excluirDependencia(int id) throws Exception {
        dependenciaDAO.excluir(id);
    }

    public void excluirDependenciasDeProduto(int idProduto) throws Exception {
        dependenciaDAO.excluirProduto(idProduto);
    }

    private void validarDependencia(Dependencia dependencia) throws Exception {
        // Adicionar validações para Dependencia, se necessário
        if (dependencia.getIdItemDependente() <= 0) {
            throw new Exception("O ID do produto dependente é obrigatório.");
        }
        if (dependencia.getIdItemNecessario() <= 0) {
            throw new Exception("O ID do produto necessário é obrigatório.");
        }
        if (dependencia.getQuantidade() <= 0) {
            throw new Exception("A quantidade deve ser maior que zero.");
        }
    }

    public List<String> atualizarEstoqueItensDependentes(int idProduto, double quantidadeProduzida) throws Exception {
        List<String> erros = new ArrayList<>();
        List<Dependencia> dependencias = listarDependenciasPorProduto(idProduto);

        for (Dependencia dependencia : dependencias) {
            Item produtoNecessario = itemDAO.buscarItemPorId(dependencia.getIdItemNecessario());
            double quantidadeNecessaria = dependencia.getQuantidade() * quantidadeProduzida;

            if ("ITEM".equals(produtoNecessario.getTipoProduto())) {
                if (produtoNecessario.getQuantidadeAtual() < quantidadeNecessaria) {
                    erros.add("Quantidade insuficiente em estoque para o item: " + produtoNecessario.getNome());
                } else {
                    produtoNecessario.setQuantidadeAtual(produtoNecessario.getQuantidadeAtual() - quantidadeNecessaria);
                    itemDAO.atualizar(produtoNecessario);
                }
            } else if ("SERVICO".equals(produtoNecessario.getTipoProduto())) {
                // Lógica para quando a dependência for um serviço (se aplicável)
            } else {
                erros.add("Tipo de produto inválido na dependência para o produto: " + produtoNecessario.getNome());
            }
        }

        return erros;
    }
}