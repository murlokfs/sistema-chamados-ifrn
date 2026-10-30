package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import java.time.OffsetDateTime;

public record ChamadoResumoResponse(Long id, String titulo, String descricao, String local,
                                     Prioridade prioridade, StatusChamado status,
                                     OffsetDateTime dataAbertura, OffsetDateTime dataFechamento,
                                     String solucao, UsuarioResponse solicitante,
                                     CategoriaResponse categoria, UsuarioResponse tecnico) {

    public static ChamadoResumoResponse de(Chamado chamado) {
        return new ChamadoResumoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
                chamado.getLocal(), chamado.getPrioridade(), chamado.getStatus(),
                chamado.getDataAbertura(), chamado.getDataFechamento(), chamado.getSolucao(),
                UsuarioResponse.de(chamado.getSolicitante()), CategoriaResponse.de(chamado.getCategoria()),
                chamado.getTecnico() == null ? null : UsuarioResponse.de(chamado.getTecnico()));
    }
}
