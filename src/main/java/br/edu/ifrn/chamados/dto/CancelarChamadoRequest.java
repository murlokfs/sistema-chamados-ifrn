package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record CancelarChamadoRequest(
        @NotNull @Positive Long solicitanteId) {
}
