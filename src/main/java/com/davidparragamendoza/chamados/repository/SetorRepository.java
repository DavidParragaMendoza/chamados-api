package com.davidparragamendoza.chamados.repository;


import com.davidparragamendoza.chamados.model.Setor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SetorRepository extends JpaRepository<Setor, Long> {
    // Buscar sector por sigla exacta (útil para validaciones)
    Optional<Setor>finBySigla(String sigla);

    // Verificar si ya existe un sector con esa sigla (al crear)
    boolean existsBySigla(String sigla);

    // Búsqueda paginada por nombre (filtro insensible a mayúsculas/minúsculas)
    Page<Setor>findByNomeContainingIgnoreCase(String nome, Pageable pageable);


}
