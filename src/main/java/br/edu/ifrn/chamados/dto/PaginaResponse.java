package br.edu.ifrn.chamados.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginaResponse<T>(List<T> content, int number, int size,
                                long totalElements, int totalPages) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
