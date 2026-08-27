package br.com.somosdb.votacao.shared.error;

public class AssociadoNaoPodeVotarException extends RuntimeException {

    public AssociadoNaoPodeVotarException() {
        super("O associado não está habilitado para votar");
    }
}
