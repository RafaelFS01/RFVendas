package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.UsuarioDAO;
import BackEnd.model.entity.Usuario;
import BackEnd.util.ConnectionFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public void criar(Usuario usuario) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "INSERT INTO usuarios (username, password, nome, email, nivel_acesso, ativo) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, usuario.getPassword());
            stmt.setString(3, usuario.getNome());
            stmt.setString(4, usuario.getEmail());
            stmt.setInt(5, usuario.getNivelAcesso());
            stmt.setBoolean(6, usuario.isAtivo());
            stmt.executeUpdate();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    @Override
    public Usuario buscarPorId(int id) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM usuarios WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearResultSet(rs);
            }
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }

    @Override
    public Usuario buscarPorUsername(String username) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM usuarios WHERE username = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearResultSet(rs);
            }
            return null;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }

    @Override
    public List<Usuario> listarTodos() throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Usuario> usuarios = new ArrayList<>();
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM usuarios ORDER BY nome";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                usuarios.add(mapearResultSet(rs));
            }
            return usuarios;
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }

    @Override
    public void atualizar(Usuario usuario) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "UPDATE usuarios SET nome = ?, email = ?, nivel_acesso = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setInt(3, usuario.getNivelAcesso());
            stmt.setInt(4, usuario.getId());
            stmt.executeUpdate();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    @Override
    public void atualizarSenha(Usuario usuario) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "UPDATE usuarios SET password = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuario.getPassword());
            stmt.setInt(2, usuario.getId());
            stmt.executeUpdate();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    @Override
    public void atualizarStatus(Usuario usuario) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "UPDATE usuarios SET ativo = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, usuario.isAtivo());
            stmt.setInt(2, usuario.getId());
            stmt.executeUpdate();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt);
        }
    }

    @Override
    public boolean autenticar(String username, String password) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionFactory.getConnection();
            String sql = "SELECT * FROM usuarios WHERE username = ? AND password = ? AND ativo = true";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            ConnectionFactory.closeConnection(conn, stmt, rs);
        }
    }

    private Usuario mapearResultSet(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setUsername(rs.getString("username"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setNivelAcesso(rs.getInt("nivel_acesso"));
        usuario.setAtivo(rs.getBoolean("ativo"));
        return usuario;
    }
}