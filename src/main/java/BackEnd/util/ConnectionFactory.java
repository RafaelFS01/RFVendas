package BackEnd.util;

// Importe BCrypt para hashing de senha

// Imports necessários para as funções originais e novas
import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.List;

public class ConnectionFactory {

    // --- Configuração ---
    private static final String CONFIG_FILE = "config.properties"; // Nome do arquivo em src/main/resources
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    // Senha padrão que será HASHADA antes de salvar no banco
    private static final String DEFAULT_ADMIN_PASSWORD_PLAINTEXT = "1234";
    // -----------------------------------------------------------------------

    public static Properties props = new Properties();
    private static String databaseSpecificUrl; // Armazena a URL final do banco (ex: jdbc:mysql://host:port/rfvendas?params...)
    private static boolean initializationOk = false; // Flag para indicar se a inicialização deu certo

    // Bloco estático: Executado UMA VEZ quando a classe é carregada pela JVM.
    // Responsável por carregar propriedades e inicializar o banco/tabelas/usuário admin.
    static {
        System.out.println("[DB Setup] Iniciando configuração completa do banco de dados...");
        try {
            loadProperties(); // 1. Carrega as propriedades do config.properties
            initializeDatabaseSchema(); // 2. Tenta criar o banco e todas as tabelas
            ensureDefaultAdminUserExists(); // 3. Garante que o usuário admin padrão exista
            initializationOk = true; // Marca que tudo correu bem
            System.out.println("[DB Setup] Configuração do banco de dados e usuário admin concluída com sucesso.");

        } catch (Exception e) { // Captura qualquer erro durante o setup (IO, SQL, etc.)
            initializationOk = false; // Marca que a inicialização falhou
            System.err.println("*****************************************************");
            System.err.println("ERRO FATAL DURANTE A INICIALIZAÇÃO DO BANCO DE DADOS OU USUÁRIO ADMIN!");
            System.err.println("A aplicação pode não funcionar corretamente.");
            // (Mensagens de erro detalhadas omitidas para brevidade, veja versões anteriores se necessário)
            e.printStackTrace(); // Imprime o stack trace do erro original
            System.err.println("*****************************************************");
            // Considerar lançar RuntimeException para parar a aplicação
            // throw new RuntimeException("Falha crítica na inicialização do banco de dados.", e);
        }
    }

