package BackEnd.model.entity;

import java.time.LocalDate;
import java.util.List;

public class Pedido {
    private int id;
    private Cliente cliente;
    private TipoVenda tipoVenda;
    private LocalDate dataPedido;
    private double valorTotal;
    private StatusPedido status;
    private List<ItemPedido> itens;
    private String observacoes;

    // Construtor vazio
    public Pedido() {
    }

    // Construtor com todos os campos
    public Pedido(int id, Cliente cliente, TipoVenda tipoVenda, LocalDate dataPedido,
                  double valorTotal, StatusPedido status, List<ItemPedido> itens, String observacoes) {
        this.id = id;
        this.cliente = cliente;
        this.tipoVenda = tipoVenda;
        this.dataPedido = dataPedido;
        this.valorTotal = valorTotal;
        this.status = status;
        this.itens = itens;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(TipoVenda tipoVenda) {
        this.tipoVenda = tipoVenda;
    }

    public LocalDate getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDate dataPedido) {
        this.dataPedido = dataPedido;
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }
    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}