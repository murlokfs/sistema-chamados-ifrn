package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @EntityGraph(attributePaths = "setor")
    @Query("""
            select c from Categoria c
            where locate(lower(:nome), lower(c.nome)) > 0
              and (:setorId is null or c.setor.id = :setorId)
            """)
    Page<Categoria> filtrar(String nome, Long setorId, Pageable pageable);

    boolean existsBySetorId(Long setorId);
}
