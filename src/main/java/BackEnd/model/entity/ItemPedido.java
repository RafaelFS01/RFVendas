package BackEnd.model.entity;

public class ItemPedido {
    private int id;
    private Pedido pedido;
    private Item item;
    private double quantidade;
    private double precoVenda;

    // Construtor vazio
    public ItemPedido() {
    }

    // Construtor com todos os campos
    public ItemPedido(int id, Pedido pedido, Item item, double quantidade, double precoVenda) {
        this.id = id;
        this.pedido = pedido;
        this.item = item;
        this.quantidade = quantidade;
        this.precoVenda = precoVenda;
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(double precoVenda) {
        this.precoVenda = precoVenda;
    }
}