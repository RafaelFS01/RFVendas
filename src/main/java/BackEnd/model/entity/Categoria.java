// Em src/main/java/seuprojeto/model/entity/Categoria.java

package BackEnd.model.entity;

public class Categoria {
    private Integer id;
    private String nome;
    private String descricao;

    // Getters e Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return nome; // Retorna o nome da categoria
    }
}