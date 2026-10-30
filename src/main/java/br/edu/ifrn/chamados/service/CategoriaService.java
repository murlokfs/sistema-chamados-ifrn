package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorService setorService;

    public PaginaResponse<CategoriaResponse> listar(String nome, Long setorId, Pageable pageable) {
        pageable = Paginacao.validar(pageable, Set.of("id", "nome", "descricao", "setor.id", "setor.nome"));
        return PaginaResponse.de(categoriaRepository.filtrar(nome.strip(), setorId, pageable)
                .map(CategoriaResponse::de));
    }

    public PaginaResponse<CategoriaResponse> listarPorSetor(Long setorId, String nome, Pageable pageable) {
        setorService.buscarEntidade(setorId);
        return listar(nome, setorId, pageable);
    }

    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(buscarEntidade(id));
    }

    public Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest dados) {
        Categoria categoria = new Categoria();
        preencher(categoria, dados, setorService.buscarEntidade(dados.setorId()));
        return CategoriaResponse.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest dados) {
        Categoria categoria = buscarEntidade(id);
        Setor setor = setorService.buscarEntidade(dados.setorId());
        if (!categoria.getSetor().getId().equals(setor.getId()) && chamadoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Uma categoria com chamados não pode mudar de setor.");
        }
        preencher(categoria, dados, setor);
        return CategoriaResponse.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é possível excluir uma categoria com chamados vinculados.");
        }
        categoriaRepository.delete(categoria);
    }

    private void preencher(Categoria categoria, CategoriaRequest dados, Setor setor) {
        categoria.setNome(dados.nome().strip());
        categoria.setDescricao(dados.descricao().strip());
        categoria.setSetor(setor);
    }
}
