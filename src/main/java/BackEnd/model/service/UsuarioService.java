package BackEnd.model.service;

import BackEnd.model.dao.interfaces.UsuarioDAO;
import BackEnd.model.dao.impl.UsuarioDAOImpl;
import BackEnd.model.entity.Usuario;
import java.util.List;

public class UsuarioService {
    private final UsuarioDAO usuarioDAO;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }

    public boolean autenticar(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username é obrigatório");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }

        return usuarioDAO.autenticar(username, password);
    }

    public Usuario buscarPorUsername(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username é obrigatório");
        }

        Usuario usuario = usuarioDAO.buscarPorUsername(username);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        return usuario;
    }

    public List<Usuario> listarTodos() throws Exception {
        return usuarioDAO.listarTodos();
    }

    public void cadastrar(Usuario usuario) throws Exception {
        validarUsuario(usuario);
        if (usuarioDAO.buscarPorUsername(usuario.getUsername()) != null) {
            throw new IllegalArgumentException("Username já existe");
        }
        usuarioDAO.criar(usuario);
    }

    public void atualizar(Usuario usuario) throws Exception {
        validarUsuario(usuario);
        Usuario existente = usuarioDAO.buscarPorUsername(usuario.getUsername());
        if (existente == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        usuarioDAO.atualizar(usuario);
        usuarioDAO.atualizarSenha(usuario);
    }

    public void atualizarStatus(Usuario usuario) throws Exception {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não pode ser nulo");
        }
        if (usuario.getId() <= 0) {
            throw new IllegalArgumentException("ID do usuário inválido");
        }
        usuarioDAO.atualizarStatus(usuario);
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não pode ser nulo");
        }
        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username é obrigatório");
        }
        if (usuario.getNome() == null || usuario.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }
        if (usuario.getNivelAcesso() <= 0) {
            throw new IllegalArgumentException("Nível de acesso inválido");
        }
    }
}