package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.CategoriaRequestDTO;
import com.davidparragamendoza.chamados.dto.CategoriaResponseDTO;
import com.davidparragamendoza.chamados.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public ResponseEntity<Page<CategoriaResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long setorId,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(categoriaService.listar(nome, setorId, pageable));
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponseDTO> salvar(@Valid @RequestBody CategoriaRequestDTO requestDTO) {
        CategoriaResponseDTO responseDTO = categoriaService.salvar(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequestDTO requestDTO) {
        return ResponseEntity.ok(categoriaService.atualizar(id, requestDTO));
    }

    @GetMapping("/setores/{setorId}/categorias")
    public ResponseEntity<List<CategoriaResponseDTO>> listarPorSetor(@PathVariable Long setorId) {
        return ResponseEntity.ok(categoriaService.listarPorSetor(setorId));
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        categoriaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
