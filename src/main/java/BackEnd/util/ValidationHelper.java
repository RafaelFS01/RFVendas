package BackEnd.util;

import java.util.regex.Pattern;

public class ValidationHelper {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidCPF(String cpf) {
        if (cpf == null) {
            return false;
        }
        cpf = cpf.replaceAll("[^0-9]", "");
        if (cpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (ex: 11111111111), o que é inválido
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) {
            primeiroDigito = 0;
        }

        // Verifica o primeiro dígito verificador
        if (primeiroDigito != (cpf.charAt(9) - '0')) {
            return false;
        }

        // Calcula o segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) {
            segundoDigito = 0;
        }

        // Verifica o segundo dígito verificador
        return segundoDigito == (cpf.charAt(10) - '0');
    }

    public static boolean isValidCNPJ(String cnpj) {
        if (cnpj == null) {
            return false;
        }

        cnpj = cnpj.replaceAll("[^0-9]", "");
        if (cnpj.length() != 14) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (ex: 11111111111111), o que é inválido
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int soma = 0;
        int peso = 2;
        for (int i = 11; i >= 0; i--) {
            soma += (cnpj.charAt(i) - '0') * peso;
            peso = (peso == 9) ? 2 : peso + 1;
        }
        int primeiroDigito = (soma % 11) < 2 ? 0 : 11 - (soma % 11);

        // Verifica o primeiro dígito verificador
        if (primeiroDigito != (cnpj.charAt(12) - '0')) {
            return false;
        }

        // Calcula o segundo dígito verificador
        soma = 0;
        peso = 2;
        for (int i = 12; i >= 0; i--) {
            soma += (cnpj.charAt(i) - '0') * peso;
            peso = (peso == 9) ? 2 : peso + 1;
        }
        int segundoDigito = (soma % 11) < 2 ? 0 : 11 - (soma % 11);

        // Verifica o segundo dígito verificador
        return segundoDigito == (cnpj.charAt(13) - '0');
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    //Remoção do isValidName, pois não será mais utilizado
    //Remoção do isNumeric, pois não será mais utilizado

    public static boolean isValidId(String id) {
        if (isNullOrEmpty(id)) return false;
        return id.matches("^[A-Za-z0-9-]+$"); // Agora permite letras, números e hífen
    }
}