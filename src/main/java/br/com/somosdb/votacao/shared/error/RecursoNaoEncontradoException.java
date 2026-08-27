package br.com.somosdb.votacao.shared.error;

public class RecursoNaoEncontradoException extends RuntimeException {

    private final String codigo;

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}

