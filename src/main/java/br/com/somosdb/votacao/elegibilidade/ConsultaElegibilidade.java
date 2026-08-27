package br.com.somosdb.votacao.elegibilidade;

public interface ConsultaElegibilidade {

    StatusElegibilidade consultar(Cpf cpf);
}
