package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record ResolverChamadoRequest(
        @NotNull @Positive Long tecnicoId,
        @NotBlank @Size(max = 5000) String solucao) {
}
