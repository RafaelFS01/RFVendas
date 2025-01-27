package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Dependencia;

import java.sql.SQLException;
import java.util.List;

public interface DependenciaDAO {
    void salvarDependencia(Dependencia dependencia) throws SQLException;
    Dependencia buscarPorId(int id) throws SQLException;
    List<Dependencia> buscarPorIdProdutoDependente(int idProdutoDependente) throws SQLException;
    void atualizar(Dependencia dependencia) throws SQLException;
    void excluir(int id) throws SQLException;
    void excluirProduto(int idProduto) throws SQLException;
}