package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Usuario;
import java.util.List;

public interface UsuarioDAO {
    void criar(Usuario usuario) throws Exception;
    Usuario buscarPorId(int id) throws Exception;
    Usuario buscarPorUsername(String username) throws Exception;
    List<Usuario> listarTodos() throws Exception;
    void atualizar(Usuario usuario) throws Exception;
    void atualizarSenha(Usuario usuario) throws Exception;
    void atualizarStatus(Usuario usuario) throws Exception;
    boolean autenticar(String username, String password) throws Exception;
}