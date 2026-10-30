package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorService setorService;

    public PaginaResponse<UsuarioResponse> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable) {
        pageable = Paginacao.validar(pageable, Set.of("id", "nome", "email", "matricula", "tipo", "setor.id", "setor.nome"));
        return PaginaResponse.de(usuarioRepository.filtrar(nome.strip(), tipo, setorId, pageable)
                .map(UsuarioResponse::de));
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(buscarEntidade(id));
    }

    public Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest dados) {
        Usuario usuario = new Usuario();
        preencher(usuario, dados);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest dados) {
        Usuario usuario = buscarEntidade(id);
        Long setorAtual = usuario.getSetor() == null ? null : usuario.getSetor().getId();
        if (usuario.getTipo() != dados.tipo() &&
                (chamadoRepository.existsBySolicitanteId(id) || chamadoRepository.existsByTecnicoId(id))) {
            throw new RegraNegocioException("Um usuário com chamados vinculados não pode mudar de tipo.");
        }
        if (!Objects.equals(setorAtual, dados.setorId()) && chamadoRepository.existsByTecnicoId(id)) {
            throw new RegraNegocioException("Um técnico com chamados atribuídos não pode mudar de setor.");
        }
        preencher(usuario, dados);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    private void preencher(Usuario usuario, UsuarioRequest dados) {
        String email = dados.email().strip().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId() == null ? 0L : usuario.getId())) {
            throw new RegraNegocioException("Já existe um usuário com esse email.");
        }
        if (dados.tipo() == TipoUsuario.TECNICO && dados.setorId() == null) {
            throw new RegraNegocioException("Todo técnico deve pertencer a um setor.");
        }
        if (dados.tipo() == TipoUsuario.SOLICITANTE && dados.setorId() != null) {
            throw new RegraNegocioException("Apenas técnicos podem ter um setor vinculado.");
        }
        Setor setor = dados.setorId() == null ? null : setorService.buscarEntidade(dados.setorId());
        usuario.setNome(dados.nome().strip());
        usuario.setEmail(email);
        usuario.setMatricula(dados.matricula().strip());
        usuario.setTipo(dados.tipo());
        usuario.setSetor(setor);
    }
}
