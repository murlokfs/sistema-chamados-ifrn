package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.*;

public record SetorRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 20) String sigla) {
}
