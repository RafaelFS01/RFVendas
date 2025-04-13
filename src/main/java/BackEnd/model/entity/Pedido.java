package BackEnd.model.entity;

import java.time.LocalDate;
import java.util.List;

public class Pedido {
    private int id;
    private Cliente cliente;
    // REMOVIDO: private TipoVenda tipoVenda;
    private LocalDate dataPedido;
    private LocalDate dataRetorno; // ADICIONADO
    private double valorTotal;
    private StatusPedido status;
    private List<ItemPedido> itens;
    private String observacoes;

    // Construtor vazio
    public Pedido() {
    }

    // Construtor com campos (sem tipoVenda, com dataRetorno)
    // Ajuste conforme necessidade - talvez nem todos os campos sejam necessários no construtor
    public Pedido(int id, Cliente cliente, LocalDate dataPedido, LocalDate dataRetorno,
                  double valorTotal, StatusPedido status, List<ItemPedido> itens, String observacoes) {
        this.id = id;
        this.cliente = cliente;
        this.dataPedido = dataPedido;
        this.dataRetorno = dataRetorno; // ADICIONADO
        this.valorTotal = valorTotal;
        this.status = status;
        this.itens = itens;
        this.observacoes = observacoes;
    }

    // --- Getters e Setters ---

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

    // REMOVIDO: getTipoVenda() e setTipoVenda()
    /*
    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(TipoVenda tipoVenda) {
        this.tipoVenda = tipoVenda;
    }
    */

    public LocalDate getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDate dataPedido) {
        this.dataPedido = dataPedido;
    }

    // ADICIONADO: Getter e Setter para dataRetorno
    public LocalDate getDataRetorno() {
        return dataRetorno;
    }

    public void setDataRetorno(LocalDate dataRetorno) {
        this.dataRetorno = dataRetorno;
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
        // Ao definir itens, recalcular valor total pode ser uma boa prática
        // this.valorTotal = calcularValorTotalInterno(); // Exemplo
        this.itens = itens;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    // Opcional: Método interno para recalcular o valor total se a lista de itens for modificada
    // private double calcularValorTotalInterno() {
    //     if (this.itens == null) {
    //         return 0.0;
    //     }
    //     return this.itens.stream()
    //              .mapToDouble(ip -> ip.getQuantidade() * ip.getPrecoVenda())
    //              .sum();
    // }

    // toString(), equals(), hashCode() podem ser úteis, mas omitidos por brevidade.
}