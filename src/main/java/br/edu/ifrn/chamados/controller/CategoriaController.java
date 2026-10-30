package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest dados) {
        CategoriaResponse categoria = categoriaService.criar(dados);
        return ResponseEntity.created(URI.create("/categorias/" + categoria.id())).body(categoria);
    }

    @GetMapping
    public PaginaResponse<CategoriaResponse> listar(@RequestParam(defaultValue = "") String nome,
            @RequestParam(required = false) Long setorId, Pageable pageable) {
        return categoriaService.listar(nome, setorId, pageable);
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return categoriaService.buscar(id);
    }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest dados) {
        return categoriaService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
