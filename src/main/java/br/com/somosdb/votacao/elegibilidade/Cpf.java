package br.com.somosdb.votacao.elegibilidade;

import java.util.Optional;

public final class Cpf {

    private static final int TAMANHO = 11;
    private static final String FORMATO_ACEITO = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}";

    private final String numero;

    private Cpf(String numero) {
        this.numero = numero;
    }

    public static Optional<Cpf> criar(String valor) {
        if (valor == null || !valor.matches(FORMATO_ACEITO)) {
            return Optional.empty();
        }
        String numero = valor.replaceAll("\\D", "");
        if (numero.length() != TAMANHO || todosDigitosIguais(numero)) {
            return Optional.empty();
        }
        boolean valido = calcularDigito(numero, 9) == Character.digit(numero.charAt(9), 10)
                && calcularDigito(numero, 10) == Character.digit(numero.charAt(10), 10);
        return valido ? Optional.of(new Cpf(numero)) : Optional.empty();
    }

    public String numero() {
        return numero;
    }

    private static boolean todosDigitosIguais(String numero) {
        return numero.chars().allMatch(digito -> digito == numero.charAt(0));
    }

    private static int calcularDigito(String numero, int quantidade) {
        int soma = 0;
        for (int indice = 0; indice < quantidade; indice++) {
            soma += Character.digit(numero.charAt(indice), 10) * (quantidade + 1 - indice);
        }
        int digito = 11 - soma % 11;
        return digito >= 10 ? 0 : digito;
    }
}
