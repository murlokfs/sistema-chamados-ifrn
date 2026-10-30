package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String matricula,
                              TipoUsuario tipo, SetorResponse setor) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getMatricula(), usuario.getTipo(),
                usuario.getSetor() == null ? null : SetorResponse.de(usuario.getSetor()));
    }
}
