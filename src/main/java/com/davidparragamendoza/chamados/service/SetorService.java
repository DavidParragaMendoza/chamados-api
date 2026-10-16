package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.SetorRequestDTO;
import com.davidparragamendoza.chamados.dto.SetorResponseDTO;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SetorService {
    private final SetorRepository setorRepository;

    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    //1. Lista con filtro opcional por nombre y paginación
    @Transactional(readOnly =true)
    public Page<SetorResponseDTO> listarTodos(String nome, Pageable pageable){
        if (nome !=null && !nome.isBlank()){
            return setorRepository.findByNomeContainingIgnoreCase(nome, pageable)
                    .map(SetorResponseDTO::fromEntity);
        }
        return setorRepository.findAll(pageable)
                .map(SetorResponseDTO::fromEntity);
    }

    //2. Buscar por ID.
    @Transactional(readOnly = true)
    public SetorResponseDTO buscarPorId(Long id){
        Setor setor = setorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sector no encontrado con el ID: " + id));

        return SetorResponseDTO.fromEntity(setor);
    }

    //3. Guardar nuevo Sector (Válida unicidad de siglas)
    @Transactional
    public SetorResponseDTO salvar(SetorRequestDTO dto){
        if (setorRepository.existsBySigla(dto.sigla())) {
            throw new IllegalArgumentException("Ya existe un sector registrado con la sigla: " + dto.sigla());
        }
        Setor novoSetor = Setor.builder()
                .nome(dto.nome())
                .sigla(dto.sigla())
                .ativo(true)
                .build();
        Setor setorSalvo = setorRepository.save(novoSetor);
        return SetorResponseDTO.fromEntity(setorSalvo);
    }

    //4. Actualizar un sector existente
    @Transactional
    public SetorResponseDTO atualizar(Long id, SetorRequestDTO dto){
        Setor setor = setorRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Sector no encontrado con el ID: " + id));
        // Validar que si cambia la sigla, la nueva no esté ya ocupada por otro sector
        if (!setor.getSigla().equalsIgnoreCase(dto.sigla()) && setorRepository.existsBySigla(dto.sigla())) {
            throw new IllegalArgumentException("Ya existe otro sector registrado con la sigla: " + dto.sigla());
        }

        setor.setNome(dto.nome());
        setor.setSigla(dto.sigla());

        Setor setorAtualizado = setorRepository.save(setor);
        return SetorResponseDTO.fromEntity(setorAtualizado);
    }

    // 5. Eliminar (o Desactivar) por ID
    @Transactional
    public void deletar(Long id) {
        if (!setorRepository.existsById(id)) {
            throw new EntityNotFoundException("Sector no encontrado con el ID: " + id);
        }
        setorRepository.deleteById(id);
    }
}