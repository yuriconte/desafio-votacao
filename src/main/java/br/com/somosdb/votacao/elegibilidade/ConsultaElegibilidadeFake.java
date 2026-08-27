package br.com.somosdb.votacao.elegibilidade;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

@Component
public class ConsultaElegibilidadeFake implements ConsultaElegibilidade {

    private final RandomGenerator randomGenerator;

    public ConsultaElegibilidadeFake(RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    @Override
    public StatusElegibilidade consultar(Cpf cpf) {
        return randomGenerator.nextBoolean()
                ? StatusElegibilidade.ABLE_TO_VOTE
                : StatusElegibilidade.UNABLE_TO_VOTE;
    }
}
