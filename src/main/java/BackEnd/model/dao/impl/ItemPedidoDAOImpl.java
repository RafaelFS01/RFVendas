package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.ItemPedido;
import BackEnd.model.entity.Pedido;
import BackEnd.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemPedidoDAOImpl implements ItemPedidoDAO {

    private ItemDAO itemDAO;

    public ItemPedidoDAOImpl() {
        this.itemDAO = new ItemDAOImpl();
    }

    @Override
    public void salvar(ItemPedido itemPedido) throws Exception {
        Connection conn = null;
        conn = ConnectionFactory.getConnection();
        String sql = "INSERT INTO itens_pedido (pedido_id, item_id, quantidade, preco_venda) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemPedido.getPedido().getId());
            stmt.setInt(2, itemPedido.getItem().getId());
            stmt.setDouble(3, itemPedido.getQuantidade());
            stmt.setDouble(4, itemPedido.getPrecoVenda());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erro ao salvar item do pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ItemPedido> buscarPorIdPedido(int idPedido) throws Exception {
        String sql = "SELECT * FROM itens_pedido WHERE pedido_id = ?";
        List<ItemPedido> itensPedido = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemPedido itemPedido = new ItemPedido();
                    itemPedido.setId(rs.getInt("id"));
                    // Buscar Pedido (pode ser otimizado para evitar buscar o mesmo pedido várias vezes)
                    // itemPedido.setPedido(pedidoDAO.buscarPorId(rs.getInt("pedido_id")));
                    // Buscar Item
                    Item item = itemDAO.buscarItemPorId(rs.getInt("item_id"));
                    itemPedido.setItem(item);
                    itemPedido.setQuantidade(rs.getDouble("quantidade"));
                    itemPedido.setPrecoVenda(rs.getDouble("preco_venda"));

                    itensPedido.add(itemPedido);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao buscar itens do pedido por ID do pedido: " + e.getMessage(), e);
        }
        return itensPedido;
    }

    @Override
    public void excluir(int id) throws Exception {
        String sql = "DELETE FROM itens_pedido WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erro ao excluir item do pedido: " + e.getMessage(), e);
        }
    }
}