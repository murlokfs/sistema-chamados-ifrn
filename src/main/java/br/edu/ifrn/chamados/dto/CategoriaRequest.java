package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record CategoriaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 1000) String descricao,
        @NotNull @Positive Long setorId) {
}
