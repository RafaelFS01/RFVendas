package BackEnd.model.entity;

public class LancamentoItem {
    private int idItem;
    private double quantidade;
    private double custo;

    public LancamentoItem(int idItem, double quantidade, double custo) {
        this.idItem = idItem;
        this.quantidade = quantidade;
        this.custo = custo;
    }

    // Getters e Setters
    public int getIdItem() {
        return idItem;
    }

    public void setIdItem(int idItem) {
        this.idItem = idItem;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public double getCusto() {
        return custo;
    }

    public void setCusto(double custo) {
        this.custo = custo;
    }
}