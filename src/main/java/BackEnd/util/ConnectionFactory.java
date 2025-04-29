package BackEnd.util;

import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.Properties;

// Importe a exceção customizada que deve estar em BackEnd.exception.BackupRestoreException
import BackEnd.exception.BackupRestoreException;

public class ConnectionFactory {

    // --- Configuração ---
    private static final String CONFIG_FILE = "config.properties"; // Nome do arquivo em src/main/resources
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD_PLAINTEXT = "1234"; // SENHA EM TEXTO CLARO - INSEGURO!
    // -----------------------------------------------------------------------

    public static Properties props = new Properties();
    private static String databaseSpecificUrl;
    private static String mysqlDumpPath; // Caminho para o mysqldump lido do config
    private static String mysqlCliPath;  // Caminho para o mysql lido do config
    private static boolean initializationOk = false;

    static {
        System.out.println("[DB Setup] Iniciando configuração completa do banco de dados...");
        try {
            loadProperties();
            initializeDatabaseSchema();
            ensureDefaultAdminUserExists();
            initializationOk = true;
            System.out.println("[DB Setup] Configuração do banco de dados e usuário admin concluída com sucesso.");

        } catch (Exception e) {
            initializationOk = false;
            System.err.println("*****************************************************");
            System.err.println("ERRO FATAL DURANTE A INICIALIZAÇÃO DO BANCO DE DADOS, USUÁRIO ADMIN OU CONFIGURAÇÃO!");
            System.err.println("A aplicação pode não funcionar corretamente.");
            System.err.println("Causa: " + e.getMessage());
            e.printStackTrace();
            System.err.println("*****************************************************");
            // throw new RuntimeException("Falha crítica na inicialização do banco de dados.", e);
        }
    }

