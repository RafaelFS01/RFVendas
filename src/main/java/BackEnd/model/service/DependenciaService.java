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

    public void salvarDependencia(Dependencia dependencia) throws Exception {
        validarDependencia(dependencia);
        dependenciaDAO.salvarDependencia(dependencia);
    }

    private void validarDependencia(Dependencia dependencia) throws Exception {
        if (dependencia.getIdItemDependente() <= 0 || itemDAO.buscarItemPorId(dependencia.getIdItemDependente()) == null) {
            throw new Exception("Dependência inválida: ID de item dependente inválido ou item não encontrado.");
        }
        if (dependencia.getIdItemNecessario() <= 0 || itemDAO.buscarItemPorId(dependencia.getIdItemNecessario()) == null) {
            throw new Exception("Dependência inválida: ID de item necessário inválido ou item não encontrado.");
        }
        if (dependencia.getIdCategoria() <= 0) {
            throw new Exception("Dependência inválida: ID de categoria inválido ou item não encontrado.");
        }
        if (dependencia.getQuantidade() <= 0) {
            throw new Exception("Dependência inválida: Quantidade deve ser maior que zero.");
        }
    }

    public Dependencia buscarPorId(int id) throws Exception {
        return dependenciaDAO.buscarPorId(id);
    }

    public List<Dependencia> buscarPorIdItemDependente(int idItemDependente) throws Exception {
        return dependenciaDAO.buscarPorIdItemDependente(idItemDependente);
    }

    public void atualizar(Dependencia dependencia) throws Exception {
        if (dependencia.getId() <= 0) {
            throw new Exception("Dependência inválida: ID deve ser maior que zero para atualização.");
        }
        validarDependencia(dependencia);
        dependenciaDAO.atualizar(dependencia);
    }

    public void excluir(int id) throws Exception {
        if (id <= 0) {
            throw new Exception("ID inválido: ID deve ser maior que zero para exclusão.");
        }
        dependenciaDAO.excluir(id);
    }

    public void excluirItem(int id) throws Exception {
        if (id <= 0) {
            throw new Exception("ID inválido: ID deve ser maior que zero para exclusão.");
        }
        dependenciaDAO.excluirItem(id);
    }

    public List<String> atualizarEstoqueItensDependentes(int itemId, Double quantidadeAdicionada) throws Exception {
        List<String> erros = new ArrayList<>();
        List<Dependencia> dependencias = dependenciaDAO.buscarPorIdItemDependente(itemId);

        for (Dependencia dependencia : dependencias) {
            double quantidadeReduzir = quantidadeAdicionada * dependencia.getQuantidade();
            Item itemDependente = itemDAO.buscarItemPorId(dependencia.getIdItemNecessario());

            if (itemDependente.getQuantidadeEstoque() >= quantidadeReduzir) {
                itemDependente.setQuantidadeEstoque(itemDependente.getQuantidadeEstoque() - quantidadeReduzir);
                itemDependente.setQuantidadeAtual(itemDependente.getQuantidadeAtual() - quantidadeReduzir);
                itemDAO.atualizar(itemDependente);
            } else {
                erros.add("Estoque insuficiente para o item dependente: " + itemDependente.getNome() + " (ID: " + itemDependente.getId() + ")");
            }
        }
        return erros;
    }
}