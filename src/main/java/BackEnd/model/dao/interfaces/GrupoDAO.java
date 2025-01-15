package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Grupo;
import java.util.List;

public interface GrupoDAO {
    void criar(Grupo grupo) throws Exception;
    Grupo buscarPorId(int id) throws Exception;
    Grupo buscarPorNome(String nome) throws Exception;
    List<Grupo> listarTodos() throws Exception;
    void atualizar(Grupo grupo) throws Exception;
    void deletar(int id) throws Exception;
    boolean existePorId(int id) throws Exception;

    boolean existePorNome(String nome) throws Exception;
}