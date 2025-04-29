package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ClienteDAO;
// Descomente se ItemDAO for usado em futuras implementações aqui
// import BackEnd.model.dao.interfaces.ItemDAO;
import BackEnd.model.dao.interfaces.ItemPedidoDAO;
import BackEnd.model.dao.interfaces.PedidoDAO;
import BackEnd.model.entity.*; // Importa todas as entidades: Pedido, Cliente, ItemPedido, StatusPedido, TipoPagamento
import BackEnd.util.AlertHelper; // Usado para exibir alertas em cenários específicos (como retries)
import BackEnd.util.ConnectionFactory; // Para obter conexões com o banco de dados

import java.sql.*; // Importações JDBC padrão
import java.time.LocalDate; // Para trabalhar com as datas
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da interface PedidoDAO para interagir com a tabela 'pedidos'
 * no banco de dados usando JDBC.
 * Gerencia as operações de CRUD (Create, Read, Update, Delete - implícito via status)
 * para a entidade Pedido, incluindo o tratamento dos campos dataRetorno e tipoPagamento.
 */
public class PedidoDAOImpl implements PedidoDAO {

    // Dependências de outros DAOs para buscar entidades relacionadas
    private final ClienteDAO clienteDAO;
    private final ItemPedidoDAO itemPedidoDAO;
    // private final ItemDAO itemDAO; // Instanciar se for necessário interagir com Item diretamente aqui

    /**
     * Construtor que inicializa os DAOs dependentes.
     */
    public PedidoDAOImpl() {
        this.clienteDAO = new ClienteDAOImpl(); // Cria instância para buscar Clientes
        this.itemPedidoDAO = new ItemPedidoDAOImpl(); // Cria instância para buscar ItemPedido
        // this.itemDAO = new ItemDAOImpl();
    }

    /**
     * Salva um novo pedido no banco de dados, incluindo dataRetorno (se houver) e tipoPagamento (se houver).
     * Implementa lógica de retry simples para timeouts de lock.
     *
     * @param pedido O objeto Pedido a ser salvo. O ID será populado após a inserção.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados ou após múltiplas tentativas de retry.
     */
    @Override
    public void salvar(Pedido pedido) throws Exception {
        // SQL para inserir um novo registro na tabela pedidos
        String sql = "INSERT INTO pedidos (cliente_id, data_pedido, data_retorno, valor_total, status, observacoes, tipo_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        int maxRetries = 3; // Número máximo de tentativas em caso de timeout
        int retryDelay = 500; // Atraso inicial em milissegundos

        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                conn = ConnectionFactory.getConnection(); // Obtém uma conexão
                conn.setAutoCommit(false); // Inicia a transação

                // Usa try-with-resources para garantir que o PreparedStatement seja fechado
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Solicita as chaves geradas (ID)

                    // Define os parâmetros do PreparedStatement
                    if (pedido.getCliente() != null && pedido.getCliente().getId() != null) {
                        stmt.setString(1, pedido.getCliente().getId());
                    } else {
                        // Lançar exceção ou tratar se cliente for obrigatório
                        throw new SQLException("Cliente inválido ou ID do cliente nulo ao salvar pedido.");
                    }
                    stmt.setDate(2, java.sql.Date.valueOf(pedido.getDataPedido()));

                    // Trata dataRetorno (pode ser nulo)
                    if (pedido.getDataRetorno() != null) {
                        stmt.setDate(3, java.sql.Date.valueOf(pedido.getDataRetorno()));
                    } else {
                        stmt.setNull(3, Types.DATE); // Define como NULL no banco
                    }

                    stmt.setDouble(4, pedido.getValorTotal());
                    stmt.setString(5, pedido.getStatus().name()); // Salva o nome do enum StatusPedido
                    stmt.setString(6, pedido.getObservacoes());

                    // Trata tipoPagamento (pode ser nulo)
                    if (pedido.getTipoPagamento() != null) {
                        stmt.setString(7, pedido.getTipoPagamento().name()); // Salva o nome do enum TipoPagamento
                    } else {
                        stmt.setNull(7, Types.VARCHAR); // Define como NULL no banco
                    }

                    // Executa a inserção
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Falha ao salvar o pedido, nenhuma linha afetada.");
                    }

