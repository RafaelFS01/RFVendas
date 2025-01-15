package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Pedido;
import BackEnd.model.entity.StatusPedido;
import BackEnd.model.entity.TipoVenda;

import java.sql.Connection;
import java.util.List;

public interface PedidoDAO {
    void salvar(Pedido pedido) throws Exception;
    Pedido buscarPorId(int id) throws Exception;
    List<Pedido> listar() throws Exception;
    void atualizarStatus(int id, StatusPedido status, Connection conn) throws Exception;
    void atualizarTipoVenda(int pedidoId, TipoVenda tipoVenda) throws Exception;
    void atualizarPedido(Pedido pedido) throws Exception;
}