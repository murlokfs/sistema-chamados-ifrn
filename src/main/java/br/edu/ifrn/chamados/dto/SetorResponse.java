package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Setor;

public record SetorResponse(Long id, String nome, String sigla) {

    public static SetorResponse de(Setor setor) {
        return new SetorResponse(setor.getId(), setor.getNome(), setor.getSigla());
    }
}
