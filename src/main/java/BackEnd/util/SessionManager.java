package BackEnd.util;

import BackEnd.model.entity.Usuario;

public class SessionManager {
    private static Usuario usuarioLogado;
    private static boolean loggedIn = false;

    public static void setUsuarioLogado(Usuario usuario) {
        usuarioLogado = usuario;
        loggedIn = (usuario != null);
    }

    public static Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    public static void limparSessao() {
        usuarioLogado = null;
        loggedIn = false;
    }

    public static boolean temPermissao(int nivelPermissaoRequerido) {
        if (!loggedIn || usuarioLogado == null) {
            return false;
        }
        return usuarioLogado.getNivelAcesso() <= nivelPermissaoRequerido;
    }
}