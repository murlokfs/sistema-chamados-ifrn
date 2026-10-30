package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Prioridade;
import jakarta.validation.constraints.*;

public record AbrirChamadoRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 5000) String descricao,
        @NotBlank @Size(max = 150) String local,
        Prioridade prioridade,
        @NotNull @Positive Long solicitanteId,
        @NotNull @Positive Long categoriaId) {
}
