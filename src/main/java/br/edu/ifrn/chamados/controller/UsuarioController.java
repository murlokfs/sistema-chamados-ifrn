package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.model.TipoUsuario;
import br.edu.ifrn.chamados.service.ChamadoService;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest dados) {
        UsuarioResponse usuario = usuarioService.criar(dados);
        return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(usuario);
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(defaultValue = "") String nome,
            @RequestParam(required = false) TipoUsuario tipo, @RequestParam(required = false) Long setorId,
            Pageable pageable) {
        return usuarioService.listar(nome, tipo, setorId, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return usuarioService.buscar(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest dados) {
        return usuarioService.atualizar(id, dados);
    }

    @GetMapping("/{id}/chamados")
    public PaginaResponse<ChamadoResumoResponse> chamados(@PathVariable Long id,
            @RequestParam(required = false) StatusChamado status, Pageable pageable) {
        return chamadoService.listarPorUsuario(id, status, pageable);
    }
}
