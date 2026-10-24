package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.AssumirChamadoDTO;
import com.davidparragamendoza.chamados.dto.CancelarChamadoDTO;
import com.davidparragamendoza.chamados.dto.ChamadoCreateDTO;
import com.davidparragamendoza.chamados.dto.ChamadoDetalheResponseDTO;
import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.InteracaoRequestDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;
import com.davidparragamendoza.chamados.dto.ResolverChamadoDTO;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.service.ChamadoService;
import com.davidparragamendoza.chamados.service.InteracaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final InteracaoService interacaoService;

    public ChamadoController(ChamadoService chamadoService, InteracaoService interacaoService) {
        this.chamadoService = chamadoService;
        this.interacaoService = interacaoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponseDTO> abrir(@Valid @RequestBody ChamadoCreateDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chamadoService.abrir(requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoDetalheResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<ChamadoResponseDTO>> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) PrioridadeChamado prioridade,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chamadoService.listar(
                status, prioridade, setorId, categoriaId, solicitanteId, tecnicoId, dataInicio, dataFim, pageable));
    }

    @PatchMapping("/{id}/assumir")
    public ResponseEntity<ChamadoResponseDTO> assumir(
            @PathVariable Long id,
            @Valid @RequestBody AssumirChamadoDTO requestDTO) {
        return ResponseEntity.ok(chamadoService.assumir(id, requestDTO));
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<ChamadoResponseDTO> resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolverChamadoDTO requestDTO) {
        return ResponseEntity.ok(chamadoService.resolver(id, requestDTO));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ChamadoResponseDTO> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarChamadoDTO requestDTO) {
        return ResponseEntity.ok(chamadoService.cancelar(id, requestDTO));
    }

    @PostMapping("/{id}/interacoes")
    public ResponseEntity<InteracaoResponseDTO> criarInteracao(
            @PathVariable Long id,
            @Valid @RequestBody InteracaoRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interacaoService.criar(id, requestDTO));
    }

    @GetMapping("/{id}/interacoes")
    public ResponseEntity<List<InteracaoResponseDTO>> listarInteracoes(@PathVariable Long id) {
        return ResponseEntity.ok(interacaoService.listarPorChamado(id));
    }
}