    /**
     * Carrega as propriedades do arquivo config.properties localizado no classpath.
     * Valida as propriedades essenciais e constrói a URL específica do banco.
     * @throws IOException Se o arquivo não for encontrado, não puder ser lido, ou faltarem propriedades essenciais.
     */
    private static void loadProperties() throws IOException {
        try (InputStream input = ConnectionFactory.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                System.err.println("[DB Setup] ERRO CRÍTICO: Arquivo '" + CONFIG_FILE + "' não encontrado no classpath.");
                setDefaultPropertiesOnError();
                throw new IOException("Arquivo de configuração '" + CONFIG_FILE + "' não encontrado no classpath.");
            } else {
                props.load(input);
                System.out.println("[DB Setup] Arquivo de configuração '" + CONFIG_FILE + "' carregado.");

                // Validar propriedades carregadas
                String serverUrl = props.getProperty("db.server.url");
                String dbName = props.getProperty("db.name");
                String user = props.getProperty("db.user");
                // String password = props.getProperty("db.password"); // Não precisa validar aqui

                if (serverUrl == null || serverUrl.trim().isEmpty() ||
                        dbName == null || dbName.trim().isEmpty() ||
                        user == null || user.trim().isEmpty() ) {
                    throw new IOException("Propriedades essenciais ('db.server.url', 'db.name', 'db.user') estão vazias ou não encontradas no config.");
                }
                serverUrl = serverUrl.trim();
                dbName = dbName.trim();

                // ----- CORREÇÃO NA CONSTRUÇÃO DA URL -----
                // 1. Garante que a URL base do servidor termine com /
                if (!serverUrl.endsWith("/")) {
                    serverUrl += "/";
                }

                // 2. Constrói a URL base + nome do banco
                String baseUrlWithDb = serverUrl + dbName; // Ex: jdbc:mysql://localhost:3306/rfvendas

                // 3. Adiciona parâmetros à URL já com o nome do banco
                String urlParams = "";
                String separator = "?"; // Começa com ?
                if (!baseUrlWithDb.matches(".*[?&]useSSL=.*")) { urlParams += separator + "useSSL=false"; separator = "&"; }
                if (!baseUrlWithDb.matches(".*[?&]serverTimezone=.*")) { urlParams += separator + "serverTimezone=UTC"; separator = "&"; }
                if (!baseUrlWithDb.matches(".*[?&]allowPublicKeyRetrieval=.*")) { urlParams += separator + "allowPublicKeyRetrieval=true"; separator = "&"; }
                if (!baseUrlWithDb.matches(".*[?&]createDatabaseIfNotExist=.*")) { urlParams += separator + "createDatabaseIfNotExist=true"; /*separator = "&";*/ } // Já existia no driver

                // 4. Define a URL final
                databaseSpecificUrl = baseUrlWithDb + urlParams;
                // ----- FIM DA CORREÇÃO -----

                props.setProperty("db.url", databaseSpecificUrl); // Guarda a URL completa para referência
                System.out.println("[DB Setup] URL base do servidor configurada: " + serverUrl); // Log da URL base corrigida
                System.out.println("[DB Setup] Nome do banco de dados: " + dbName);
                System.out.println("[DB Setup] URL completa do banco definida: " + databaseSpecificUrl.split("\\?")[0] + "..."); // Log sem parâmetros
            }
        } catch (IOException e) {
            System.err.println("[DB Setup] Erro de IO ao ler o arquivo '" + CONFIG_FILE + "'. Verifique permissões e formato.");
            setDefaultPropertiesOnError();
            throw e;
        }
    }

    /** Define propriedades padrão mínimas em caso de falha ao carregar o arquivo config. */
    private static void setDefaultPropertiesOnError() {
        // Implementação do setDefaultPropertiesOnError (igual à versão anterior completa)
        System.err.println("[DB Setup] ATENÇÃO: Usando propriedades padrão de fallback!");
        props.setProperty("db.server.url", "jdbc:mysql://localhost:3306/");
        props.setProperty("db.name", "rfvendas");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", ""); // Senha vazia como fallback
        databaseSpecificUrl = "jdbc:mysql://localhost:3306/rfvendas?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true";
        props.setProperty("db.url", databaseSpecificUrl);
    }

    /**
     * Garante que o banco de dados e todas as tabelas necessárias existam.
     * @throws SQLException Se ocorrer um erro de SQL durante a criação.
     */
    private static void initializeDatabaseSchema() throws SQLException {
        // Implementação do initializeDatabaseSchema (igual à versão anterior completa)
        String serverUrl = props.getProperty("db.server.url");
        String dbName = props.getProperty("db.name");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        System.out.println("[DB Setup] Tentando conectar ao servidor MySQL: " + serverUrl.split("\\?")[0] + "...");
        try (Connection serverConn = DriverManager.getConnection(serverUrl, user, password);
             Statement stmt = serverConn.createStatement()) {
            System.out.println("[DB Setup] Conectado ao servidor. Verificando/Criando banco de dados `" + dbName + "`...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            System.out.println("[DB Setup] Banco de dados `" + dbName + "` verificado/criado.");
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO ao conectar ao servidor ou criar banco de dados!");
            throw e;
        }

        System.out.println("[DB Setup] Tentando conectar ao banco específico: " + databaseSpecificUrl.split("\\?")[0] + "...");
        try (Connection dbConn = DriverManager.getConnection(databaseSpecificUrl, user, password)) {
            System.out.println("[DB Setup] Conectado ao banco `" + dbName + "`. Verificando/Criando tabelas...");
            createTablesIfNotExists(dbConn);
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO ao conectar ao banco `" + dbName + "` ou criar tabelas!");
            throw e;
        }
    }

    /**
     * Executa os comandos CREATE TABLE IF NOT EXISTS para as tabelas da aplicação.
     * @param conn Uma conexão ativa com o banco de dados específico (`rfvendas`).
     * @throws SQLException Se ocorrer um erro ao executar algum comando DDL.
     */
    private static void createTablesIfNotExists(Connection conn) throws SQLException {
        // Lista dos comandos CREATE TABLE (igual à versão anterior completa)
        List<String> createTableStatements = List.of(
                """
                CREATE TABLE IF NOT EXISTS categorias (
                    id INT NOT NULL AUTO_INCREMENT, nome VARCHAR(255) NOT NULL, descricao TEXT NULL, PRIMARY KEY (id), UNIQUE KEY uk_categorias_nome (nome)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS grupos (
                    id INT NOT NULL AUTO_INCREMENT, nome VARCHAR(255) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_grupos_nome (nome)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INT NOT NULL AUTO_INCREMENT, username VARCHAR(50) NOT NULL, password VARCHAR(255) NOT NULL, nome VARCHAR(100) NOT NULL, email VARCHAR(100) NOT NULL, nivel_acesso INT NOT NULL, ativo TINYINT(1) NULL DEFAULT 1, created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id), UNIQUE KEY uk_usuarios_username (username), UNIQUE KEY uk_usuarios_email (email)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS clientes (
                    id VARCHAR(255) NOT NULL, nome VARCHAR(255) NOT NULL, cpf_cnpj VARCHAR(14) NOT NULL, logradouro VARCHAR(255) NOT NULL, bairro VARCHAR(255) NOT NULL, cidade VARCHAR(255) NOT NULL, numero VARCHAR(20) NOT NULL, complemento VARCHAR(255) NULL, telefone_celular VARCHAR(20) NOT NULL, email VARCHAR(255) NOT NULL, comprador VARCHAR(255) NULL, tipo_cliente ENUM('PESSOA_FISICA','PESSOA_JURIDICA') NOT NULL, id_grupo INT NULL, PRIMARY KEY (id), UNIQUE KEY uk_clientes_cpf_cnpj (cpf_cnpj), INDEX idx_clientes_nome (nome), CONSTRAINT fk_cliente_grupo FOREIGN KEY (id_grupo) REFERENCES grupos(id) ON DELETE SET NULL ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS itens (
                    id INT NOT NULL, nome VARCHAR(255) NOT NULL, descricao TEXT NULL, preco_venda DECIMAL(10,2) NOT NULL, preco_custo DECIMAL(10,2) NOT NULL, unidade_medida VARCHAR(50) NOT NULL, quantidade_estoque DECIMAL(10,5) NOT NULL, quantidade_minima DECIMAL(10,5) NOT NULL, quantidade_atual DECIMAL(10,5) NOT NULL DEFAULT 0.00000, categoria_id INT NULL, imagem_path VARCHAR(512) NULL, PRIMARY KEY (id), UNIQUE KEY uk_itens_nome (nome), CONSTRAINT fk_item_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS dependencias (
                    id INT NOT NULL AUTO_INCREMENT, id_item_dependente INT NOT NULL, id_item_necessario INT NOT NULL, id_categoria INT NOT NULL, quantidade DECIMAL(10,2) NOT NULL, PRIMARY KEY (id), CONSTRAINT fk_dependencia_item_dep FOREIGN KEY (id_item_dependente) REFERENCES itens(id) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT fk_dependencia_item_nec FOREIGN KEY (id_item_necessario) REFERENCES itens(id) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT fk_dependencia_categoria FOREIGN KEY (id_categoria) REFERENCES categorias(id) ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS pedidos (
                    id INT NOT NULL AUTO_INCREMENT, cliente_id VARCHAR(255) NOT NULL, data_pedido DATE NOT NULL, data_retorno DATE NULL, valor_total DECIMAL(10,2) NOT NULL DEFAULT 0.00, status ENUM('CONCLUIDO','EM_ANDAMENTO','CANCELADO') NOT NULL DEFAULT 'EM_ANDAMENTO', observacoes TEXT NULL, PRIMARY KEY (id), CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,
                """
                CREATE TABLE IF NOT EXISTS itens_pedido (
                    id INT NOT NULL AUTO_INCREMENT, pedido_id INT NOT NULL, item_id INT NOT NULL, quantidade DECIMAL(10,2) NOT NULL, preco_venda DECIMAL(10,2) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_pedido_item (pedido_id, item_id), CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT fk_item_pedido_item FOREIGN KEY (item_id) REFERENCES itens(id) ON DELETE RESTRICT ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """
        );
        try (Statement stmt = conn.createStatement()) {
            int tableCount = 0;
            for (String sql : createTableStatements) {
                tableCount++;
                System.out.println("[DB Setup] Executando SQL Tabela " + tableCount + ": " + sql.substring(0, Math.min(sql.length(), 100)).split("\n")[0].trim() + "...");
                stmt.executeUpdate(sql);
            }
            System.out.println("[DB Setup] Todas as " + tableCount + " tabelas verificadas/criadas.");
        }
    }

    /**
     * Verifica se o usuário administrador padrão existe. Se não, o insere com senha padrão hashada.
     * @throws SQLException Se ocorrer erro ao interagir com a tabela 'usuarios'.
     */
    private static void ensureDefaultAdminUserExists() throws SQLException {
        // Implementação do ensureDefaultAdminUserExists (igual à versão anterior completa)
        System.out.println("[DB Setup] Verificando/Criando usuário admin padrão ('" + DEFAULT_ADMIN_USERNAME + "')...");
        String sqlCheck = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        String sqlInsert = "INSERT INTO usuarios (username, password, nome, email, nivel_acesso, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(databaseSpecificUrl, props.getProperty("db.user"), props.getProperty("db.password"));
             PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {
            pstmtCheck.setString(1, DEFAULT_ADMIN_USERNAME);
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("[DB Setup] Usuário admin ('" + DEFAULT_ADMIN_USERNAME + "') já existe.");
                    return;
                }
            }
            System.out.println("[DB Setup] Usuário admin ('" + DEFAULT_ADMIN_USERNAME + "') não encontrado. Criando...");
            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                pstmtInsert.setString(1, DEFAULT_ADMIN_USERNAME);
                pstmtInsert.setString(2, "1234");
                pstmtInsert.setString(3, "Administrador Padrão");
                pstmtInsert.setString(4, "admin@localhost");
                pstmtInsert.setInt(5, 1); // Nível Admin
                pstmtInsert.setBoolean(6, true);
                int rowsAffected = pstmtInsert.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("[DB Setup] Usuário admin criado com sucesso.");
                    System.out.println(" /!\\ ==================================================================== /!\\");
                    System.out.println(" /!\\ SEGURANÇA: Senha padrão '" + DEFAULT_ADMIN_PASSWORD_PLAINTEXT + "' definida para '" + DEFAULT_ADMIN_USERNAME + "'. Troque-a! /!\\");
                    System.out.println(" /!\\ ==================================================================== /!\\");
                } else {
                    System.err.println("[DB Setup] ERRO INESPERADO: Falha ao inserir usuário admin.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO SQL ao verificar/criar usuário admin!");
            throw e;
        }
    }

    /**
     * Obtém uma conexão com o banco de dados configurado e inicializado.
     * @return Uma conexão SQL ativa.
     * @throws SQLException Se a inicialização falhou ou se não for possível conectar agora.
     */
    public static Connection getConnection() throws SQLException {
        // Implementação do getConnection (igual à versão anterior completa)
        if (!initializationOk) {
            throw new SQLException("A inicialização do banco de dados falhou. Verifique os logs.");
        }
        try {
            return DriverManager.getConnection(
                    databaseSpecificUrl,
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
        } catch (SQLException e) {
            System.err.println("[DB Connection] ERRO SQL ao obter nova conexão.");
            e.printStackTrace();
            throw e;
        }
    }

    // --- Métodos Utilitários de Fechamento (Padrão - sem alterações) ---
    /** Fecha a conexão SQL. */
    public static void closeConnection(Connection conn) {
        // Implementação do closeConnection(conn) (igual à versão anterior completa)
        if (conn != null) { try { if (!conn.isClosed()) { conn.close(); } } catch (SQLException e) { System.err.println("Erro (ignorado) ao fechar Conexão: " + e.getMessage()); } }
    }

    /** Fecha o PreparedStatement e a conexão SQL. */
    public static void closeConnection(Connection conn, PreparedStatement stmt) {
        // Implementação do closeConnection(conn, stmt) (igual à versão anterior completa)
        if (stmt != null) { try { if (!stmt.isClosed()) { stmt.close(); } } catch (SQLException e) { System.err.println("Erro (ignorado) ao fechar Statement: " + e.getMessage()); } }
        closeConnection(conn);
    }

    /** Fecha o ResultSet, o PreparedStatement e a conexão SQL. */
    public static void closeConnection(Connection conn, PreparedStatement stmt, ResultSet rs) {
        // Implementação do closeConnection(conn, stmt, rs) (igual à versão anterior completa)
        if (rs != null) { try { if (!rs.isClosed()) { rs.close(); } } catch (SQLException e) { System.err.println("Erro (ignorado) ao fechar ResultSet: " + e.getMessage()); } }
        closeConnection(conn, stmt);
    }

    // --- MÉTODOS DE BACKUP/RESTORE - Mantidos EXATAMENTE como no seu código original ---

    public static void exportarBancoDeDados(String nomeArquivo) {
        Connection conn = null; // Declara fora para o finally
        try {
            // conn = getConnection(); // Conexão não é estritamente necessária aqui, mas pode pegar props

            String user = props.getProperty("db.user");
            String senha = props.getProperty("db.password");
            String nomeBanco = props.getProperty("db.name");

            // Cria o diretório de backup se não existir (pode falhar por permissão)
            File backupDir = new File("backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs(); // Não verifica sucesso da criação do diretório
            }

            // Caminho HARDCODED do seu ambiente original
            String caminhoCompleto = "C:\\Users\\rafa_\\OneDrive\\Documentos\\OneDrive\\Flux\\" + nomeArquivo;

            // Caminho HARDCODED do seu ambiente original
            String[] comando = {
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                    "-u", user,
                    "-p" + senha, // Passar senha assim é um risco
                    nomeBanco
            };

            ProcessBuilder processBuilder = new ProcessBuilder(comando);
            // Redireciona a SAÍDA do mysqldump para o arquivo especificado
            processBuilder.redirectOutput(new File(caminhoCompleto));

            Process processo = processBuilder.start();

            // É importante ler o stream de erro para evitar que o processo bloqueie
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(processo.getErrorStream()));
            String linhaErro;
            StringBuilder erros = new StringBuilder();
            while ((linhaErro = errorReader.readLine()) != null) {
                erros.append(linhaErro).append("\n");
            }
            errorReader.close(); // Fechar o reader

            int resultadoProcesso = processo.waitFor(); // Espera o processo terminar


            if (resultadoProcesso == 0) {
                System.out.println("Backup realizado com sucesso em: " + caminhoCompleto);
            } else {
                System.err.println("Erro durante o backup (código de saída: " + resultadoProcesso + "):");
                System.err.print(erros.toString()); // Imprime os erros capturados do stderr
            }
        } catch (Exception e) { // Captura IOException, InterruptedException, etc.
            System.err.println("Erro GERAL ao realizar backup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // A conexão não foi aberta neste método, então não precisa fechar aqui
            // closeConnection(conn);
        }
    }

    public static void importarBancoDeDados(String nomeArquivo) {
        Connection conn = null; // Declara fora para o finally
        try {
            // conn = getConnection(); // Conexão não é necessária para importar via mysql.exe

            String user = props.getProperty("db.user");
            String senha = props.getProperty("db.password");
            String nomeBanco = props.getProperty("db.name");
            // Caminho HARDCODED do seu ambiente original
            String caminhoCompleto = "C:\\Users\\rafa_\\OneDrive\\Documentos\\OneDrive\\Flux\\" + nomeArquivo;

            // Caminho HARDCODED do seu ambiente original
            String[] comando = {
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe", // caminho completo do mysql
                    "-u", user,
                    "-p" + senha, // Risco de segurança
                    nomeBanco
            };

            ProcessBuilder processBuilder = new ProcessBuilder(comando);
            // Redireciona o CONTEÚDO do arquivo SQL para a ENTRADA do mysql.exe
            processBuilder.redirectInput(new File(caminhoCompleto));

            Process processo = processBuilder.start();

            // É importante ler o stream de erro
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(processo.getErrorStream()));
            String linhaErro;
            StringBuilder erros = new StringBuilder();
            while ((linhaErro = errorReader.readLine()) != null) {
                erros.append(linhaErro).append("\n");
            }
            errorReader.close();

            // Também pode ser útil ler o stream de saída normal (stdout)
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(processo.getInputStream()));
            String linhaOut;
            while ((linhaOut = outputReader.readLine()) != null) {
                System.out.println("mysql stdout: " + linhaOut); // Log da saída normal
            }
            outputReader.close();


            int resultadoProcesso = processo.waitFor();

            if (resultadoProcesso == 0) {
                System.out.println("Importação realizada com sucesso de: " + caminhoCompleto);
            } else {
                System.err.println("Erro durante a importação (código de saída: " + resultadoProcesso + "):");
                System.err.print(erros.toString()); // Imprime os erros capturados do stderr
            }
        } catch (Exception e) { // Captura IOException, InterruptedException, etc.
            System.err.println("Erro GERAL ao importar backup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // A conexão não foi aberta neste método
            // closeConnection(conn);
        }
    }
}