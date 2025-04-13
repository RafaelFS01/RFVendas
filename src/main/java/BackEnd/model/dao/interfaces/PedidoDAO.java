package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Pedido;
import BackEnd.model.entity.StatusPedido;
// REMOVIDO: import BackEnd.model.entity.TipoVenda; // Não mais necessário

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public interface PedidoDAO {

    /**
     * Salva um novo pedido no banco de dados.
     * A implementação deve lidar com o novo campo dataRetorno.
     * @param pedido O objeto Pedido a ser salvo.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    void salvar(Pedido pedido) throws Exception;

    /**
     * Busca um pedido pelo seu ID.
     * A implementação deve carregar o campo dataRetorno.
     * @param id O ID do pedido a ser buscado.
     * @return O objeto Pedido encontrado, ou null se não encontrado.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    Pedido buscarPorId(int id) throws Exception;

    /**
     * Lista todos os pedidos existentes no banco de dados.
     * A implementação deve carregar o campo dataRetorno para cada pedido.
     * @return Uma lista de todos os Pedidos.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    List<Pedido> listar() throws Exception;

    /**
     * Atualiza o status de um pedido específico.
     * @param id O ID do pedido a ter o status atualizado.
     * @param status O novo StatusPedido.
     * @param conn Uma conexão opcional para transações (pode ser null).
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    void atualizarStatus(int id, StatusPedido status, Connection conn) throws Exception;

    // REMOVIDO: Método para atualizar tipo de venda não é mais necessário
    // void atualizarTipoVenda(int pedidoId, TipoVenda tipoVenda) throws Exception;

    /**
     * Atualiza os dados de um pedido existente no banco de dados.
     * A implementação deve lidar com a atualização do campo dataRetorno.
     * @param pedido O objeto Pedido com os dados atualizados.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    void atualizarPedido(Pedido pedido) throws Exception;

    /**
     * Atualiza o status de um pedido para CONCLUIDO e define a data de retorno.
     * @param id O ID do pedido a ser concluído.
     * @param dataRetorno A data em que o pedido foi retornado/concluído.
     * @throws Exception Se ocorrer um erro no banco de dados ou o pedido não for encontrado.
     */
    void concluirPedido(int id, LocalDate dataRetorno) throws Exception; // NOVO MÉTODO
}