package BackEnd.model.entity;

public class Usuario {
    private int id;
    private String username;
    private String password;
    private String nome;
    private String email;
    private int nivelAcesso;
    private boolean ativo;

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(int nivelAcesso) { this.nivelAcesso = nivelAcesso; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}

