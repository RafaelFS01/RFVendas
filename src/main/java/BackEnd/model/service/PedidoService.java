package BackEnd.model.service;

import BackEnd.model.dao.impl.ItemDAOImpl;
import BackEnd.model.dao.impl.ItemPedidoDAOImpl;
import BackEnd.model.dao.impl.PedidoDAOImpl;
import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.dao.interfaces.PedidoDAO;
import BackEnd.model.entity.*; // Importa todas as entidades: Pedido, ItemPedido, Item, Cliente, StatusPedido, TipoPagamento, etc.

import java.sql.SQLException; // Embora não usado diretamente aqui, pode ser relevante para exceções de DAO
import java.time.LocalDate; // Import necessário para dataRetorno e concluirPedido
import java.util.List;
import java.util.Objects; // Para verificações de nulos se necessário

/**
 * Camada de Serviço para a entidade Pedido.
 * Contém a lógica de negócio relacionada a pedidos, intermediando
 * as ações do Controller com a camada de acesso a dados (DAO).
 * Gerencia operações como salvar, atualizar, buscar, cancelar, concluir pedidos
 * e validar dados.
 */
public class PedidoService {

    // Instâncias dos DAOs para interagir com o banco de dados
    private final PedidoDAO pedidoDAO;
    private final ItemPedidoDAO itemPedidoDAO;
    private final ItemDAO itemDAO; // Necessário para atualizar a quantidade do item

    /**
     * Construtor padrão que inicializa as instâncias dos DAOs.
     */
    public PedidoService() {
        this.pedidoDAO = new PedidoDAOImpl();
        this.itemPedidoDAO = new ItemPedidoDAOImpl();
        this.itemDAO = new ItemDAOImpl(); // DAO para interagir com a entidade Item
    }

    /**
     * Salva um novo pedido após realizar validações de negócio.
     * O objeto Pedido já deve conter todos os dados (incluindo tipoPagamento)
     * preenchidos pelo Controller.
     *
     * @param pedido O objeto Pedido a ser salvo (status geralmente definido como EM_ANDAMENTO pelo Controller).
     * @throws Exception Se a validação falhar ou ocorrer um erro no DAO ao salvar.
     */
    public void salvarPedido(Pedido pedido) throws Exception {
        validarPedido(pedido); // Valida os dados essenciais do pedido
        // O DAO será responsável por persistir todos os campos, incluindo tipoPagamento e dataRetorno (se houver)
        pedidoDAO.salvar(pedido);
        // Nota: Os itens são salvos separadamente pela chamada salvarItens() no Controller
    }

