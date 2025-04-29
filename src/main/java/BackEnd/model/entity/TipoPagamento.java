package BackEnd.model.entity;

/**
 * Enumeração que representa os tipos de pagamento possíveis para um pedido.
 * Cada constante possui uma descrição amigável para exibição na interface.
 */
public enum TipoPagamento {

    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    PIX("Pix"),
    CHEQUE("Cheque"),
    BOLETO_BANCARIO("Boleto Bancário");

    private final String descricao;

    /**
     * Construtor privado para associar uma descrição a cada tipo de pagamento.
     * @param descricao A descrição amigável do tipo de pagamento.
     */
    TipoPagamento(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna a descrição amigável do tipo de pagamento.
     * Útil para exibição em interfaces de usuário (ComboBox, Labels, etc.).
     * @return A descrição do tipo de pagamento.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna a descrição amigável do tipo de pagamento.
     * Sobrescreve o método toString() padrão para facilitar o uso direto
     * em componentes JavaFX (como ComboBox) que o utilizam por padrão
     * para exibir o texto do item, dispensando a necessidade de um StringConverter
     * customizado apenas para mostrar a descrição.
     *
     * @return A descrição do tipo de pagamento.
     */
    @Override
    public String toString() {
        return this.descricao;
    }
}