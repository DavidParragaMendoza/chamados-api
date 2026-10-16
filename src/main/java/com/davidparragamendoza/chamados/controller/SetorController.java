package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.SetorRequestDTO;
import com.davidparragamendoza.chamados.dto.SetorResponseDTO;
import com.davidparragamendoza.chamados.service.SetorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/setores")
public class SetorController {

    private final SetorService setorService;

    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    // 1. GET /setores (Listar con filtro opcional por nombre y paginación)
    @GetMapping
    public ResponseEntity<Page<SetorResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<SetorResponseDTO> page = setorService.listarTodos(nome, pageable);
        return ResponseEntity.ok(page);
    }

    // 2. GET /setores/{id} (Buscar un sector específico por ID)
    @GetMapping("/{id}")
    public ResponseEntity<SetorResponseDTO> buscarPorId(@PathVariable Long id) {
        SetorResponseDTO dto = setorService.buscarPorId(id);
        return ResponseEntity.ok(dto);
    }

    // 3. POST /setores (Crear un nuevo sector)
    @PostMapping
    public ResponseEntity<SetorResponseDTO> salvar(@Valid @RequestBody SetorRequestDTO requestDTO) {
        SetorResponseDTO responseDTO = setorService.salvar(requestDTO);

        // Genera la URI del nuevo recurso creado (ej. /setores/1) para el header Location
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(responseDTO.id())
                .toUri();

        return ResponseEntity.created(uri).body(responseDTO);
    }

    // 4. PUT /setores/{id} (Actualizar un sector existente)
    @PutMapping("/{id}")
    public ResponseEntity<SetorResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody SetorRequestDTO requestDTO) {
        SetorResponseDTO responseDTO = setorService.atualizar(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    // 5. DELETE /setores/{id} (Eliminar un sector)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        setorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}