    private static void loadProperties() throws IOException {
        try (InputStream input = ConnectionFactory.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                System.err.println("[DB Setup] ERRO CRÍTICO: Arquivo '" + CONFIG_FILE + "' não encontrado no classpath.");
                setDefaultPropertiesOnError();
                throw new IOException("Arquivo de configuração '" + CONFIG_FILE + "' não encontrado no classpath.");
            }

            props.load(input);
            System.out.println("[DB Setup] Arquivo de configuração '" + CONFIG_FILE + "' carregado.");

            // Validação de Propriedades Essenciais de Conexão
            String serverUrl = props.getProperty("db.server.url");
            String dbName = props.getProperty("db.name");
            String user = props.getProperty("db.user");

            if (isNullOrEmpty(serverUrl) || isNullOrEmpty(dbName) || isNullOrEmpty(user)) {
                throw new IOException("Propriedades essenciais ('db.server.url', 'db.name', 'db.user') estão vazias ou não encontradas no " + CONFIG_FILE);
            }
            serverUrl = serverUrl.trim();
            dbName = dbName.trim();

            // Construção da URL do Banco
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            String baseUrlWithDb = serverUrl + dbName;
            String urlParams = buildUrlParameters(baseUrlWithDb);
            databaseSpecificUrl = baseUrlWithDb + urlParams;
            props.setProperty("db.url", databaseSpecificUrl);

            System.out.println("[DB Setup] URL base do servidor: " + serverUrl);
            System.out.println("[DB Setup] Nome do banco: " + dbName);
            System.out.println("[DB Setup] URL completa: " + databaseSpecificUrl.split("\\?")[0] + "...");

            // Carregar e Validar Caminhos dos Executáveis MySQL
            mysqlDumpPath = props.getProperty("mysql.dump.path");
            mysqlCliPath = props.getProperty("mysql.cli.path");

            if (isNullOrEmpty(mysqlDumpPath)) {
                throw new IOException("Propriedade 'mysql.dump.path' não encontrada ou vazia no " + CONFIG_FILE);
            }
            if (isNullOrEmpty(mysqlCliPath)) {
                throw new IOException("Propriedade 'mysql.cli.path' não encontrada ou vazia no " + CONFIG_FILE);
            }
            mysqlDumpPath = mysqlDumpPath.trim();
            mysqlCliPath = mysqlCliPath.trim();

            File dumpFile = new File(mysqlDumpPath);
            File cliFile = new File(mysqlCliPath);
            if (!dumpFile.exists() || !dumpFile.isFile()) {
                System.err.println("[DB Setup] AVISO: 'mysql.dump.path' não encontrado ou não é arquivo: " + mysqlDumpPath);
            }
            if (!cliFile.exists() || !cliFile.isFile()) {
                System.err.println("[DB Setup] AVISO: 'mysql.cli.path' não encontrado ou não é arquivo: " + mysqlCliPath);
            }

            System.out.println("[DB Setup] Caminho mysqldump: " + mysqlDumpPath);
            System.out.println("[DB Setup] Caminho mysql CLI: " + mysqlCliPath);

        } catch (IOException e) {
            System.err.println("[DB Setup] Erro de IO ao ler ou validar '" + CONFIG_FILE + "'.");
            setDefaultPropertiesOnError();
            throw e;
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static String buildUrlParameters(String baseUrl) {
        String urlParams = "";
        String separator = baseUrl.contains("?") ? "&" : "?";
        if (!baseUrl.matches(".*[?&]useSSL=.*")) { urlParams += separator + "useSSL=false"; separator = "&"; }
        if (!baseUrl.matches(".*[?&]serverTimezone=.*")) { urlParams += separator + "serverTimezone=UTC"; separator = "&"; }
        if (!baseUrl.matches(".*[?&]allowPublicKeyRetrieval=.*")) { urlParams += separator + "allowPublicKeyRetrieval=true"; separator = "&"; }
        if (!baseUrl.matches(".*[?&]createDatabaseIfNotExist=.*")) { urlParams += separator + "createDatabaseIfNotExist=true"; }
        return urlParams;
    }

    private static void setDefaultPropertiesOnError() {
        System.err.println("[DB Setup] ATENÇÃO: Usando propriedades de fallback para conexão!");
        props.setProperty("db.server.url", "jdbc:mysql://localhost:3306/");
        props.setProperty("db.name", "rfvendas");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", "");
        databaseSpecificUrl = "jdbc:mysql://localhost:3306/rfvendas?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true";
        props.setProperty("db.url", databaseSpecificUrl);
        System.err.println("[DB Setup] ATENÇÃO: Caminhos para 'mysqldump' e 'mysql' não configurados (fallback). Backup/Restore falharão.");
        mysqlDumpPath = null;
        mysqlCliPath = null;
    }

    private static void initializeDatabaseSchema() throws SQLException {
        // Código igual à versão anterior (criação do banco)
        String serverUrl = props.getProperty("db.server.url");
        String dbName = props.getProperty("db.name");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        System.out.println("[DB Setup] Conectando ao servidor: " + serverUrl.split("\\?")[0] + "...");
        try (Connection serverConn = DriverManager.getConnection(serverUrl, user, password);
             Statement stmt = serverConn.createStatement()) {
            System.out.println("[DB Setup] Verificando/Criando banco `" + dbName + "`...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            System.out.println("[DB Setup] Banco `" + dbName + "` OK.");
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO ao conectar ao servidor ou criar banco!");
            throw e;
        }

        System.out.println("[DB Setup] Conectando ao banco específico: " + databaseSpecificUrl.split("\\?")[0] + "...");
        try (Connection dbConn = DriverManager.getConnection(databaseSpecificUrl, user, password)) {
            System.out.println("[DB Setup] Verificando/Criando tabelas...");
            createTablesIfNotExists(dbConn);
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO ao conectar ao banco específico ou criar tabelas!");
            throw e;
        }
    }

    private static void createTablesIfNotExists(Connection conn) throws SQLException {
        // Código igual à versão anterior (lista de CREATE TABLEs)
        List<String> createTableStatements = List.of(
                // ... (todos os seus CREATE TABLE statements aqui, omitidos para brevidade) ...
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
                    id INT NOT NULL AUTO_INCREMENT, cliente_id VARCHAR(255) NOT NULL, data_pedido DATE NOT NULL, data_retorno DATE NULL, valor_total DECIMAL(10,2) NOT NULL DEFAULT 0.00, status ENUM('CONCLUIDO','EM_ANDAMENTO','CANCELADO') NOT NULL DEFAULT 'EM_ANDAMENTO', observacoes TEXT NULL, tipo_pagamento VARCHAR(50) NULL DEFAULT NULL, PRIMARY KEY (id), CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT ON UPDATE CASCADE
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
                System.out.println("[DB Setup] Executando SQL Tabela " + tableCount + "...");
                stmt.executeUpdate(sql);
            }
            System.out.println("[DB Setup] Todas as " + tableCount + " tabelas OK.");
        }
    }

    private static void ensureDefaultAdminUserExists() throws SQLException {
        // Código igual à versão anterior (criação do admin com senha insegura)
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

            System.out.println("[DB Setup] Usuário admin não encontrado. Criando...");
            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                pstmtInsert.setString(1, DEFAULT_ADMIN_USERNAME);
                // !!! FALHA DE SEGURANÇA !!!
                pstmtInsert.setString(2, DEFAULT_ADMIN_PASSWORD_PLAINTEXT);
                pstmtInsert.setString(3, "Administrador Padrão");
                pstmtInsert.setString(4, "admin@localhost");
                pstmtInsert.setInt(5, 1); // Nível Admin
                pstmtInsert.setBoolean(6, true);
                int rowsAffected = pstmtInsert.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("[DB Setup] Usuário admin criado com sucesso.");
                    System.out.println(" /!\\ SEGURANÇA: Senha padrão '" + DEFAULT_ADMIN_PASSWORD_PLAINTEXT + "' definida para '" + DEFAULT_ADMIN_USERNAME + "'. Troque-a! /!\\");
                } else {
                    System.err.println("[DB Setup] ERRO INESPERADO: Falha ao inserir usuário admin.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Setup] ERRO SQL ao verificar/criar usuário admin!");
            throw e;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initializationOk) {
            throw new SQLException("Inicialização da ConnectionFactory falhou. Não é possível obter conexão.");
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

    // --- Métodos Utilitários de Fechamento ---
    public static void closeConnection(Connection conn) {
        if (conn != null) { try { if (!conn.isClosed()) { conn.close(); } } catch (SQLException e) { /* Ignorado */ } }
    }
    public static void closeConnection(Connection conn, PreparedStatement stmt) {
        if (stmt != null) { try { if (!stmt.isClosed()) { stmt.close(); } } catch (SQLException e) { /* Ignorado */ } }
        closeConnection(conn);
    }
    public static void closeConnection(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) { try { if (!rs.isClosed()) { rs.close(); } } catch (SQLException e) { /* Ignorado */ } }
        closeConnection(conn, stmt);
    }

    // --- MÉTODOS DE BACKUP/RESTORE - COM THROWS BackupRestoreException ---

    /**
     * Exporta o banco de dados usando mysqldump.
     * @param targetFilePath Caminho completo do arquivo de destino.
     * @throws IOException Erro de I/O.
     * @throws InterruptedException Thread interrompida.
     * @throws BackupRestoreException Erro específico de backup/restore (caminho não configurado, processo falhou).
     */
    public static void exportarBancoDeDados(String targetFilePath)
            throws IOException, InterruptedException, BackupRestoreException { // <<< DECLARAÇÃO IMPORTANTE

        if (isNullOrEmpty(mysqlDumpPath)) {
            throw new BackupRestoreException("Caminho para 'mysqldump' não configurado ('mysql.dump.path').");
        }
        if (isNullOrEmpty(targetFilePath)) {
            throw new IllegalArgumentException("Caminho do arquivo de destino não pode ser nulo/vazio.");
        }

        String user = props.getProperty("db.user");
        String senha = props.getProperty("db.password", "");
        String nomeBanco = props.getProperty("db.name");
        File targetFile = new File(targetFilePath);

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("[Backup] AVISO: Falha ao criar diretório: " + parentDir.getAbsolutePath());
            }
        }

