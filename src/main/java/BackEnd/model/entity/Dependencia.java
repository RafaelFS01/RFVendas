package BackEnd.model.entity;

public class Dependencia {

    private int id;
    private int idItemDependente;
    private int idItemNecessario;
    private int idCategoria;
    private double quantidade;

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdItemDependente() {
        return idItemDependente;
    }

    public void setIdItemDependente(int idItemDependente) {
        this.idItemDependente = idItemDependente;
    }

    public int getIdItemNecessario() {
        return idItemNecessario;
    }

    public void setIdItemNecessario(int idItemNecessario) {
        this.idItemNecessario = idItemNecessario;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    // (Opcional) Método toString para facilitar a visualização dos dados
    @Override
    public String toString() {
        return "Dependencia{" +
                "id=" + id +
                ", idItemDependente=" + idItemDependente +
                ", idItemNecessario=" + idItemNecessario +
                ", idCategoria=" + idCategoria +
                ", quantidade=" + quantidade +
                '}';
    }
}
