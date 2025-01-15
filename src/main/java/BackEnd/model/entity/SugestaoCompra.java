// Em src/main/java/seuprojeto/model/entity/SugestaoCompra.java

package BackEnd.model.entity;

public class SugestaoCompra {

    private Item item;
    private double quantidadeSugerida;

    // Construtor
    public SugestaoCompra(Item item, double quantidadeSugerida) {
        this.item = item;
        this.quantidadeSugerida = quantidadeSugerida;
    }

    // Getters e Setters

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getQuantidadeSugerida() {
        return quantidadeSugerida;
    }

    public void setQuantidadeSugerida(double quantidadeSugerida) {
        this.quantidadeSugerida = quantidadeSugerida;
    }
}