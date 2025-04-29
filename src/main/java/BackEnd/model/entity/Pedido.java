package BackEnd.model.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects; // Import para Objects.hash e Objects.equals

/**
 * Representa um Pedido no sistema.
 * Contém informações sobre o cliente, datas, itens, status, valor total,
 * forma de pagamento e observações.
 */
public class Pedido {

    private int id;                     // Identificador único do pedido
    private Cliente cliente;            // Cliente associado ao pedido
    // REMOVIDO: private TipoVenda tipoVenda; // Não é mais utilizado
    private LocalDate dataPedido;       // Data em que o pedido foi realizado
    private LocalDate dataRetorno;      // Data em que o pedido foi concluído/retornado (ADICIONADO)
    private TipoPagamento tipoPagamento;  // Forma como o pedido foi/será pago (NOVO CAMPO ADICIONADO)
    private double valorTotal;          // Valor total calculado do pedido
    private StatusPedido status;        // Status atual do pedido (ex: EM_ANDAMENTO, CONCLUIDO)
    private List<ItemPedido> itens;     // Lista dos itens que compõem o pedido
    private String observacoes;         // Observações adicionais sobre o pedido

    /**
     * Construtor padrão (vazio).
     * Necessário para algumas bibliotecas e frameworks (como ORM ou desserialização).
     */
    public Pedido() {
    }

    /**
     * Construtor completo com todos os campos atuais da entidade.
     *
     * @param id            O ID do pedido.
     * @param cliente       O Cliente associado.
     * @param dataPedido    A data de realização do pedido.
     * @param dataRetorno   A data de retorno/conclusão do pedido (pode ser nulo).
     * @param tipoPagamento A forma de pagamento selecionada (pode ser nulo).
     * @param valorTotal    O valor total do pedido.
     * @param status        O status atual do pedido.
     * @param itens         A lista de itens do pedido.
     * @param observacoes   Observações adicionais.
     */
    public Pedido(int id, Cliente cliente, LocalDate dataPedido, LocalDate dataRetorno, TipoPagamento tipoPagamento,
                  double valorTotal, StatusPedido status, List<ItemPedido> itens, String observacoes) {
        this.id = id;
        this.cliente = cliente;
        this.dataPedido = dataPedido;
        this.dataRetorno = dataRetorno;         // Atribui dataRetorno
        this.tipoPagamento = tipoPagamento;     // Atribui tipoPagamento
        this.valorTotal = valorTotal;
        this.status = status;
        this.itens = itens;                     // Cuidado: Atribui a referência da lista, não uma cópia.
        this.observacoes = observacoes;
    }

    // --- Getters e Setters ---
    // Métodos para acessar e modificar os atributos da classe.

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

    // REMOVIDO: Getter e Setter para tipoVenda não existem mais.
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

    // NOVO: Getter e Setter para tipoPagamento
    public TipoPagamento getTipoPagamento() {
        return tipoPagamento;
    }

    public void setTipoPagamento(TipoPagamento tipoPagamento) {
        this.tipoPagamento = tipoPagamento;
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

    /**
     * Define a lista de itens do pedido.
     * Considerar se uma cópia da lista deve ser feita para evitar modificações externas.
     * @param itens A nova lista de ItemPedido.
     */
    public void setItens(List<ItemPedido> itens) {
        // Exemplo: se quisesse garantir que a lista interna é sempre uma ArrayList e cópia:
        // this.itens = (itens != null) ? new ArrayList<>(itens) : new ArrayList<>();
        this.itens = itens;
        // Opcionalmente, recalcular o valor total sempre que os itens são definidos:
        // this.valorTotal = calcularValorTotalInterno(); // Se existir método interno
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    // --- Métodos Utilitários (Opcionais) ---

    /**
     * Gera uma representação em String do objeto Pedido.
     * Útil para logging e depuração.
     * @return Uma String descrevendo o pedido.
     */
    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", cliente=" + (cliente != null ? cliente.getNome() : "null") + // Exibe nome do cliente
                ", dataPedido=" + dataPedido +
                ", dataRetorno=" + dataRetorno +
                ", tipoPagamento=" + tipoPagamento +
                ", valorTotal=" + valorTotal +
                ", status=" + status +
                ", itens=" + (itens != null ? "size=" + itens.size() : "null") + // Mostra tamanho da lista de itens
                ", observacoes='" + observacoes + '\'' +
                '}';
    }

    /**
     * Compara este Pedido com outro objeto para verificar igualdade.
     * Dois pedidos são considerados iguais se tiverem o mesmo ID (assumindo que ID é único).
     * @param o O objeto a ser comparado.
     * @return true se os objetos forem iguais, false caso contrário.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Mesma referência
        if (o == null || getClass() != o.getClass()) return false; // Objeto nulo ou classe diferente
        Pedido pedido = (Pedido) o;
        // Compara apenas pelo ID, assumindo que é a chave primária e único
        // Se id for 0 (pedido ainda não salvo), a comparação de objeto padrão pode ser mais apropriada
        // ou comparar por outros campos se houver uma chave natural.
        return id != 0 && id == pedido.id;
    }

    /**
     * Gera um código hash para o objeto Pedido.
     * Baseado principalmente no ID, se for diferente de 0.
     * @return O código hash.
     */
    @Override
    public int hashCode() {
        // Usa o ID para o hash se for diferente de 0, caso contrário usa o hash padrão do objeto
        return (id != 0) ? Objects.hash(id) : super.hashCode();
        // Alternativa: Incluir mais campos no hash se a comparação 'equals' for mais complexa
        // return Objects.hash(id, cliente, dataPedido, dataRetorno, tipoPagamento, valorTotal, status, itens, observacoes);
    }

    // Opcional: Método interno para recalcular o valor total
    // private double calcularValorTotalInterno() {
    //     if (this.itens == null) {
    //         return 0.0;
    //     }
    //     return this.itens.stream()
    //              .filter(Objects::nonNull) // Evita NullPointerException se item for nulo na lista
    //              .mapToDouble(ip -> ip.getQuantidade() * ip.getPrecoVenda())
    //              .sum();
    // }
}