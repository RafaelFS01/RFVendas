package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.Categoria;
import BackEnd.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {

    @Override
    public void salvarItem(Item item) throws Exception {
        String sql = "INSERT INTO itens (id, nome, descricao, preco_venda, preco_custo, unidade_medida, " +
                "quantidade_estoque, quantidade_minima, quantidade_atual, " +
                "categoria_id, tipo_produto) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Inicia transação

            stmt.setInt(1, item.getId());
            stmt.setString(2, item.getNome());
            stmt.setString(3, item.getDescricao());
            stmt.setDouble(4, item.getPrecoVenda());
            stmt.setDouble(5, item.getPrecoCusto());
            stmt.setString(6, item.getUnidadeMedida());
            stmt.setDouble(7, item.getQuantidadeEstoque());
            stmt.setDouble(8, item.getQuantidadeMinima());
            stmt.setDouble(9, item.getQuantidadeAtual());
            stmt.setInt(10, item.getCategoria().getId());
            stmt.setString(11, item.getTipoProduto()); // Define o tipo_produto

            stmt.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            throw new Exception("Erro ao salvar item: " + e.getMessage());
        }
    }

    @Override
    public void atualizar(Item item) throws Exception {
        String sql = "UPDATE itens SET nome = ?, descricao = ?, preco_venda = ?, preco_custo = ?, unidade_medida = ?, " +
                "quantidade_estoque = ?, quantidade_minima = ?, quantidade_atual = ?, categoria_id = ?, tipo_produto = ? " +
                "WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getNome());
            stmt.setString(2, item.getDescricao());
            stmt.setDouble(3, item.getPrecoVenda());
            stmt.setDouble(4, item.getPrecoCusto());
            stmt.setString(5, item.getUnidadeMedida());
            stmt.setDouble(6, item.getQuantidadeEstoque());
            stmt.setDouble(7, item.getQuantidadeMinima());
            stmt.setDouble(8, item.getQuantidadeAtual());
            stmt.setInt(9, item.getCategoria().getId());
            stmt.setString(10, item.getTipoProduto()); // Atualiza o tipo_produto
            stmt.setInt(11, item.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new Exception("Erro ao atualizar item: " + e.getMessage());
        }
    }

    @Override
    public boolean buscarItemPorNome(String nome) throws Exception {
        String sql = "SELECT 1 FROM itens WHERE nome = ? AND tipo_produto = 'ITEM'";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new Exception("Erro ao buscar item por nome: " + e.getMessage());
        }
    }

    @Override
    public Item buscarItemPorId(int id) throws Exception {
        String sql = "SELECT i.*, c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSetParaItem(rs);
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new Exception("Erro ao buscar item por ID: " + e.getMessage());
        }
    }

    @Override
    public List<Item> listarItens() throws Exception {
        List<Item> itens = new ArrayList<>();
        String sql = "SELECT i.*, c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                itens.add(mapearResultSetParaItem(rs));
            }
            return itens;

        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens: " + e.getMessage());
        }
    }

    @Override
    public List<Item> listarItensPorCategoria(int idCategoria) throws Exception {
        List<Item> itens = new ArrayList<>();
        String sql = "SELECT i.*, c.nome AS categoria_nome, c.descricao AS categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.categoria_id = ? AND i.tipo_produto = 'ITEM'";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCategoria);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(mapearResultSetParaItem(rs));
                }
            }

        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens por categoria: " + e.getMessage());
        }
        return itens;
    }

    @Override
    public List<Item> listarItensAbaixoDoMinimo() throws Exception {
        List<Item> itens = new ArrayList<>();
        String sql = "SELECT i.*, c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.quantidade_atual < i.quantidade_minima AND i.tipo_produto = 'ITEM' " +
                "ORDER BY i.nome";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                itens.add(mapearResultSetParaItem(rs));
            }
            return itens;

        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens abaixo do mínimo: " + e.getMessage());
        }
    }

    @Override
    public void deletar(int id) throws Exception {
        String sql = "DELETE FROM itens WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new Exception("Erro ao deletar item: " + e.getMessage());
        }
    }

    private Item mapearResultSetParaItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setNome(rs.getString("nome"));
        item.setDescricao(rs.getString("descricao"));
        item.setPrecoVenda(rs.getDouble("preco_venda"));
        item.setPrecoCusto(rs.getDouble("preco_custo"));
        item.setUnidadeMedida(rs.getString("unidade_medida"));
        item.setQuantidadeEstoque(rs.getDouble("quantidade_estoque"));
        item.setQuantidadeMinima(rs.getDouble("quantidade_minima"));
        item.setQuantidadeAtual(rs.getDouble("quantidade_atual"));
        item.setTipoProduto(rs.getString("tipo_produto"));

        Categoria categoria = new Categoria();
        categoria.setId(rs.getInt("categoria_id"));
        categoria.setNome(rs.getString("categoria_nome"));
        categoria.setDescricao(rs.getString("categoria_descricao"));
        item.setCategoria(categoria);

        return item;
    }
}