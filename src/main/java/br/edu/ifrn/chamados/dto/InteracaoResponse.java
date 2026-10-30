package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Interacao;
import java.time.OffsetDateTime;

public record InteracaoResponse(Long id, Long chamadoId, String texto, OffsetDateTime dataHora,
                               UsuarioResponse autor) {

    public static InteracaoResponse de(Interacao interacao) {
        return new InteracaoResponse(interacao.getId(), interacao.getChamado().getId(),
                interacao.getTexto(), interacao.getDataHora(), UsuarioResponse.de(interacao.getAutor()));
    }
}
