// Em src/main/java/seuprojeto/model/dao/impl/CategoriaDAOImpl.java

package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.CategoriaDAO;
import BackEnd.model.entity.Categoria;
import BackEnd.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOImpl implements CategoriaDAO {

    @Override
    public Categoria salvarCategoria(Categoria categoria) throws Exception {
        if (categoria == null || categoria.getNome() == null) {
            throw new IllegalArgumentException("Categoria inválida.");
        }

        String sqlBuscar = "SELECT id FROM categorias WHERE nome = ?";
        String sqlInserir = "INSERT INTO categorias (nome, descricao) VALUES (?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmtBuscar = conn.prepareStatement(sqlBuscar);
             PreparedStatement stmtInserir = conn.prepareStatement(sqlInserir, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // 1. Verificar se a categoria já existe
            stmtBuscar.setString(1, categoria.getNome());
            try (ResultSet rs = stmtBuscar.executeQuery()) {
                if (rs.next()) {
                    // Categoria já existe, retornar a categoria existente
                    categoria.setId(rs.getInt("id"));
                    return categoria;
                }
            }

            // 2. Categoria não existe, inserir e retornar a nova categoria
            stmtInserir.setString(1, categoria.getNome());
            stmtInserir.setString(2, categoria.getDescricao());
            stmtInserir.executeUpdate();

            // Obter o ID gerado
            try (ResultSet generatedKeys = stmtInserir.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    categoria.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Falha ao inserir categoria, nenhum ID obtido.");
                }
            }
            return categoria;

        } catch (SQLException e) {
            throw new Exception("Erro ao salvar categoria: " + e.getMessage());
        }
    }

    @Override
    public Categoria buscarCategoriaPorNome(String nome) throws Exception {
        String sql = "SELECT * FROM categorias WHERE nome = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSetParaCategoria(rs);
                } else {
                    return null; // Categoria não encontrada
                }
            }

        } catch (SQLException e) {
            throw new Exception("Erro ao buscar categoria por nome: " + e.getMessage());
        }
    }

    public List<Categoria> listarCategorias() throws Exception {
        List<Categoria> categorias = new ArrayList<>();
        String sql = "SELECT * FROM categorias";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categorias.add(mapearResultSetParaCategoria(rs));
            }
            return categorias;

        } catch (SQLException e) {
            throw new Exception("Erro ao listar categorias: " + e.getMessage());
        }
    }

    private Categoria mapearResultSetParaCategoria(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));
        categoria.setDescricao(rs.getString("descricao"));
        return categoria;
    }
}