        String[] comando = {
                mysqlDumpPath, "-u", user, "-p" + senha, "--host=localhost", "--port=3306",
                "--protocol=tcp", "--single-transaction", "--routines", "--triggers",
                "--events", "--databases", nomeBanco
        };

        System.out.println("[Backup] Executando exportação para: " + targetFilePath);
        ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.redirectOutput(targetFile);
        processBuilder.redirectErrorStream(false); // Capturar erro separadamente

        Process processo = processBuilder.start();
        StringBuilder erros = readStream(processo.getErrorStream()); // Lê stderr
        int resultadoProcesso = processo.waitFor();

        if (resultadoProcesso != 0) {
            String errorMsg = "Erro no backup (mysqldump código: " + resultadoProcesso + ").";
            if (erros.length() > 0) { errorMsg += " Erros:\n" + erros.toString(); }
            System.err.println(errorMsg);
            throw new BackupRestoreException(errorMsg); // <<< LANÇANDO A EXCEÇÃO
        } else if (!targetFile.exists() || targetFile.length() == 0) {
            String warnMsg = "Backup finalizou (código 0), mas arquivo está vazio/ausente: " + targetFilePath;
            System.err.println("[Backup] AVISO: " + warnMsg);
            // Considerar lançar exceção se isso for um erro crítico para você
            // throw new BackupRestoreException(warnMsg);
        } else {
            System.out.println("[Backup] Backup realizado com sucesso: " + targetFilePath);
        }
    }

    /**
     * Importa um banco de dados a partir de um arquivo .sql.
     * @param sourceFilePath Caminho completo do arquivo de origem.
     * @throws IOException Erro de I/O.
     * @throws InterruptedException Thread interrompida.
     * @throws BackupRestoreException Erro específico de backup/restore (caminho não configurado, arquivo não encontrado, processo falhou).
     */
    public static void importarBancoDeDados(String sourceFilePath)
            throws IOException, InterruptedException, BackupRestoreException { // <<< DECLARAÇÃO IMPORTANTE

        if (isNullOrEmpty(mysqlCliPath)) {
            throw new BackupRestoreException("Caminho para 'mysql' não configurado ('mysql.cli.path').");
        }
        if (isNullOrEmpty(sourceFilePath)) {
            throw new IllegalArgumentException("Caminho do arquivo de origem não pode ser nulo/vazio.");
        }

        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new BackupRestoreException("Arquivo de origem não encontrado/inválido: " + sourceFilePath);
        }
        if (sourceFile.length() == 0) {
            throw new BackupRestoreException("Arquivo de origem está vazio: " + sourceFilePath);
        }

        String user = props.getProperty("db.user");
        String senha = props.getProperty("db.password", "");
        String nomeBanco = props.getProperty("db.name");

        String[] comando = {
                mysqlCliPath, "-u", user, "-p" + senha, "--host=localhost", "--port=3306",
                "--protocol=tcp", nomeBanco
        };

        System.out.println("[Restore] Executando importação de: " + sourceFilePath);
        ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.redirectInput(sourceFile);
        processBuilder.redirectErrorStream(false);

        Process processo = processBuilder.start();
        StringBuilder erros = readStream(processo.getErrorStream()); // Lê stderr
        StringBuilder output = readStream(processo.getInputStream()); // Lê stdout
        int resultadoProcesso = processo.waitFor();

        if (resultadoProcesso != 0) {
            String errorMsg = "Erro na importação (mysql código: " + resultadoProcesso + ") do arquivo " + sourceFilePath + ".";
            if (erros.length() > 0) { errorMsg += " Erros (stderr):\n" + erros.toString(); }
            if (output.length() > 0) { errorMsg += " Saída (stdout):\n" + output.toString(); }
            System.err.println(errorMsg);
            throw new BackupRestoreException(errorMsg); // <<< LANÇANDO A EXCEÇÃO
        } else {
            System.out.println("[Restore] Importação realizada com sucesso de: " + sourceFilePath);
            if (output.length() > 0) { System.out.println("[Restore] Saída (stdout):\n" + output); }
            if (erros.length() > 0) { System.err.println("[Restore] AVISO: Importação OK (código 0), mas houve saída em stderr (warnings?):\n" + erros); }
        }
    }

    // Método auxiliar para ler um InputStream e retornar como String
    private static StringBuilder readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb;
    }

    /*
     * // Definição da Exceção Customizada (Removida daqui)
     * // Certifique-se que esta classe existe em: BackEnd.exception.BackupRestoreException.java
     * public static class BackupRestoreException extends Exception {
     *     public BackupRestoreException(String message) { super(message); }
     *     public BackupRestoreException(String message, Throwable cause) { super(message, cause); }
     * }
     */
}