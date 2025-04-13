package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.CategoriaDAO;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.Categoria;
import BackEnd.util.ConnectionFactory;

import java.sql.*; // Import Types para setNull
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {

    private final CategoriaDAO categoriaDAO;

    public ItemDAOImpl() {
        // Assumindo que CategoriaDAOImpl existe e está correto
        this.categoriaDAO = new CategoriaDAOImpl();
    }

    @Override
    public void salvarItem(Item item) throws Exception {
        // SQL inclui a nova coluna imagem_path
        String sql = "INSERT INTO itens (id, nome, descricao, preco_venda, preco_custo, unidade_medida, " +
                "quantidade_estoque, quantidade_minima, quantidade_atual, " +
                "categoria_id, imagem_path) " + // Adicionada nova coluna
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Adicionado placeholder
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Transações são geralmente melhor gerenciadas no Service, mas mantendo como no original por enquanto
            conn.setAutoCommit(false);

            stmt.setInt(1, item.getId());
            stmt.setString(2, item.getNome());
            stmt.setString(3, item.getDescricao());
            // Usar setBigDecimal para DECIMAL seria mais preciso, mas mantendo Double
            stmt.setDouble(4, item.getPrecoVenda() != null ? item.getPrecoVenda() : 0.0);
            stmt.setDouble(5, item.getPrecoCusto() != null ? item.getPrecoCusto() : 0.0);
            stmt.setString(6, item.getUnidadeMedida());
            stmt.setDouble(7, item.getQuantidadeEstoque() != null ? item.getQuantidadeEstoque() : 0.0);
            stmt.setDouble(8, item.getQuantidadeMinima() != null ? item.getQuantidadeMinima() : 0.0);
            stmt.setDouble(9, item.getQuantidadeAtual() != null ? item.getQuantidadeAtual() : 0.0);

            // Trata Categoria nula (embora o Service deva validar antes)
            if (item.getCategoria() != null && item.getCategoria().getId() > 0) {
                stmt.setInt(10, item.getCategoria().getId());
            } else {
                stmt.setNull(10, Types.INTEGER); // Ou lançar erro se categoria for obrigatória
                // throw new SQLException("Categoria inválida ou não definida para o item.");
            }

            // Define o valor para imagem_path (índice 11)
            if (item.getImagemPath() != null && !item.getImagemPath().trim().isEmpty()) {
                stmt.setString(11, item.getImagemPath().trim());
            } else {
                stmt.setNull(11, Types.VARCHAR); // Define como NULL se não houver caminho
            }

            stmt.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            // Idealmente, o rollback deveria ser tentado aqui se conn não for nulo e autoCommit era false
            throw new Exception("Erro ao salvar item no banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Item item) throws Exception {
        // SQL inclui imagem_path no SET
        String sql = "UPDATE itens SET nome = ?, descricao = ?, preco_venda = ?, preco_custo = ?, " +
                "unidade_medida = ?, quantidade_estoque = ?, quantidade_minima = ?, " +
                "quantidade_atual = ?, categoria_id = ?, imagem_path = ? " + // Adicionado imagem_path
                "WHERE id = ?"; // Placeholder para ID no final
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getNome());
            stmt.setString(2, item.getDescricao());
            stmt.setDouble(3, item.getPrecoVenda() != null ? item.getPrecoVenda() : 0.0);
            stmt.setDouble(4, item.getPrecoCusto() != null ? item.getPrecoCusto() : 0.0);
            stmt.setString(5, item.getUnidadeMedida());
            stmt.setDouble(6, item.getQuantidadeEstoque() != null ? item.getQuantidadeEstoque() : 0.0);
            stmt.setDouble(7, item.getQuantidadeMinima() != null ? item.getQuantidadeMinima() : 0.0);
            stmt.setDouble(8, item.getQuantidadeAtual() != null ? item.getQuantidadeAtual() : 0.0);

            if (item.getCategoria() != null && item.getCategoria().getId() > 0) {
                stmt.setInt(9, item.getCategoria().getId());
            } else {
                stmt.setNull(9, Types.INTEGER);
                // throw new SQLException("Categoria inválida ou não definida para o item.");
            }

            // Define o valor para imagem_path (índice 10)
            if (item.getImagemPath() != null && !item.getImagemPath().trim().isEmpty()) {
                stmt.setString(10, item.getImagemPath().trim());
            } else {
                stmt.setNull(10, Types.VARCHAR);
            }

            // Define o ID para o WHERE (índice 11)
            stmt.setInt(11, item.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                // Considerar lançar uma exceção específica se nenhum registro foi atualizado
                System.err.println("Aviso: Nenhuma linha atualizada para o item ID " + item.getId() + ". O item pode não existir.");
                // throw new SQLException("Item com ID " + item.getId() + " não encontrado para atualização.");
            }

        } catch (SQLException e) {
            throw new Exception("Erro ao atualizar item no banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean buscarItemPorNome(String nome) throws Exception {
        // Nenhuma alteração necessária aqui
        String sql = "SELECT 1 FROM itens WHERE nome = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao buscar item por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public Item buscarItemPorId(int id) throws Exception {
        // Query ajustada para garantir que todos os campos necessários estão sendo pegos
        // Usar i.* é geralmente suficiente, mas listar explicitamente pode ser mais seguro.
        String sql = "SELECT i.id, i.nome, i.descricao, i.preco_venda, i.preco_custo, i.unidade_medida, " +
                "i.quantidade_estoque, i.quantidade_minima, i.quantidade_atual, i.categoria_id, i.imagem_path, " + // Inclui imagem_path
                "c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSetParaItem(rs); // Mapeamento inclui imagem_path
                } else {
                    return null; // Item não encontrado
                }
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao buscar item por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Item> listarItens() throws Exception {
        List<Item> itens = new ArrayList<>();
        // Query ajustada para garantir todos os campos
        String sql = "SELECT i.id, i.nome, i.descricao, i.preco_venda, i.preco_custo, i.unidade_medida, " +
                "i.quantidade_estoque, i.quantidade_minima, i.quantidade_atual, i.categoria_id, i.imagem_path, " + // Inclui imagem_path
                "c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "ORDER BY i.nome"; // Adicionado ORDER BY
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                itens.add(mapearResultSetParaItem(rs)); // Mapeamento inclui imagem_path
            }
            return itens;
        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Item> listarItensPorCategoria(int idCategoria) throws Exception {
        List<Item> itens = new ArrayList<>();
        String sql = "SELECT i.id, i.nome, i.descricao, i.preco_venda, i.preco_custo, i.unidade_medida, " +
                "i.quantidade_estoque, i.quantidade_minima, i.quantidade_atual, i.categoria_id, i.imagem_path, " + // Inclui imagem_path
                "c.nome AS categoria_nome, c.descricao AS categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.categoria_id = ? ORDER BY i.nome";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCategoria);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(mapearResultSetParaItem(rs)); // Mapeamento inclui imagem_path
                }
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens por categoria: " + e.getMessage(), e);
        }
        return itens;
    }


    @Override
    public List<Item> listarItensAbaixoDoMinimo() throws Exception {
        List<Item> itens = new ArrayList<>();
        String sql = "SELECT i.id, i.nome, i.descricao, i.preco_venda, i.preco_custo, i.unidade_medida, " +
                "i.quantidade_estoque, i.quantidade_minima, i.quantidade_atual, i.categoria_id, i.imagem_path, " + // Inclui imagem_path
                "c.nome as categoria_nome, c.descricao as categoria_descricao " +
                "FROM itens i " +
                "LEFT JOIN categorias c ON i.categoria_id = c.id " +
                "WHERE i.quantidade_atual < i.quantidade_minima " +
                "ORDER BY i.nome";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                itens.add(mapearResultSetParaItem(rs)); // Mapeamento inclui imagem_path
            }
            return itens;
        } catch (SQLException e) {
            throw new Exception("Erro ao listar itens abaixo do mínimo: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletar(int id) throws Exception {
        // Nenhuma alteração necessária aqui. O Service busca o path antes.
        String sql = "DELETE FROM itens WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("Aviso: Nenhuma linha deletada para o item ID " + id + ". O item pode já ter sido removido.");
                // throw new SQLException("Item com ID " + id + " não encontrado para exclusão.");
            }
        } catch (SQLException e) {
            // Capturar especificamente FK constraint violation se itens estiverem em pedidos?
            throw new Exception("Erro ao deletar item do banco de dados: " + e.getMessage(), e);
        }
    }

    /**
     * Mapeia uma linha do ResultSet para um objeto Item, incluindo a categoria e o caminho da imagem.
     * @param rs O ResultSet posicionado na linha correta.
     * @return O objeto Item populado.
     * @throws SQLException Se ocorrer erro ao acessar o ResultSet.
     */
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

        // Mapeia o caminho da imagem
        item.setImagemPath(rs.getString("imagem_path")); // Obtém o valor da nova coluna

        // Mapeia a Categoria (se existir)
        int categoriaId = rs.getInt("categoria_id");
        if (!rs.wasNull()) { // Verifica se categoria_id não era NULL
            Categoria categoria = new Categoria();
            categoria.setId(categoriaId);
            // Pega nome/descrição da categoria do JOIN para evitar consulta extra
            categoria.setNome(rs.getString("categoria_nome"));
            categoria.setDescricao(rs.getString("categoria_descricao"));
            item.setCategoria(categoria);
        } else {
            item.setCategoria(null); // Define como null se não houver categoria associada
        }

        return item;
    }
}