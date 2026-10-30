package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.TipoUsuario;
import jakarta.validation.constraints.*;

public record UsuarioRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String matricula,
        @NotNull TipoUsuario tipo,
        @Positive Long setorId) {
}
