package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.GrupoDAO;
import BackEnd.model.entity.Grupo;
import BackEnd.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GrupoDAOImpl implements GrupoDAO {

    @Override
    public void criar(Grupo grupo) throws Exception {
        String sql = "INSERT INTO grupos (nome) VALUES (?)";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, grupo.getNome());
            stmt.executeUpdate();

            // Obtém o ID gerado automaticamente
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    grupo.setId(generatedKeys.getInt(1));
                } else {
                    throw new Exception("Falha ao obter o ID gerado para o grupo.");
                }
            }
        }
    }

    @Override
    public Grupo buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM grupos WHERE id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearGrupo(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Grupo buscarPorNome(String nome) throws Exception {
        String sql = "SELECT * FROM grupos WHERE nome = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearGrupo(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Grupo> listarTodos() throws Exception {
        String sql = "SELECT * FROM grupos ORDER BY nome";
        List<Grupo> grupos = new ArrayList<>();
        try (Connection conexao = ConnectionFactory.getConnection();
             Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                grupos.add(mapearGrupo(rs));
            }
        }
        return grupos;
    }

    @Override
    public void atualizar(Grupo grupo) throws Exception {
        String sql = "UPDATE grupos SET nome = ? WHERE id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, grupo.getNome());
            stmt.setInt(2, grupo.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deletar(int id) throws Exception {
        String sql = "DELETE FROM grupos WHERE id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean existePorId(int id) throws Exception {
        String sql = "SELECT 1 FROM grupos WHERE id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean existePorNome(String nome) throws Exception {
        String sql = "SELECT 1 FROM grupos WHERE nome = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Grupo mapearGrupo(ResultSet rs) throws Exception {
        Grupo grupo = new Grupo();
        grupo.setId(rs.getInt("id"));
        grupo.setNome(rs.getString("nome"));
        return grupo;
    }
}