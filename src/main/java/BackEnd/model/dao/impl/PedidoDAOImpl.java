package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ClienteDAO;
// import BackEnd.model.dao.interfaces.ItemDAO; // Descomente se usado
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.dao.interfaces.PedidoDAO;
import BackEnd.model.entity.*;
import BackEnd.util.AlertHelper; // Ainda usado para erros de conexão/rollback
import BackEnd.util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate; // Import necessário
import java.util.ArrayList;
import java.util.List;

public class PedidoDAOImpl implements PedidoDAO {

    private final ClienteDAO clienteDAO;
    private final ItemPedidoDAO itemPedidoDAO;
    // private final ItemDAO itemDAO;

    public PedidoDAOImpl() {
        this.clienteDAO = new ClienteDAOImpl();
        this.itemPedidoDAO = new ItemPedidoDAOImpl();
        // this.itemDAO = new ItemDAOImpl();
    }

    @Override
    public void salvar(Pedido pedido) throws Exception {
        String sql = "INSERT INTO pedidos (cliente_id, data_pedido, data_retorno, valor_total, status, observacoes) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        int maxRetries = 5;
        int retryDelay = 1000;

        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                conn = ConnectionFactory.getConnection();
                conn.setAutoCommit(false);

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, pedido.getCliente().getId());
                    stmt.setDate(2, java.sql.Date.valueOf(pedido.getDataPedido()));
                    if (pedido.getDataRetorno() != null) {
                        stmt.setDate(3, java.sql.Date.valueOf(pedido.getDataRetorno()));
                    } else {
                        stmt.setNull(3, Types.DATE);
                    }
                    stmt.setDouble(4, pedido.getValorTotal());
                    stmt.setString(5, pedido.getStatus().name());
                    stmt.setString(6, pedido.getObservacoes());
                    stmt.executeUpdate();

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            pedido.setId(generatedKeys.getInt(1));
                        } else {
                            throw new SQLException("Falha ao obter o ID do pedido, nenhum ID foi gerado.");
                        }
                    }
                }
                conn.commit();
                // ConnectionFactory.exportarBancoDeDados("BACKUP.2024"); // Opcional
                return;
            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                        // Lógica de retry (mantida como estava)
                        if (e.getCause() instanceof com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException &&
                                e.getMessage().contains("Lock wait timeout exceeded") && retryCount < maxRetries - 1) {
                            AlertHelper.showWarning("Timeout BD", "Tentando salvar novamente... (" + (retryCount + 2) + "/" + maxRetries + ")");
                            Thread.sleep(retryDelay);
                            retryDelay *= 2;
                            continue;
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        AlertHelper.showError("Erro Rollback", ex.getMessage());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        AlertHelper.showError("Erro", "Thread interrompida.");
                        throw e;
                    }
                }
                throw new Exception("Erro final ao salvar pedido: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        }
        throw new Exception("Não foi possível salvar o pedido após " + maxRetries + " tentativas.");
    }

    @Override
    public Pedido buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM pedidos WHERE id = ?";
        Pedido pedido = null;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    pedido = new Pedido();
                    pedido.setId(rs.getInt("id"));
                    Cliente cliente = clienteDAO.buscarPorId(rs.getString("cliente_id"));
                    pedido.setCliente(cliente);
                    pedido.setDataPedido(rs.getDate("data_pedido").toLocalDate());
                    Date dataRetornoSql = rs.getDate("data_retorno");
                    pedido.setDataRetorno(dataRetornoSql != null ? dataRetornoSql.toLocalDate() : null);
                    pedido.setValorTotal(rs.getDouble("valor_total"));
                    pedido.setStatus(StatusPedido.valueOf(rs.getString("status")));
                    pedido.setObservacoes(rs.getString("observacoes"));
                    // Buscar itens
                    List<ItemPedido> itensPedido = itemPedidoDAO.buscarPorIdPedido(id);
                    pedido.setItens(itensPedido);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao buscar pedido por ID: " + e.getMessage(), e);
        }
        return pedido;
    }

    @Override
    public List<Pedido> listar() throws Exception {
        String sql = "SELECT * FROM pedidos ORDER BY data_pedido DESC, id DESC";
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Pedido pedido = new Pedido();
                pedido.setId(rs.getInt("id"));
                Cliente cliente = clienteDAO.buscarPorId(rs.getString("cliente_id"));
                pedido.setCliente(cliente);
                pedido.setDataPedido(rs.getDate("data_pedido").toLocalDate());
                Date dataRetornoSql = rs.getDate("data_retorno");
                pedido.setDataRetorno(dataRetornoSql != null ? dataRetornoSql.toLocalDate() : null);
                pedido.setValorTotal(rs.getDouble("valor_total"));
                pedido.setStatus(StatusPedido.valueOf(rs.getString("status")));
                pedido.setObservacoes(rs.getString("observacoes"));
                List<ItemPedido> itensPedido = itemPedidoDAO.buscarPorIdPedido(pedido.getId());
                pedido.setItens(itensPedido);
                pedidos.add(pedido);
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao listar pedidos: " + e.getMessage(), e);
        }
        return pedidos;
    }

    @Override
    public void atualizarStatus(int id, StatusPedido status, Connection conn) throws Exception {
        String sql = "UPDATE pedidos SET status = ? WHERE id = ?";
        boolean localConnection = (conn == null);
        if (localConnection) {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(true);
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Erro ao atualizar status: Pedido ID " + id + " não encontrado.");
        } catch (SQLException e) {
            throw new Exception("Erro ao atualizar status do pedido: " + e.getMessage(), e);
        } finally {
            if (localConnection && conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @Override
    public void atualizarPedido(Pedido pedido) throws Exception {
        String sql = "UPDATE pedidos SET cliente_id = ?, data_pedido = ?, data_retorno = ?, valor_total = ?, status = ?, observacoes = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pedido.getCliente() != null ? pedido.getCliente().getId() : null);
            stmt.setDate(2, java.sql.Date.valueOf(pedido.getDataPedido()));
            if (pedido.getDataRetorno() != null) {
                stmt.setDate(3, java.sql.Date.valueOf(pedido.getDataRetorno()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            stmt.setDouble(4, pedido.getValorTotal());
            stmt.setString(5, pedido.getStatus().name());
            stmt.setString(6, pedido.getObservacoes());
            stmt.setInt(7, pedido.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Erro ao atualizar pedido: Pedido ID " + pedido.getId() + " não encontrado.");
        } catch (SQLException e) {
            throw new Exception("Erro ao atualizar pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Implementação do novo método para concluir o pedido no banco de dados.
     */
    @Override
    public void concluirPedido(int id, LocalDate dataRetorno) throws Exception {
        String sql = "UPDATE pedidos SET status = ?, data_retorno = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); // Autocommit é true por padrão
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, StatusPedido.CONCLUIDO.name()); // Status fixo

            // Validação da data (embora o Service deva garantir não ser nula)
            if (dataRetorno == null) {
                System.err.println("AVISO: Tentativa de concluir pedido ID " + id + " com data de retorno nula. Usando data atual.");
                // Ou lançar exceção: throw new IllegalArgumentException("Data de retorno é obrigatória para concluir.");
                dataRetorno = LocalDate.now(); // Fallback para data atual se nulo (decisão de design)
            }
            stmt.setDate(2, java.sql.Date.valueOf(dataRetorno)); // Data de retorno

            stmt.setInt(3, id); // ID do pedido

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                // Lança exceção se o pedido não foi encontrado para atualizar
                throw new SQLException("Nenhum pedido encontrado com ID " + id + " para concluir.");
            }
        } catch (SQLException e) {
            // Encapsula a SQLException em uma Exception mais genérica para a camada de serviço
            throw new Exception("Erro no banco de dados ao tentar concluir o pedido ID " + id + ": " + e.getMessage(), e);
        }
    }
}