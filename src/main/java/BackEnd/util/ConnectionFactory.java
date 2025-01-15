package BackEnd.util;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionFactory {
    private static final String CONFIG_FILE = "config.properties";
    public static Properties props = new Properties();

    static {
        try {
            props.load(new FileInputStream(CONFIG_FILE));
        } catch (IOException e) {
            props.setProperty("db.url", "jdbc:mysql://localhost:3306/Flux");
            props.setProperty("db.user", "root");
            props.setProperty("db.password", "root");
            props.setProperty("db.name", "Flux");
            props.setProperty("db.backup.path", "backup/");
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }

    public static void closeConnection(Connection conn, PreparedStatement stmt) {
        closeConnection(conn);
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar statement: " + e.getMessage());
        }
    }

    public static void closeConnection(Connection conn, PreparedStatement stmt, ResultSet rs) {
        closeConnection(conn, stmt);
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar resultset: " + e.getMessage());
        }
    }

    public static void exportarBancoDeDados(String nomeArquivo) {
        Connection conn = null;
        try {
            conn = getConnection();

            String user = props.getProperty("db.user");
            String senha = props.getProperty("db.password");
            String nomeBanco = props.getProperty("db.name");

            // Cria o diretório de backup se não existir
            File backupDir = new File("backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String caminhoCompleto = "C:\\Users\\rafa_\\OneDrive\\Documentos\\OneDrive\\Flux\\" + nomeArquivo;

            String[] comando = {
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                    "-u", user,
                    "-p" + senha,
                    nomeBanco
            };

            ProcessBuilder processBuilder = new ProcessBuilder(comando);
            processBuilder.redirectOutput(new File(caminhoCompleto));

            Process processo = processBuilder.start();
            int resultadoProcesso = processo.waitFor();


            if (resultadoProcesso == 0) {
                System.out.println("Backup realizado com sucesso em: " + caminhoCompleto);
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(processo.getErrorStream()));
                String linha;
                System.err.println("Erro durante o backup:");
                while ((linha = errorReader.readLine()) != null) {
                    System.err.println(linha);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao realizar backup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

            public static void importarBancoDeDados(String nomeArquivo) {
        Connection conn = null;
        try {
            conn = getConnection();

            String user = props.getProperty("db.user");
            String senha = props.getProperty("db.password");
            String nomeBanco = props.getProperty("db.name");
            String caminhoCompleto = "C:\\Users\\rafa_\\OneDrive\\Documentos\\OneDrive\\Flux\\" + nomeArquivo;

            String[] comando = {
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe", // caminho completo do mysql
                    "-u", user,
                    "-p" + senha,
                    nomeBanco
            };

            ProcessBuilder processBuilder = new ProcessBuilder(comando);
            processBuilder.redirectInput(new File(caminhoCompleto));

            Process processo = processBuilder.start();
            int resultadoProcesso = processo.waitFor();

            if (resultadoProcesso == 0) {
                System.out.println("Importação realizada com sucesso de: " + caminhoCompleto);
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(processo.getErrorStream()));
                String linha;
                System.err.println("Erro durante a importação:");
                while ((linha = errorReader.readLine()) != null) {
                    System.err.println(linha);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao importar backup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }
}