package BackEnd.model.service;

import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.impl.ItemPedidoDAOImpl;
import BackEnd.model.dao.impl.PedidoDAOImpl;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.dao.interfaces.PedidoDAO;
import BackEnd.model.entity.*;

import java.sql.SQLException;
import java.time.LocalDate; // Import necessário
import java.util.List;

public class PedidoService {

    private final PedidoDAO pedidoDAO;
    private final ItemPedidoDAO itemPedidoDAO;
    private final ItemDAO itemDAO;

    public PedidoService() {
        this.pedidoDAO = new PedidoDAOImpl();
        this.itemPedidoDAO = new ItemPedidoDAOImpl();
        this.itemDAO = new ItemDAOImpl();
    }

    /**
     * Salva um novo pedido após validação.
     * @param pedido O Pedido a ser salvo (status já deve estar definido pelo Controller).
     * @throws Exception Se a validação falhar ou ocorrer erro no DAO.
     */
    public void salvarPedido(Pedido pedido) throws Exception {
        validarPedido(pedido);
        pedidoDAO.salvar(pedido);
    }

    /**
     * Salva os itens associados a um pedido e atualiza a quantidade ATUAL dos itens.
     * @param pedido O Pedido (já salvo e com ID) contendo os itens.
     * @throws Exception Se ocorrer erro ao salvar itens ou atualizar estoque.
     */
    public void salvarItens(Pedido pedido) throws Exception {
        if (pedido == null || pedido.getId() == 0 || pedido.getItens() == null) {
            throw new Exception("Pedido inválido ou sem itens para salvar.");
        }

        for (ItemPedido itemPedido : pedido.getItens()) {
            itemPedido.setPedido(pedido);
            itemPedidoDAO.salvar(itemPedido);

            Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
            if (item != null) {
                double novaQuantidadeAtual = item.getQuantidadeAtual() - itemPedido.getQuantidade();
                item.setQuantidadeAtual(novaQuantidadeAtual);
                itemDAO.atualizar(item);
            } else {
                System.err.println("AVISO: Item com ID " + itemPedido.getItem().getId() + " não encontrado para atualização de quantidade no pedido ID " + pedido.getId());
                // Considerar lançar exceção se for crítico
            }
        }
    }

    /**
     * Busca um pedido pelo ID.
     * @param id O ID do pedido.
     * @return O Pedido encontrado ou null.
     * @throws Exception Se ocorrer erro no DAO.
     */
    public Pedido buscarPedidoPorId(int id) throws Exception {
        return pedidoDAO.buscarPorId(id);
    }

    /**
     * Lista todos os pedidos.
     * @return Lista de Pedidos.
     * @throws Exception Se ocorrer erro no DAO.
     */
    public List<Pedido> listarPedidos() throws Exception {
        return pedidoDAO.listar();
    }

