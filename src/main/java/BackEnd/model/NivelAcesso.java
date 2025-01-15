package BackEnd.model;

public enum NivelAcesso {
    ADMIN(1, "Administrador", "Acesso total ao sistema"),
    GESTOR(2, "Gestor", "Gerenciamento de empréstimos e relatórios"),
    USUARIO(3, "Usuário Padrão", "Operações básicas");

    private final int nivel;
    private final String descricao;
    private final String detalhes;

    NivelAcesso(int nivel, String descricao, String detalhes) {
        this.nivel = nivel;
        this.descricao = descricao;
        this.detalhes = detalhes;
    }

    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public static NivelAcesso fromNivel(int nivel) {
        for (NivelAcesso n : NivelAcesso.values()) {
            if (n.getNivel() == nivel) {
                return n;
            }
        }
        return USUARIO; // default
    }

    @Override
    public String toString() {
        return this.descricao;
    }
}