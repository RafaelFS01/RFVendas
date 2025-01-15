package BackEnd.model.service;

import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.dao.interfaces.PedidoDAO;
import BackEnd.model.dao.impl.ItemPedidoDAOImpl;
import BackEnd.model.dao.impl.PedidoDAOImpl;
import BackEnd.model.entity.*;
import BackEnd.util.AlertHelper;
import BackEnd.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PedidoService {

    private PedidoDAO pedidoDAO;
    private ItemPedidoDAO itemPedidoDAO;
    private ItemDAO itemDAO;

    public PedidoService() {
        this.pedidoDAO = new PedidoDAOImpl();
        this.itemPedidoDAO = new ItemPedidoDAOImpl();
        this.itemDAO = new ItemDAOImpl(); // Adicione esta linha para instanciar o ItemDAO
    }

    public void salvarPedido(Pedido pedido) throws Exception {
        validarPedido(pedido);
        pedidoDAO.salvar(pedido);
    }

    public void salvarItens(Pedido pedido) throws Exception {
        // Salvar itens do pedido e atualizar itens
        for (ItemPedido itemPedido : pedido.getItens()) {
            itemPedido.setPedido(pedido);
            itemPedidoDAO.salvar(itemPedido);

            Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
            if (item != null) {
                if (pedido.getTipoVenda() == TipoVenda.NOTA_FISCAL || pedido.getTipoVenda() == TipoVenda.VENDA_NORMAL) {
                    item.setQuantidadeEstoque(item.getQuantidadeEstoque() - itemPedido.getQuantidade());
                }
                if (pedido.getTipoVenda() == TipoVenda.NOTA_FISCAL || pedido.getTipoVenda() == TipoVenda.VENDA_NORMAL || pedido.getTipoVenda() == TipoVenda.PEDIDO) {
                    item.setQuantidadeAtual(item.getQuantidadeAtual() - itemPedido.getQuantidade());
                }
                itemDAO.atualizar(item);
            }
        }
    }

    public Pedido buscarPedidoPorId(int id) throws Exception {
        return pedidoDAO.buscarPorId(id);
    }

    public List<Pedido> listarPedidos() throws Exception {
        return pedidoDAO.listar();
    }

    public void cancelarPedido(int id) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(id);
        if (pedido == null){
            throw new Exception("Pedido com a ID: " + id + " não foi encontrado");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new Exception("Pedido já está cancelado.");
        }
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            throw new Exception("Não é possível cancelar um pedido já concluído.");
        }

        pedidoDAO.atualizarStatus(id, StatusPedido.CANCELADO, null);
    }

    public void atualizarTipoVenda(int pedidoId, TipoVenda tipoVenda) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(pedidoId);
        if (pedido == null) {
            throw new Exception("Pedido não encontrado.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new Exception("Não é possível alterar o tipo de venda de um pedido cancelado.");
        }

        if (tipoVenda == TipoVenda.VENDA_NORMAL || tipoVenda == TipoVenda.NOTA_FISCAL) {
            if (pedido.getTipoVenda() == TipoVenda.PEDIDO) {
                for (ItemPedido itemPedido : pedido.getItens()) {
                    Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
                    if (item != null) {
                        if (item.getQuantidadeEstoque() >= itemPedido.getQuantidade()) {
                            item.setQuantidadeEstoque(item.getQuantidadeEstoque() - itemPedido.getQuantidade());
                        } else {
                            throw new Exception("Estoque insuficiente para o item: " + item.getNome());
                        }
                        itemDAO.atualizar(item);
                    }
                }
            }
        }
        pedidoDAO.atualizarTipoVenda(pedidoId, tipoVenda);
        if (tipoVenda == TipoVenda.VENDA_NORMAL || tipoVenda == TipoVenda.NOTA_FISCAL) {
            pedidoDAO.atualizarStatus(pedidoId, StatusPedido.CONCLUIDO, null);
        }
    }

    public void validarPedido(Pedido pedido) throws Exception {
        if (pedido.getCliente() == null) {
            throw new Exception("Cliente não selecionado.");
        }
        if (pedido.getTipoVenda() == null) {
            throw new Exception("Tipo de venda não selecionado.");
        }
        if (pedido.getDataPedido() == null) {
            throw new Exception("Data do pedido não selecionada.");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new Exception("Nenhum item adicionado ao pedido.");
        }
        for (ItemPedido itemPedido : pedido.getItens()) {
            if (itemPedido.getQuantidade() <= 0) {
                throw new Exception("Quantidade do item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
            }
            if (itemPedido.getPrecoVenda() <= 0){
                throw new Exception("O preço do item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
            }
            // Adicione outras validações necessárias, como verificar se há estoque suficiente, etc.
        }
    }

    public double calcularValorTotal(List<ItemPedido> itensPedido) {
        return itensPedido.stream()
                .mapToDouble(itemPedido -> itemPedido.getQuantidade() * itemPedido.getPrecoVenda())
                .sum();
    }

    public void atualizarPedido(Pedido pedido) throws Exception {
        validarPedido(pedido);
        pedidoDAO.atualizarPedido(pedido);
    }

    public void atualizarItens(Pedido pedido) throws Exception {
        // 1. Excluir os itens antigos do pedido
        List<ItemPedido> itensAntigos = itemPedidoDAO.buscarPorIdPedido(pedido.getId());
        for (ItemPedido itemAntigo : itensAntigos) {
            itemPedidoDAO.excluir(itemAntigo.getId()); // Você precisará criar este método em ItemPedidoDAO
        }

        // 2. Salvar os novos itens do pedido e atualizar itens
        for (ItemPedido itemPedido : pedido.getItens()) {
            itemPedido.setPedido(pedido);
            itemPedidoDAO.salvar(itemPedido);

            Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
            if (item != null) {
                // Atualiza as quantidades com base no tipo de venda (se necessário)
                if (pedido.getTipoVenda() == TipoVenda.NOTA_FISCAL || pedido.getTipoVenda() == TipoVenda.VENDA_NORMAL) {
                    item.setQuantidadeEstoque(item.getQuantidadeEstoque() - itemPedido.getQuantidade());
                }
                if (pedido.getTipoVenda() == TipoVenda.NOTA_FISCAL || pedido.getTipoVenda() == TipoVenda.VENDA_NORMAL || pedido.getTipoVenda() == TipoVenda.PEDIDO) {
                    item.setQuantidadeAtual(item.getQuantidadeAtual() - itemPedido.getQuantidade());
                }
                itemDAO.atualizar(item);
            }
        }
    }
}