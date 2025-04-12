package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.ItemPedido;

import java.util.List;

public interface ItemPedidoDAO {
    void salvar(ItemPedido itemPedido) throws Exception;
    List<ItemPedido> buscarPorIdPedido(int idPedido) throws Exception;
    void excluir(int id) throws Exception;
}