    /**
     * Salva os itens associados a um pedido e deduz a quantidade da coluna
     * 'quantidadeAtual' dos itens correspondentes no estoque.
     * Este método é chamado após o pedido principal ter sido salvo (para ter o ID).
     *
     * @param pedido O Pedido (já salvo e com ID) contendo a lista de ItemPedido a ser salva.
     * @throws Exception Se o pedido for inválido, não tiver itens, ou ocorrer erro ao salvar
     *                   os itens ou ao atualizar a quantidade atual do Item.
     */
    public void salvarItens(Pedido pedido) throws Exception {
        if (pedido == null || pedido.getId() == 0) {
            throw new Exception("Pedido inválido ou não salvo para adicionar itens. ID: " + (pedido != null ? pedido.getId() : "null"));
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            // Embora a validação principal esteja em validarPedido, uma checagem aqui é segura.
            System.err.println("Aviso: Tentativa de salvar itens para um pedido sem lista de itens ou lista vazia. Pedido ID: " + pedido.getId());
            return; // Ou lançar exceção se for considerado um erro crítico
        }

        for (ItemPedido itemPedido : pedido.getItens()) {
            if (itemPedido == null || itemPedido.getItem() == null || itemPedido.getItem().getId() == 0) {
                throw new Exception("Pedido contém um ItemPedido inválido ou um Item sem ID. Pedido ID: " + pedido.getId());
            }
            itemPedido.setPedido(pedido); // Garante a associação com o pedido pai
            itemPedidoDAO.salvar(itemPedido); // Salva o ItemPedido no banco

            // Atualiza a quantidade ATUAL do Item no estoque
            Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
            if (item != null) {
                // Verifica se há quantidade suficiente ANTES de deduzir (embora o Controller deva ter validado)
                if (item.getQuantidadeAtual() < itemPedido.getQuantidade()) {
                    // Lançar exceção aqui indica inconsistência, pois deveria ter sido validado antes.
                    throw new Exception("Erro crítico de estoque: Quantidade atual (" + item.getQuantidadeAtual()
                            + ") do item ID " + item.getId() + " é insuficiente para deduzir "
                            + itemPedido.getQuantidade() + ". Pedido ID: " + pedido.getId());
                }
                double novaQuantidadeAtual = item.getQuantidadeAtual() - itemPedido.getQuantidade();
                item.setQuantidadeAtual(novaQuantidadeAtual);
                itemDAO.atualizar(item); // Persiste a atualização da quantidade do item
            } else {
                // Se o item não for encontrado, isso é um problema sério de integridade de dados.
                System.err.println("ERRO CRÍTICO: Item com ID " + itemPedido.getItem().getId() + " referenciado no ItemPedido ID "
                        + itemPedido.getId() + " (Pedido ID " + pedido.getId() + ") não foi encontrado no banco de dados para atualização de quantidade.");
                // Considerar lançar uma exceção específica aqui para sinalizar o erro grave.
                throw new Exception("Inconsistência de dados: Item ID " + itemPedido.getItem().getId() + " não encontrado para atualização de estoque.");
            }
        }
    }

    /**
     * Busca um pedido completo pelo seu ID, incluindo cliente e lista de itens.
     *
     * @param id O ID do pedido a ser buscado.
     * @return O objeto Pedido encontrado, ou null se nenhum pedido com o ID for encontrado.
     * @throws Exception Se ocorrer um erro no DAO durante a busca.
     */
    public Pedido buscarPedidoPorId(int id) throws Exception {
        // O DAO é responsável por buscar todos os campos, incluindo dataRetorno e tipoPagamento
        return pedidoDAO.buscarPorId(id);
    }

    /**
     * Lista todos os pedidos existentes no banco de dados.
     *
     * @return Uma lista contendo todos os objetos Pedido. A lista pode estar vazia se não houver pedidos.
     * @throws Exception Se ocorrer um erro no DAO durante a listagem.
     */
    public List<Pedido> listarPedidos() throws Exception {
        // O DAO é responsável por buscar todos os campos para cada pedido na lista
        return pedidoDAO.listar();
    }

    /**
     * Cancela um pedido, alterando seu status para CANCELADO.
     * Regra de Negócio: Não cancela pedidos já Concluídos.
     * Nota: Esta operação NÃO devolve itens ao estoque por padrão. Uma lógica adicional seria necessária para isso.
     *
     * @param id O ID do pedido a ser cancelado.
     * @throws Exception Se o pedido não for encontrado, já estiver cancelado, for inválido para cancelamento (ex: CONCLUIDO),
     *                   ou se ocorrer um erro no DAO ao atualizar o status.
     */
    public void cancelarPedido(int id) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(id); // Busca para verificar o status atual

        if (pedido == null) {
            throw new Exception("Pedido com ID " + id + " não encontrado para cancelamento.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            // Evita operação redundante ou erros
            System.out.println("Info: Pedido ID " + id + " já está cancelado.");
            // Poderia lançar exceção se preferir: throw new Exception("Pedido já está cancelado.");
            return;
        }
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            // Regra de negócio: Não permitir cancelamento de pedidos concluídos
            throw new Exception("Não é possível cancelar um pedido que já foi concluído. Pedido ID: " + id);
        }

        // Atualiza o status para CANCELADO no banco de dados
        // Passa null para a conexão, pois é uma operação única aqui
        pedidoDAO.atualizarStatus(id, StatusPedido.CANCELADO, null);

