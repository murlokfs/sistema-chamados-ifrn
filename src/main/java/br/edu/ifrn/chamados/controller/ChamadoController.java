package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.model.Prioridade;
import br.edu.ifrn.chamados.model.StatusChamado;
import br.edu.ifrn.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody AbrirChamadoRequest dados) {
        ChamadoResponse chamado = chamadoService.abrir(dados);
        return ResponseEntity.created(URI.create("/chamados/" + chamado.id())).body(chamado);
    }

    @GetMapping
    public PaginaResponse<ChamadoResumoResponse> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Pageable pageable) {
        return chamadoService.listar(status, prioridade, setorId, categoriaId, solicitanteId,
                tecnicoId, dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscar(@PathVariable Long id) {
        return chamadoService.buscar(id);
    }

    @PatchMapping("/{id}/assumir")
    public ChamadoResponse assumir(@PathVariable Long id, @Valid @RequestBody AssumirChamadoRequest dados) {
        return chamadoService.assumir(id, dados);
    }

    @PatchMapping("/{id}/resolver")
    public ChamadoResponse resolver(@PathVariable Long id, @Valid @RequestBody ResolverChamadoRequest dados) {
        return chamadoService.resolver(id, dados);
    }

    @PatchMapping("/{id}/cancelar")
    public ChamadoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarChamadoRequest dados) {
        return chamadoService.cancelar(id, dados);
    }

    @PostMapping("/{id}/interacoes")
    public ResponseEntity<InteracaoResponse> interagir(@PathVariable Long id, @Valid @RequestBody InteracaoRequest dados) {
        return ResponseEntity.status(201).body(chamadoService.interagir(id, dados));
    }
}