                    // Recupera o ID gerado pelo banco
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            pedido.setId(generatedKeys.getInt(1)); // Define o ID no objeto Pedido
                        } else {
                            throw new SQLException("Falha ao obter o ID gerado para o pedido.");
                        }
                    }
                } // Fim do try-with-resources para PreparedStatement

                conn.commit(); // Confirma a transação se tudo ocorreu bem
                System.out.println("Pedido ID " + pedido.getId() + " salvo com sucesso.");
                // ConnectionFactory.exportarBancoDeDados("BACKUP_APOS_PEDIDO_" + pedido.getId()); // Opcional: Backup após sucesso
                return; // Sai do método após sucesso

            } catch (Exception e) {
                // Lógica de Rollback e Retry (mantida da versão anterior)
                if (conn != null) {
                    try {
                        System.err.println("Erro ao salvar pedido ID " + pedido.getId() + ", realizando rollback...");
                        conn.rollback(); // Desfaz a transação em caso de erro
                    } catch (SQLException exRollback) {
                        System.err.println("Erro CRÍTICO ao tentar realizar rollback: " + exRollback.getMessage());
                        // Adiciona o erro de rollback à exceção original
                        e.addSuppressed(exRollback);
                    }
                }

                // Verifica se é um erro de timeout e se ainda pode tentar novamente
                // Ajuste a verificação do tipo de exceção e mensagem conforme o seu driver JDBC e banco
                boolean isTimeoutError = (e instanceof SQLException && e.getMessage().toLowerCase().contains("lock wait timeout exceeded")) ||
                        (e.getCause() instanceof SQLException && e.getCause().getMessage().toLowerCase().contains("lock wait timeout exceeded"));

                if (isTimeoutError && retryCount < maxRetries - 1) {
                    System.err.println("Timeout detectado ao salvar pedido. Tentando novamente em " + retryDelay + "ms... (" + (retryCount + 2) + "/" + maxRetries + ")");
                    AlertHelper.showWarning("Timeout Banco de Dados", "Ocorreu um timeout ao salvar, tentando novamente (" + (retryCount + 2) + "/" + maxRetries + "). Aguarde...");
                    try {
                        Thread.sleep(retryDelay); // Espera antes de tentar novamente
                        retryDelay *= 2; // Aumenta o tempo de espera (backoff exponencial)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Restaura o status de interrupção
                        System.err.println("Thread interrompida durante o sleep para retry.");
                        throw new Exception("Operação de salvar pedido interrompida durante espera para retry.", ie);
                    }
                    // Continua para a próxima iteração do loop
                } else {
                    // Se não for timeout ou se excedeu as tentativas, lança a exceção original
                    throw new Exception("Erro final ao salvar pedido (após " + (retryCount + 1) + " tentativas): " + e.getMessage(), e);
                }
            } finally {
                // Garante que a conexão seja fechada, mesmo se ocorrerem erros
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException exClose) {
                        System.err.println("Erro ao fechar a conexão do banco: " + exClose.getMessage());
                        // Adiciona como suprimido à exceção principal se houver uma
                    }
                }
            }
        } // Fim do loop de retry
        // Se chegou aqui, todas as tentativas falharam
        throw new Exception("Não foi possível salvar o pedido após " + maxRetries + " tentativas devido a timeouts repetidos.");
    }

    /**
     * Busca um pedido pelo seu ID, carregando todos os seus dados, incluindo cliente,
     * itens, dataRetorno e tipoPagamento.
     *
     * @param id O ID do pedido a ser buscado.
     * @return O objeto Pedido encontrado, ou null se não existir.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    @Override
    public Pedido buscarPorId(int id) throws Exception {
        // SQL para selecionar um pedido específico pelo ID
        String sql = "SELECT * FROM pedidos WHERE id = ?";
        Pedido pedido = null;

        // Usa try-with-resources para Connection, PreparedStatement e ResultSet
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id); // Define o parâmetro ID na query

            try (ResultSet rs = stmt.executeQuery()) { // Executa a busca
                if (rs.next()) { // Se encontrou um resultado
                    pedido = new Pedido(); // Cria o objeto Pedido
                    pedido.setId(rs.getInt("id"));

                    // Busca o Cliente associado usando o ClienteDAO
                    String clienteId = rs.getString("cliente_id");
                    if (clienteId != null) {
                        Cliente cliente = clienteDAO.buscarPorId(clienteId);
                        pedido.setCliente(cliente); // Associa o cliente ao pedido
                        if (cliente == null) {
                            System.err.println("Aviso: Cliente com ID '" + clienteId + "' referenciado no pedido ID " + id + " não encontrado.");
                            // Tratar conforme a regra de negócio (lançar erro, continuar com cliente nulo?)
                        }
                    } else {
                        System.err.println("Aviso: Pedido ID " + id + " possui cliente_id nulo no banco.");
                    }


                    pedido.setDataPedido(rs.getDate("data_pedido").toLocalDate()); // Converte SQL Date para LocalDate

                    // Carrega dataRetorno (pode ser nulo)
                    Date dataRetornoSql = rs.getDate("data_retorno");
                    pedido.setDataRetorno(dataRetornoSql != null ? dataRetornoSql.toLocalDate() : null);

                    pedido.setValorTotal(rs.getDouble("valor_total"));
                    pedido.setStatus(StatusPedido.valueOf(rs.getString("status"))); // Converte String para Enum StatusPedido
                    pedido.setObservacoes(rs.getString("observacoes"));

                    // Carrega tipoPagamento (pode ser nulo ou inválido no banco)
                    String tipoPagamentoStr = rs.getString("tipo_pagamento");
                    if (tipoPagamentoStr != null && !tipoPagamentoStr.trim().isEmpty()) {
                        try {
                            pedido.setTipoPagamento(TipoPagamento.valueOf(tipoPagamentoStr)); // Converte String para Enum TipoPagamento
                        } catch (IllegalArgumentException e) {
                            System.err.println("Alerta: Valor inválido de tipo_pagamento ('" + tipoPagamentoStr +
                                    "') encontrado no banco para pedido ID: " + id + ". Definindo como nulo.");
                            pedido.setTipoPagamento(null); // Define como nulo se o valor do banco não for um Enum válido
                        }
                    } else {
                        pedido.setTipoPagamento(null); // Define como nulo se estiver vazio ou nulo no banco
                    }

                    // Busca os itens associados a este pedido usando ItemPedidoDAO
                    List<ItemPedido> itensPedido = itemPedidoDAO.buscarPorIdPedido(id);
                    pedido.setItens(itensPedido); // Associa a lista de itens ao pedido
                }
            } // Fim do try-with-resources para ResultSet
        } catch (SQLException e) {
            throw new Exception("Erro ao buscar pedido por ID (" + id + "): " + e.getMessage(), e);
        }
        return pedido; // Retorna o pedido encontrado ou null
    }

    /**
     * Lista todos os pedidos existentes no banco de dados, ordenados por data decrescente.
     *
     * @return Uma lista de objetos Pedido. A lista estará vazia se não houver pedidos.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados.
     */
    @Override
    public List<Pedido> listar() throws Exception {
        // SQL para selecionar todos os pedidos, ordenados
        String sql = "SELECT * FROM pedidos ORDER BY data_pedido DESC, id DESC";
        List<Pedido> pedidos = new ArrayList<>(); // Lista para armazenar os resultados

        // Usa try-with-resources para Connection, PreparedStatement e ResultSet
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) { // Executa a query

            while (rs.next()) { // Itera sobre cada linha do resultado
                Pedido pedido = new Pedido(); // Cria um objeto Pedido para cada linha
                pedido.setId(rs.getInt("id"));

                // Busca o Cliente associado
                String clienteId = rs.getString("cliente_id");
                if (clienteId != null) {
                    Cliente cliente = clienteDAO.buscarPorId(clienteId);
                    pedido.setCliente(cliente);
                    if (cliente == null) {
                        System.err.println("Aviso (Listar): Cliente com ID '" + clienteId + "' referenciado no pedido ID " + pedido.getId() + " não encontrado.");
                    }
                } else {
                    System.err.println("Aviso (Listar): Pedido ID " + pedido.getId() + " possui cliente_id nulo no banco.");
                }

                pedido.setDataPedido(rs.getDate("data_pedido").toLocalDate());

                // Carrega dataRetorno
                Date dataRetornoSql = rs.getDate("data_retorno");
                pedido.setDataRetorno(dataRetornoSql != null ? dataRetornoSql.toLocalDate() : null);

                pedido.setValorTotal(rs.getDouble("valor_total"));
                pedido.setStatus(StatusPedido.valueOf(rs.getString("status")));
                pedido.setObservacoes(rs.getString("observacoes"));

                // Carrega tipoPagamento
                String tipoPagamentoStr = rs.getString("tipo_pagamento");
                if (tipoPagamentoStr != null && !tipoPagamentoStr.trim().isEmpty()) {
                    try {
                        pedido.setTipoPagamento(TipoPagamento.valueOf(tipoPagamentoStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Alerta (Listar): Valor inválido de tipo_pagamento ('" + tipoPagamentoStr +
                                "') encontrado no banco para pedido ID: " + pedido.getId() + ". Definindo como nulo.");
                        pedido.setTipoPagamento(null);
                    }
                } else {
                    pedido.setTipoPagamento(null);
                }

                // Busca os itens associados a este pedido
                // ATENÇÃO: Isso pode causar o problema N+1 (uma query para cada pedido).
                // Para muitos pedidos, considere fazer um JOIN ou buscar itens em lote.
                List<ItemPedido> itensPedido = itemPedidoDAO.buscarPorIdPedido(pedido.getId());
                pedido.setItens(itensPedido);

                pedidos.add(pedido); // Adiciona o pedido populado à lista
            }
        } catch (SQLException e) {
            throw new Exception("Erro ao listar pedidos: " + e.getMessage(), e);
        }
        return pedidos; // Retorna a lista de pedidos
    }

    /**
     * Atualiza apenas o status de um pedido específico.
     * Pode ser usado dentro de uma transação maior se uma conexão for fornecida.
     *
     * @param id O ID do pedido a ter o status atualizado.
     * @param status O novo StatusPedido a ser definido.
     * @param conn Uma conexão JDBC opcional. Se nula, uma nova conexão será criada e fechada.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados ou se o pedido não for encontrado.
     */
    @Override
    public void atualizarStatus(int id, StatusPedido status, Connection conn) throws Exception {
        String sql = "UPDATE pedidos SET status = ? WHERE id = ?";
        boolean localConnection = (conn == null); // Verifica se precisa gerenciar a conexão localmente
        Connection currentConn = null;

        try {
            // Obtém a conexão: usa a fornecida ou cria uma nova
            currentConn = localConnection ? ConnectionFactory.getConnection() : conn;
            if (localConnection) {
                currentConn.setAutoCommit(true); // Garante autocommit se a conexão for local
            }

            // Usa try-with-resources apenas para o PreparedStatement
            try (PreparedStatement stmt = currentConn.prepareStatement(sql)) {
                stmt.setString(1, status.name()); // Define o novo status
                stmt.setInt(2, id); // Define o ID do pedido

                int affectedRows = stmt.executeUpdate(); // Executa a atualização
                if (affectedRows == 0) {
                    // Se nenhuma linha foi afetada, o pedido com o ID não existe
                    throw new SQLException("Erro ao atualizar status: Pedido com ID " + id + " não encontrado.");
                }
                System.out.println("Status do pedido ID " + id + " atualizado para " + status.name());
            } // Fim do try-with-resources para PreparedStatement

        } catch (SQLException e) {
            // Se ocorrer um erro SQL, lança uma exceção encapsulada
            throw new Exception("Erro ao atualizar status do pedido ID " + id + ": " + e.getMessage(), e);
        } finally {
            // Fecha a conexão somente se ela foi criada localmente neste método
            if (localConnection && currentConn != null) {
                try {
                    currentConn.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão local em atualizarStatus: " + e.getMessage());
                    // Considerar adicionar como suprimida à exceção principal se houver uma
                }
            }
        }
    }

    /**
     * Atualiza todos os dados de um pedido existente no banco de dados.
     * Assume que o objeto Pedido contém o ID e os novos valores para os campos.
     *
     * @param pedido O objeto Pedido com os dados atualizados.
     * @throws Exception Se ocorrer um erro durante a operação no banco de dados ou se o pedido não for encontrado.
     */
    @Override
    public void atualizarPedido(Pedido pedido) throws Exception {
        // SQL para atualizar um registro de pedido existente
        String sql = "UPDATE pedidos SET cliente_id = ?, data_pedido = ?, data_retorno = ?, valor_total = ?, status = ?, observacoes = ?, tipo_pagamento = ? WHERE id = ?";

        // Usa try-with-resources para Connection e PreparedStatement
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Define os parâmetros para a atualização
            if (pedido.getCliente() != null && pedido.getCliente().getId() != null) {
                stmt.setString(1, pedido.getCliente().getId());
            } else {
                // Lançar exceção ou definir como NULL dependendo da regra de negócio
                // stmt.setNull(1, Types.VARCHAR);
                throw new SQLException("Cliente inválido ou ID do cliente nulo ao atualizar pedido ID " + pedido.getId());
            }

            stmt.setDate(2, java.sql.Date.valueOf(pedido.getDataPedido()));

            // Trata dataRetorno
            if (pedido.getDataRetorno() != null) {
                stmt.setDate(3, java.sql.Date.valueOf(pedido.getDataRetorno()));
            } else {
                stmt.setNull(3, Types.DATE);
            }

            stmt.setDouble(4, pedido.getValorTotal());
            stmt.setString(5, pedido.getStatus().name());
            stmt.setString(6, pedido.getObservacoes());

            // Trata tipoPagamento
            if (pedido.getTipoPagamento() != null) {
                stmt.setString(7, pedido.getTipoPagamento().name());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            // Define o ID na cláusula WHERE (último parâmetro)
            stmt.setInt(8, pedido.getId());

            // Executa a atualização
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                // Se nenhuma linha foi afetada, o pedido com o ID não existe
                throw new SQLException("Erro ao atualizar pedido: Pedido com ID " + pedido.getId() + " não encontrado.");
            }
            System.out.println("Pedido ID " + pedido.getId() + " atualizado com sucesso.");

        } catch (SQLException e) {
            // Lança uma exceção encapsulada em caso de erro SQL
            throw new Exception("Erro ao atualizar pedido ID " + pedido.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza o status de um pedido para CONCLUIDO e define a data de retorno no banco de dados.
     *
     * @param id O ID do pedido a ser concluído.
     * @param dataRetorno A data em que o pedido foi retornado/concluído (não deve ser nula).
     * @throws Exception Se ocorrer um erro no banco de dados, o pedido não for encontrado, ou dataRetorno for nula.
     */
    @Override
    public void concluirPedido(int id, LocalDate dataRetorno) throws Exception {
        // SQL para atualizar status e data de retorno
        String sql = "UPDATE pedidos SET status = ?, data_retorno = ? WHERE id = ?";

        // Validação prévia da data (embora o Service deva garantir)
        if (dataRetorno == null) {
            // É uma condição de erro, pois a conclusão implica uma data de retorno.
            throw new IllegalArgumentException("Data de retorno não pode ser nula ao concluir o pedido ID " + id);
        }

        // Usa try-with-resources para Connection e PreparedStatement
        try (Connection conn = ConnectionFactory.getConnection(); // Autocommit é true por padrão
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Define os parâmetros
            stmt.setString(1, StatusPedido.CONCLUIDO.name()); // Status fixo para conclusão
            stmt.setDate(2, java.sql.Date.valueOf(dataRetorno)); // Define a data de retorno
            stmt.setInt(3, id); // Define o ID do pedido na cláusula WHERE

            // Executa a atualização
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                // Se nenhuma linha foi afetada, o pedido com o ID não foi encontrado
                throw new SQLException("Nenhum pedido encontrado com ID " + id + " para marcar como concluído.");
            }
            System.out.println("Pedido ID " + id + " marcado como CONCLUIDO com data de retorno " + dataRetorno + ".");

        } catch (SQLException e) {
            // Encapsula a SQLException em uma Exception mais genérica para a camada de serviço
            throw new Exception("Erro no banco de dados ao tentar concluir o pedido ID " + id + ": " + e.getMessage(), e);
        }
    }
}