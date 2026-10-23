package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.UsuarioRequestDTO;
import com.davidparragamendoza.chamados.dto.UsuarioResponseDTO;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.service.ChamadoService;
import com.davidparragamendoza.chamados.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ChamadoService chamadoService;

    public UsuarioController(UsuarioService usuarioService, ChamadoService chamadoService) {
        this.usuarioService = usuarioService;
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> salvar(@Valid @RequestBody UsuarioRequestDTO requestDTO) {
        UsuarioResponseDTO responseDTO = usuarioService.salvar(requestDTO);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(responseDTO.id())
                .toUri();

        return ResponseEntity.created(uri).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoUsuario tipo,
            @RequestParam(required = false) Long setorId,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(nome, tipo, setorId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO requestDTO) {
        return ResponseEntity.ok(usuarioService.atualizar(id, requestDTO));
    }

    @GetMapping("/{id}/chamados")
    public ResponseEntity<Page<ChamadoResponseDTO>> listarChamados(
            @PathVariable Long id,
            @RequestParam(required = false) StatusChamado status,
            @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chamadoService.listarPorUsuario(id, status, pageable));
    }
}
