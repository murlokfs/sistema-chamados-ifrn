package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

final class Paginacao {

    private Paginacao() {
    }

    static Pageable validar(Pageable pageable, Set<String> camposPermitidos) {
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new DadosInvalidosException("A página solicitada ultrapassa o limite de paginação.");
        }
        for (Sort.Order ordem : pageable.getSort()) {
            if (!camposPermitidos.contains(ordem.getProperty())) {
                throw new DadosInvalidosException("Campo de ordenação inválido: " + ordem.getProperty() + ".");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id");
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50), sort);
    }
}
