package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record AssumirChamadoRequest(
        @NotNull @Positive Long tecnicoId) {
}
