package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {
}
