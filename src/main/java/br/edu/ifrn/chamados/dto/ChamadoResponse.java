package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import java.time.OffsetDateTime;
import java.util.List;

public record ChamadoResponse(Long id, String titulo, String descricao, String local,
                                     Prioridade prioridade, StatusChamado status,
                                     OffsetDateTime dataAbertura, OffsetDateTime dataFechamento,
                                     String solucao, UsuarioResponse solicitante,
                                     CategoriaResponse categoria, UsuarioResponse tecnico,
                              List<InteracaoResponse> interacoes) {

    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
                chamado.getLocal(), chamado.getPrioridade(), chamado.getStatus(),
                chamado.getDataAbertura(), chamado.getDataFechamento(), chamado.getSolucao(),
                UsuarioResponse.de(chamado.getSolicitante()), CategoriaResponse.de(chamado.getCategoria()),
                chamado.getTecnico() == null ? null : UsuarioResponse.de(chamado.getTecnico()),
                chamado.getInteracoes().stream().map(InteracaoResponse::de).toList());
    }
}