        // --- Lógica Opcional de Devolução de Estoque (DESCOMENTE E AJUSTE SE NECESSÁRIO) ---
        // Se ao cancelar, a quantidade dos itens DEVE retornar ao estoque:
        /*
        if (pedido.getItens() != null) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                if (itemPedido != null && itemPedido.getItem() != null) {
                    Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
                    if (item != null) {
                        double novaQuantidadeAtual = item.getQuantidadeAtual() + itemPedido.getQuantidade(); // SOMA DE VOLTA
                        item.setQuantidadeAtual(novaQuantidadeAtual);
                        itemDAO.atualizar(item);
                        System.out.println("Info: Estoque do item ID " + item.getId() + " devolvido ("+itemPedido.getQuantidade()+") ao cancelar pedido ID " + id);
                    } else {
                        System.err.println("ERRO ao devolver estoque no cancelamento: Item ID " + itemPedido.getItem().getId() + " não encontrado. Pedido ID: " + id);
                        // Considerar tratamento de erro mais robusto
                    }
                }
            }
        }
        */
        System.out.println("Pedido ID " + id + " cancelado com sucesso.");
        // Lembrete: A lógica acima de devolução de estoque não está ativa por padrão.
    }

    /**
     * Conclui um pedido, alterando seu status para CONCLUIDO, definindo a data de retorno
     * para a data atual, e **devolvendo** a quantidade dos itens do pedido para a
     * 'quantidadeAtual' no estoque.
     *
     * @param pedidoId O ID do pedido a ser concluído.
     * @throws Exception Se o pedido não for encontrado, não estiver com status 'EM_ANDAMENTO',
     *                   ou ocorrer um erro no DAO ao tentar concluir ou ao atualizar o estoque.
     */
    public void concluirPedido(int pedidoId) throws Exception {
        Pedido pedido = pedidoDAO.buscarPorId(pedidoId); // Busca o pedido completo para validação e acesso aos itens

        // --- Validações de Negócio ---
        if (pedido == null) {
            throw new Exception("Pedido com ID " + pedidoId + " não encontrado para conclusão.");
        }
        // Apenas pedidos 'EM_ANDAMENTO' podem ser concluídos por esta ação.
        if (pedido.getStatus() != StatusPedido.EM_ANDAMENTO) {
            throw new Exception("Apenas pedidos com status 'Em Andamento' podem ser concluídos. Status atual do pedido ID "
                    + pedidoId + ": " + pedido.getStatus());
        }

        // Define a data de retorno como a data atual no momento da conclusão
        LocalDate dataRetornoAtual = LocalDate.now();

        // --- Lógica de Devolução de Estoque ---
        // Ao concluir, a quantidade dos itens RETORNA para a quantidade ATUAL.
        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                if (itemPedido != null && itemPedido.getItem() != null && itemPedido.getItem().getId() != 0) {
                    Item item = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
                    if (item != null) {
                        double novaQuantidadeAtual = item.getQuantidadeAtual() + itemPedido.getQuantidade(); // SOMA DE VOLTA
                        item.setQuantidadeAtual(novaQuantidadeAtual);
                        itemDAO.atualizar(item); // Persiste a devolução no estoque do item
                        System.out.println("Info: Estoque do item ID " + item.getId() + " incrementado em " + itemPedido.getQuantidade() + " ao concluir pedido ID " + pedidoId);
                    } else {
                        // Erro crítico se o item associado não for encontrado
                        System.err.println("ERRO CRÍTICO ao devolver estoque na conclusão: Item ID "
                                + itemPedido.getItem().getId() + " não encontrado. Pedido ID: " + pedidoId);
                        // Lançar exceção pode ser apropriado para indicar falha na operação
                        throw new Exception("Inconsistência de dados: Item ID " + itemPedido.getItem().getId() + " não encontrado para devolução de estoque.");
                    }
                } else {
                    // ItemPedido ou Item inválido na lista do pedido
                    System.err.println("Aviso: ItemPedido inválido encontrado ao processar devolução de estoque para pedido ID " + pedidoId);
                    // Considerar lançar exceção ou pular este item
                }
            }
        } else {
            System.out.println("Info: Pedido ID " + pedidoId + " concluído sem itens para devolver ao estoque.");
        }

        // --- Persistência da Conclusão ---
        // Chama o método do DAO para atualizar o status para CONCLUIDO e definir a data de retorno
        pedidoDAO.concluirPedido(pedidoId, dataRetornoAtual);

        // Log ou notificação de sucesso
        System.out.println("Pedido ID " + pedidoId + " concluído com sucesso. Data de retorno definida como: " + dataRetornoAtual);
    }


    /**
     * Valida os dados essenciais de um objeto Pedido antes de salvar ou atualizar.
     * Verifica campos obrigatórios e regras básicas de negócio (ex: valores positivos).
     *
     * @param pedido O objeto Pedido a ser validado.
     * @throws Exception Se alguma validação falhar, contendo uma mensagem descritiva do erro.
     */
    public void validarPedido(Pedido pedido) throws Exception {
        if (pedido == null) {
            throw new Exception("O objeto Pedido não pode ser nulo.");
        }
        if (pedido.getCliente() == null) {
            throw new Exception("Cliente não selecionado para o pedido.");
        }
        if (pedido.getDataPedido() == null) {
            throw new Exception("Data do pedido não informada.");
        }
        // Opcional: Adicionar validação para tipoPagamento, se for obrigatório
        // if (pedido.getTipoPagamento() == null) {
        //     throw new Exception("Forma de pagamento não selecionada.");
        // }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new Exception("O pedido deve conter pelo menos um item.");
        }
        if (pedido.getStatus() == null) {
            // O status deve ser definido antes de salvar/atualizar
            throw new Exception("Status do pedido não definido.");
        }

        // Valida cada item do pedido
        for (ItemPedido itemPedido : pedido.getItens()) {
            if (itemPedido == null) {
                throw new Exception("Pedido contém uma entrada de item nula na lista.");
            }
            if (itemPedido.getItem() == null) {
                throw new Exception("Pedido contém um item inválido (objeto Item é nulo).");
            }
            if (itemPedido.getQuantidade() <= 0) {
                throw new Exception("A quantidade do item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
            }
            if (itemPedido.getPrecoVenda() <= 0) {
                throw new Exception("O preço de venda do item '" + itemPedido.getItem().getNome() + "' deve ser maior que zero.");
            }
            // Validação adicional de estoque (embora feita no Controller, uma dupla checagem aqui pode ser útil)
            // Item itemAtual = itemDAO.buscarItemPorId(itemPedido.getItem().getId());
            // if (itemAtual != null && itemPedido.getQuantidade() > itemAtual.getQuantidadeAtual()) {
            //     throw new Exception("Quantidade solicitada (" + itemPedido.getQuantidade() + ") para o item '" + itemPedido.getItem().getNome() + "' excede o estoque atual (" + itemAtual.getQuantidadeAtual() + ").");
            // }
        }

        // Valida o valor total (não deve ser negativo)
        if (pedido.getValorTotal() < 0) {
            // O cálculo do valor total deve garantir isso, mas uma verificação final é segura.
            throw new Exception("O valor total do pedido não pode ser negativo. Valor encontrado: " + pedido.getValorTotal());
        }

        // Outras validações de regras de negócio podem ser adicionadas aqui.
        // Ex: Validar se dataRetorno é posterior à dataPedido, se aplicável.
        if (pedido.getDataRetorno() != null && pedido.getDataPedido() != null && pedido.getDataRetorno().isBefore(pedido.getDataPedido())) {
            throw new Exception("A Data de Retorno não pode ser anterior à Data do Pedido.");
        }
    }

    /**
     * Calcula o valor total de uma lista de itens de pedido.
     * Método auxiliar, pode ser usado internamente ou pelo Controller.
     *
     * @param itensPedido A lista de ItemPedido para a qual o total será calculado.
     * @return O valor total calculado (soma de quantidade * precoVenda), ou 0.0 se a lista for nula ou vazia.
     */
    public double calcularValorTotal(List<ItemPedido> itensPedido) {
        if (itensPedido == null || itensPedido.isEmpty()) {
            return 0.0;
        }
        return itensPedido.stream()
                // Garante que itens nulos na lista não causem NullPointerException
                .filter(Objects::nonNull)
                .mapToDouble(itemPedido -> itemPedido.getQuantidade() * itemPedido.getPrecoVenda())
                .sum();
    }

    /**
     * Atualiza os dados principais (não os itens) de um pedido existente no banco de dados.
     * Realiza validação antes de chamar o DAO.
     * Os itens são atualizados separadamente por `atualizarItens`.
     *
     * @param pedido O objeto Pedido com os dados atualizados (ID deve estar presente).
     * @throws Exception Se a validação falhar ou ocorrer erro no DAO ao atualizar.
     */
    public void atualizarPedido(Pedido pedido) throws Exception {
        if (pedido == null || pedido.getId() == 0) {
            throw new Exception("Pedido inválido ou sem ID para atualização.");
        }
        validarPedido(pedido); // Valida os dados gerais do pedido
        // O DAO atualizará os campos no banco, incluindo tipoPagamento, dataRetorno, etc.
        pedidoDAO.atualizarPedido(pedido);
    }

    /**
     * Atualiza a lista de itens associados a um pedido existente.
     * Estratégia: Remove todos os itens antigos do pedido no banco e salva a nova lista de itens.
     * Ao salvar os novos itens, a quantidade ATUAL dos itens correspondentes é deduzida (usando `salvarItens`).
     *
     * @param pedido O Pedido (com ID válido) contendo a **nova** lista completa de itens a serem associados.
     * @throws Exception Se o pedido for inválido, ou ocorrer erro ao excluir itens antigos,
     *                   salvar os novos itens, ou atualizar o estoque dos itens.
     */
    public void atualizarItens(Pedido pedido) throws Exception {
        if (pedido == null || pedido.getId() == 0) {
            throw new Exception("Pedido inválido ou sem ID para atualização de itens.");
        }

        // 1. Buscar e Excluir todos os ItensPedido antigos associados a este pedido no banco.
        List<ItemPedido> itensAntigos = itemPedidoDAO.buscarPorIdPedido(pedido.getId());
        if (itensAntigos != null && !itensAntigos.isEmpty()) {
            System.out.println("Info: Removendo " + itensAntigos.size() + " itens antigos do pedido ID " + pedido.getId() + " antes de atualizar.");
            for (ItemPedido itemAntigo : itensAntigos) {
                // IMPORTANTE: A exclusão do ItemPedido NÃO devolve estoque automaticamente aqui.
                // A devolução só ocorre na conclusão do pedido.
                itemPedidoDAO.excluir(itemAntigo.getId());
            }
        } else {
            System.out.println("Info: Nenhum item antigo encontrado para remover no pedido ID " + pedido.getId() + " durante a atualização.");
        }

        // 2. Salvar a nova lista de itens (se houver).
        //    O método salvarItens já contém a lógica para persistir ItemPedido E deduzir estoque atual do Item.
        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            System.out.println("Info: Salvando " + pedido.getItens().size() + " novos itens para o pedido ID " + pedido.getId() + ".");
            // Reutiliza a lógica de salvar itens e deduzir estoque
            salvarItens(pedido);
        } else {
            // Se a nova lista de itens estiver vazia, nenhum item novo será adicionado.
            System.out.println("Info: Nenhuma nova lista de itens fornecida para o pedido ID " + pedido.getId() + " durante a atualização.");
            // Garante que o valor total seja zero se não houver itens
            // O Controller provavelmente já o fez, mas podemos garantir aqui também.
            if (pedido.getValorTotal() != 0.0) {
                System.out.println("Info: Ajustando valor total do pedido ID " + pedido.getId() + " para 0.0 pois não há mais itens.");
                pedido.setValorTotal(0.0);
                // Atualiza o valor total no pedido principal se foi alterado
                // (Considerar se atualizarPedido deve ser chamado de novo ou se já foi chamado antes de atualizarItens)
                // pedidoDAO.atualizarPedido(pedido); // Cuidado com chamadas recursivas ou duplas
            }
        }
    }
}