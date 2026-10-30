package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.service.CategoriaService;
import br.edu.ifrn.chamados.service.SetorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/setores")
@RequiredArgsConstructor
public class SetorController {

    private final SetorService setorService;
    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<SetorResponse> criar(@Valid @RequestBody SetorRequest dados) {
        SetorResponse setor = setorService.criar(dados);
        return ResponseEntity.created(URI.create("/setores/" + setor.id())).body(setor);
    }

    @GetMapping
    public PaginaResponse<SetorResponse> listar(@RequestParam(defaultValue = "") String nome, Pageable pageable) {
        return setorService.listar(nome, pageable);
    }

    @GetMapping("/{id}")
    public SetorResponse buscar(@PathVariable Long id) {
        return setorService.buscar(id);
    }

    @PutMapping("/{id}")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest dados) {
        return setorService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        setorService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/categorias")
    public PaginaResponse<CategoriaResponse> categorias(@PathVariable Long id,
            @RequestParam(defaultValue = "") String nome, Pageable pageable) {
        return categoriaService.listarPorSetor(id, nome, pageable);
    }
}
