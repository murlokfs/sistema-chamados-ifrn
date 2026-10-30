package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetorService {

    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;

    public PaginaResponse<SetorResponse> listar(String nome, Pageable pageable) {
        pageable = Paginacao.validar(pageable, Set.of("id", "nome", "sigla"));
        return PaginaResponse.de(setorRepository.findByNomeContainingIgnoreCase(nome.strip(), pageable)
                .map(SetorResponse::de));
    }

    public SetorResponse buscar(Long id) {
        return SetorResponse.de(buscarEntidade(id));
    }

    public Setor buscarEntidade(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", id));
    }

    @Transactional
    public SetorResponse criar(SetorRequest dados) {
        Setor setor = new Setor();
        preencher(setor, dados);
        return SetorResponse.de(setorRepository.save(setor));
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest dados) {
        Setor setor = buscarEntidade(id);
        preencher(setor, dados);
        return SetorResponse.de(setorRepository.save(setor));
    }

    @Transactional
    public void excluir(Long id) {
        Setor setor = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaSetorId(id)) {
            throw new RegraNegocioException("Não é possível excluir um setor com chamados vinculados.");
        }
        if (categoriaRepository.existsBySetorId(id) || usuarioRepository.existsBySetorId(id)) {
            throw new RegraNegocioException("Remova as categorias e transfira os técnicos antes de excluir o setor.");
        }
        setorRepository.delete(setor);
    }

    private void preencher(Setor setor, SetorRequest dados) {
        String sigla = dados.sigla().strip().toUpperCase(Locale.ROOT);
        if (setorRepository.existsBySiglaIgnoreCaseAndIdNot(sigla, setor.getId() == null ? 0L : setor.getId())) {
            throw new RegraNegocioException("Já existe um setor com essa sigla.");
        }
        setor.setNome(dados.nome().strip());
        setor.setSigla(sigla);
    }
}
