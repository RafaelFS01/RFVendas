package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Dependencia;

import java.sql.SQLException;
import java.util.List;

public interface DependenciaDAO {
    void salvarDependencia(Dependencia dependencia) throws SQLException, Exception;
    Dependencia buscarPorId(int id) throws SQLException;
    List<Dependencia> buscarPorIdItemDependente(int idItemDependente) throws SQLException;
    void atualizar(Dependencia dependencia) throws SQLException, Exception;
    void excluir(int id) throws SQLException;
    void excluirItem(int id) throws SQLException;
}