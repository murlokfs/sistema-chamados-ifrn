package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @EntityGraph(attributePaths = {"categoria.setor", "solicitante.setor", "tecnico.setor"})
    @Query("""
            select c from Chamado c
            where (:status is null or c.status = :status)
              and (:prioridade is null or c.prioridade = :prioridade)
              and (:setorId is null or c.categoria.setor.id = :setorId)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
              and (:solicitanteId is null or c.solicitante.id = :solicitanteId)
              and (:tecnicoId is null or c.tecnico.id = :tecnicoId)
              and c.dataAbertura >= coalesce(:dataInicio, c.dataAbertura)
              and (:semDataFim = true or c.dataAbertura < :dataFim)
            """)
    Page<Chamado> filtrar(StatusChamado status, Prioridade prioridade, Long setorId,
                         Long categoriaId, Long solicitanteId, Long tecnicoId,
                         OffsetDateTime dataInicio, OffsetDateTime dataFim,
                         boolean semDataFim, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria.setor", "solicitante.setor", "tecnico.setor"})
    Page<Chamado> findBySolicitanteIdAndStatus(Long solicitanteId, StatusChamado status, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria.setor", "solicitante.setor", "tecnico.setor"})
    Page<Chamado> findBySolicitanteId(Long solicitanteId, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria.setor", "solicitante.setor", "tecnico.setor"})
    Page<Chamado> findByTecnicoIdAndStatus(Long tecnicoId, StatusChamado status, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria.setor", "solicitante.setor", "tecnico.setor"})
    Page<Chamado> findByTecnicoId(Long tecnicoId, Pageable pageable);

    // Todas as ações bloqueiam a mesma linha até a transação terminar.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Chamado c where c.id = :id")
    Optional<Chamado> buscarParaAtualizar(Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);

    boolean existsBySolicitanteId(Long solicitanteId);

    boolean existsByTecnicoId(Long tecnicoId);
}
