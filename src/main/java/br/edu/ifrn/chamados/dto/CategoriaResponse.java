package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Categoria;

public record CategoriaResponse(Long id, String nome, String descricao, SetorResponse setor) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao(),
                SetorResponse.de(categoria.getSetor()));
    }
}
