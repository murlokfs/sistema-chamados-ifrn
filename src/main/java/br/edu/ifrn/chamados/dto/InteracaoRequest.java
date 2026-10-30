package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record InteracaoRequest(
        @NotNull @Positive Long autorId,
        @NotBlank @Size(max = 5000) String texto) {
}
