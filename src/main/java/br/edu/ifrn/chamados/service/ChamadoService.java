package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.DadosInvalidosException;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {

    private static final ZoneId FUSO_CAMPUS = ZoneId.of("America/Sao_Paulo");
    private static final Set<String> CAMPOS_ORDENACAO = Set.of("id", "titulo", "local", "prioridade",
            "status", "dataAbertura", "dataFechamento", "categoria.id", "solicitante.id", "tecnico.id");

    private final ChamadoRepository chamadoRepository;
    private final InteracaoRepository interacaoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;

    @Transactional
    public ChamadoResponse abrir(AbrirChamadoRequest dados) {
        Usuario solicitante = usuarioService.buscarEntidade(dados.solicitanteId());
        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE) {
            throw new RegraNegocioException("Apenas solicitantes podem abrir chamados.");
        }
        Categoria categoria = categoriaService.buscarEntidade(dados.categoriaId());
        Chamado chamado = new Chamado();
        chamado.setTitulo(dados.titulo().strip());
        chamado.setDescricao(dados.descricao().strip());
        chamado.setLocal(dados.local().strip());
        chamado.setPrioridade(dados.prioridade() == null ? Prioridade.MEDIA : dados.prioridade());
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(OffsetDateTime.now(ZoneOffset.UTC));
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public PaginaResponse<ChamadoResumoResponse> listar(StatusChamado status, Prioridade prioridade,
            Long setorId, Long categoriaId, Long solicitanteId, Long tecnicoId,
            LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        pageable = Paginacao.validar(pageable, CAMPOS_ORDENACAO);
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new DadosInvalidosException("dataInicio deve ser anterior ou igual a dataFim.");
        }
        OffsetDateTime inicio = dataInicio == null ? null : dataInicio.atStartOfDay(FUSO_CAMPUS).toOffsetDateTime();
        // O limite exclusivo inclui todo o último dia, sem depender da precisão do banco.
        OffsetDateTime fim = dataFim == null ? OffsetDateTime.now(ZoneOffset.UTC)
                : dataFim.plusDays(1).atStartOfDay(FUSO_CAMPUS).toOffsetDateTime();
        return PaginaResponse.de(chamadoRepository.filtrar(status, prioridade, setorId, categoriaId,
                solicitanteId, tecnicoId, inicio, fim, dataFim == null, pageable).map(ChamadoResumoResponse::de));
    }

    public ChamadoResponse buscar(Long id) {
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado", id));
        return ChamadoResponse.de(chamado);
    }

    public PaginaResponse<ChamadoResumoResponse> listarPorUsuario(Long usuarioId, StatusChamado status, Pageable pageable) {
        Usuario usuario = usuarioService.buscarEntidade(usuarioId);
        pageable = Paginacao.validar(pageable, CAMPOS_ORDENACAO);
        Page<Chamado> pagina;
        if (usuario.getTipo() == TipoUsuario.SOLICITANTE) {
            pagina = status == null ? chamadoRepository.findBySolicitanteId(usuarioId, pageable)
                    : chamadoRepository.findBySolicitanteIdAndStatus(usuarioId, status, pageable);
        } else {
            pagina = status == null ? chamadoRepository.findByTecnicoId(usuarioId, pageable)
                    : chamadoRepository.findByTecnicoIdAndStatus(usuarioId, status, pageable);
        }
        return PaginaResponse.de(pagina.map(ChamadoResumoResponse::de));
    }

    @Transactional
    public ChamadoResponse assumir(Long id, AssumirChamadoRequest dados) {
        Chamado chamado = buscarParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.ABERTO);
        Usuario tecnico = usuarioService.buscarEntidade(dados.tecnicoId());
        if (tecnico.getTipo() != TipoUsuario.TECNICO) {
            throw new RegraNegocioException("Apenas técnicos podem assumir chamados.");
        }
        if (!tecnico.getSetor().getId().equals(chamado.getCategoria().getSetor().getId())) {
            throw new RegraNegocioException("O técnico deve pertencer ao setor responsável pela categoria.");
        }
        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse resolver(Long id, ResolverChamadoRequest dados) {
        Chamado chamado = buscarParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.EM_ATENDIMENTO);
        Usuario tecnico = usuarioService.buscarEntidade(dados.tecnicoId());
        if (!chamado.getTecnico().getId().equals(tecnico.getId())) {
            throw new RegraNegocioException("Somente o técnico atribuído pode resolver o chamado.");
        }
        if (dados.solucao() == null || dados.solucao().isBlank()) {
            throw new RegraNegocioException("Informe a solução para resolver o chamado.");
        }
        chamado.setSolucao(dados.solucao().strip());
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataFechamento(OffsetDateTime.now(ZoneOffset.UTC));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse cancelar(Long id, CancelarChamadoRequest dados) {
        Chamado chamado = buscarParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.ABERTO);
        Usuario solicitante = usuarioService.buscarEntidade(dados.solicitanteId());
        if (!chamado.getSolicitante().getId().equals(solicitante.getId())) {
            throw new RegraNegocioException("Somente o próprio solicitante pode cancelar o chamado.");
        }
        chamado.setStatus(StatusChamado.CANCELADO);
        chamado.setDataFechamento(OffsetDateTime.now(ZoneOffset.UTC));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public InteracaoResponse interagir(Long id, InteracaoRequest dados) {
        Chamado chamado = buscarParaAtualizar(id);
        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.CANCELADO) {
            throw new RegraNegocioException("Chamados encerrados não aceitam novas interações.");
        }
        Usuario autor = usuarioService.buscarEntidade(dados.autorId());
        boolean ehSolicitante = chamado.getSolicitante().getId().equals(autor.getId());
        boolean ehTecnico = chamado.getTecnico() != null && chamado.getTecnico().getId().equals(autor.getId());
        if (!ehSolicitante && !ehTecnico) {
            throw new RegraNegocioException("Apenas o solicitante e o técnico atribuído podem registrar interações.");
        }
        Interacao interacao = new Interacao();
        interacao.setChamado(chamado);
        interacao.setAutor(autor);
        interacao.setTexto(dados.texto().strip());
        interacao.setDataHora(OffsetDateTime.now(ZoneOffset.UTC));
        return InteracaoResponse.de(interacaoRepository.save(interacao));
    }

    private Chamado buscarParaAtualizar(Long id) {
        return chamadoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado", id));
    }

    private void exigirStatus(Chamado chamado, StatusChamado esperado) {
        if (chamado.getStatus() != esperado) {
            throw new RegraNegocioException("Esta ação exige um chamado " + esperado
                    + ". O status atual é " + chamado.getStatus() + ".");
        }
    }
}
