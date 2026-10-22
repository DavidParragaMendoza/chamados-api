package com.davidparragamendoza.chamados.repository;

import com.davidparragamendoza.chamados.model.Setor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SetorRepository extends JpaRepository<Setor, Long> {

    boolean existsBySiglaIgnoreCase(String sigla);

    boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, Long id);

    Page<Setor> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM categorias WHERE setor_id = :setorId)", nativeQuery = true)
    boolean existsCategoriaBySetorId(@Param("setorId") Long setorId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM usuarios WHERE setor_id = :setorId)", nativeQuery = true)
    boolean existsUsuarioBySetorId(@Param("setorId") Long setorId);
}
