package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Setor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    Page<Setor> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, Long id);
}