    /**
     * Cancela um pedido, alterando seu status para CANCELADO.
     * @param id O ID do pedido a ser cancelado.
     * @throws Exception Se o pedido não for encontrado, já estiver cancelado/concluído, ou ocorrer erro no DAO.
     */
    public void cancelarPedido(int id) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(id);
        if (pedido == null) {
            throw new Exception("Pedido com ID " + id + " não encontrado.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new Exception("Pedido já está cancelado.");
        }
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            throw new Exception("Não é possível cancelar um pedido já concluído.");
        }
        // Lembrete: Estoque não é devolvido automaticamente ao cancelar.
        pedidoDAO.atualizarStatus(id, StatusPedido.CANCELADO, null);
    }

    /**
     * Conclui um pedido, alterando seu status para CONCLUIDO e definindo
     * a data de retorno para a data atual.
     * @param pedidoId O ID do pedido a ser concluído.
     * @throws Exception Se o pedido não for encontrado, não estiver 'Em Andamento',
     *                   ou ocorrer um erro no DAO ao tentar concluir.
     */
    public void concluirPedido(int pedidoId) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(pedidoId); // Busca o pedido para validação

        // Validações de Negócio antes de chamar o DAO
        if (pedido == null) {
            throw new Exception("Pedido com ID " + pedidoId + " não encontrado.");
        }
        if (pedido.getStatus() != StatusPedido.EM_ANDAMENTO) {
            // Decide como tratar: erro ou silenciosamente ignorar se já concluído?
            // Lançar erro é mais explícito sobre a regra.
            throw new Exception("Apenas pedidos com status 'Em Andamento' podem ser concluídos. Status atual: " + pedido.getStatus());
        }

        // Define a data de retorno como a data atual
        LocalDate dataRetornoAtual = LocalDate.now();

        // Dentro de PedidoService.concluirPedido, ANTES de chamar pedidoDAO.concluirPedido:

         for (ItemPedido itemPedido : pedido.getItens()) {
             Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
             if (item != null) {
                 item.setQuantidadeAtual(item.getQuantidadeAtual() + itemPedido.getQuantidade()); // SOMA DE VOLTA
                 itemDAO.atualizar(item);
             }
         }


        // Chama o novo método do DAO para persistir a alteração de status e data
        // O DAO agora lida com a atualização atômica desses dois campos.
        pedidoDAO.concluirPedido(pedidoId, dataRetornoAtual);

        // Log ou notificação (opcional)
        System.out.println("Pedido ID " + pedidoId + " concluído com data de retorno " + dataRetornoAtual);
    }


    /**
     * Valida os dados essenciais de um objeto Pedido.
     * @param pedido O Pedido a ser validado.
     * @throws Exception Se alguma validação falhar.
     */
    public void validarPedido(Pedido pedido) throws Exception {
        if (pedido == null) throw new Exception("Pedido não pode ser nulo.");
        if (pedido.getCliente() == null) throw new Exception("Cliente não selecionado.");
        if (pedido.getDataPedido() == null) throw new Exception("Data do pedido não selecionada.");
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) throw new Exception("Nenhum item adicionado ao pedido.");

        for (ItemPedido itemPedido : pedido.getItens()) {
            if (itemPedido.getItem() == null) throw new Exception("Pedido contém um item inválido (nulo).");
            if (itemPedido.getQuantidade() <= 0) throw new Exception("Quantidade do item '" + itemPedido.getItem().getNome() + "' deve ser > 0.");
            if (itemPedido.getPrecoVenda() <= 0) throw new Exception("Preço do item '" + itemPedido.getItem().getNome() + "' deve ser > 0.");
        }
        if (pedido.getValorTotal() < 0) throw new Exception("Valor total do pedido não pode ser negativo.");
    }

    /**
     * Calcula o valor total de uma lista de itens de pedido.
     * @param itensPedido Lista de ItemPedido.
     * @return O valor total calculado.
     */
    public double calcularValorTotal(List<ItemPedido> itensPedido) {
        if (itensPedido == null) return 0.0;
        return itensPedido.stream()
                .mapToDouble(itemPedido -> itemPedido.getQuantidade() * itemPedido.getPrecoVenda())
                .sum();
    }

    /**
     * Atualiza os dados principais de um pedido existente.
     * @param pedido O Pedido com os dados atualizados.
     * @throws Exception Se a validação falhar ou ocorrer erro no DAO.
     */
    public void atualizarPedido(Pedido pedido) throws Exception {
        validarPedido(pedido);
        pedidoDAO.atualizarPedido(pedido);
    }

    /**
     * Atualiza os itens de um pedido existente.
     * Remove os itens antigos e salva os novos, atualizando a quantidade ATUAL dos itens.
     * @param pedido O Pedido (com ID) contendo a nova lista de itens.
     * @throws Exception Se ocorrer erro ao excluir/salvar itens ou atualizar estoque.
     */
    public void atualizarItens(Pedido pedido) throws Exception {
        if (pedido == null || pedido.getId() == 0) {
            throw new Exception("Pedido inválido para atualização de itens.");
        }

        List<ItemPedido> itensAntigos = itemPedidoDAO.buscarPorIdPedido(pedido.getId());
        for (ItemPedido itemAntigo : itensAntigos) {
            // Lembrete: Nenhuma devolução de estoque aqui.
            itemPedidoDAO.excluir(itemAntigo.getId());
        }

        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            salvarItens(pedido); // Reutiliza a lógica que salva e deduz estoque atual
        } else {
            System.out.println("Info: Nenhum novo item para adicionar ao pedido ID " + pedido.getId() + " após atualização.");
        }
    }
}