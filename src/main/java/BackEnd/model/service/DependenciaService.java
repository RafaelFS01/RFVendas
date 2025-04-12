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

    // Construtor para Inje��o de Depend�ncia (Opcional)
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
        // Adicionar valida��es para Dependencia, se necess�rio
        if (dependencia.getIdItemDependente() <= 0) {
            throw new Exception("O ID do produto dependente � obrigat�rio.");
        }
        if (dependencia.getIdItemNecessario() <= 0) {
            throw new Exception("O ID do produto necess�rio � obrigat�rio.");
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
                // L�gica para quando a depend�ncia for um servi�o (se aplic�vel)
            } else {
                erros.add("Tipo de produto inv�lido na depend�ncia para o produto: " + produtoNecessario.getNome());
            }
        }

        return erros;
    }

    // M�todos adicionados para compatibilidade com controllers e services

    public List<Dependencia> buscarPorIdItemDependente(int id) throws Exception {
        try {
            return dependenciaDAO.buscarPorIdProdutoDependente(id);
        } catch (Exception e) {
            throw new Exception("Erro ao buscar depend�ncias do item dependente: " + id, e);
        }
    }

    public void excluirItem(int id) throws Exception {
        try {
            dependenciaDAO.excluirProduto(id);
        } catch (Exception e) {
            throw new Exception("Erro ao excluir depend�ncias do item: " + id, e);
        }
    }
}