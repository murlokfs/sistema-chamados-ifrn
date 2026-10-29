package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "setor")
    @Query("""
            select u from Usuario u left join u.setor setor
            where locate(lower(:nome), lower(u.nome)) > 0
              and (:tipo is null or u.tipo = :tipo)
              and (:setorId is null or u.setor.id = :setorId)
            """)
    Page<Usuario> filtrar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsBySetorId(Long setorId);
}
