package BackEnd.model.service;

import BackEnd.model.dao.impl.GrupoDAOImpl;
import BackEnd.model.dao.interfaces.GrupoDAO;
import BackEnd.model.entity.Grupo;
import BackEnd.util.ValidationHelper;

import java.util.List;

public class GrupoService {

    private final GrupoDAO grupoDAO;

    public GrupoService() {
        this.grupoDAO = new GrupoDAOImpl();
    }

    public List<Grupo> listarGrupos() throws Exception {
        try {
            return grupoDAO.listarTodos();
        } catch (Exception e) {
            throw new Exception("Erro ao listar grupos: " + e.getMessage());
        }
    }

    public void cadastrarGrupo(String nomeGrupo) throws Exception {
        if (ValidationHelper.isNullOrEmpty(nomeGrupo)) {
            throw new IllegalArgumentException("Nome do grupo não pode ser vazio.");
        }

        if (grupoDAO.existePorNome(nomeGrupo)) {
            throw new IllegalArgumentException("Já existe um grupo com esse nome.");
        }

        try {
            Grupo grupo = new Grupo();
            grupo.setNome(nomeGrupo);
            grupoDAO.criar(grupo);
        } catch (Exception e) {
            throw new Exception("Erro ao cadastrar grupo: " + e.getMessage());
        }
    }

    public void deletar(int id) throws Exception{
        grupoDAO.deletar(id);
    }
}