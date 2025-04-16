package BackEnd.exception; // Pacote para exceções customizadas

/**
 * Exceção customizada para representar erros específicos ocorridos durante
 * as operações de backup (exportação) ou restauração (importação) do banco de dados.
 *
 * Permite um tratamento de erro mais direcionado no código que chama
 * os métodos de backup/restore da ConnectionFactory.
 */
public class BackupRestoreException extends Exception {

    /**
     * Construtor que aceita apenas uma mensagem de erro.
     *
     * @param message A mensagem detalhando o erro ocorrido.
     */
    public BackupRestoreException(String message) {
        super(message);
    }

    /**
     * Construtor que aceita uma mensagem de erro e a causa original (exceção encapsulada).
     * Útil para propagar a informação da exceção original (e.g., IOException, InterruptedException)
     * que causou a falha no backup/restore.
     *
     * @param message A mensagem detalhando o erro ocorrido.
     * @param cause   A exceção original que causou esta exceção.
     */
    public BackupRestoreException(String message, Throwable cause) {
        super(message, cause);
    }
}