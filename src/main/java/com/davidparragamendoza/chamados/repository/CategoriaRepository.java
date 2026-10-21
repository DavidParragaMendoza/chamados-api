package com.davidparragamendoza.chamados.repository;

import com.davidparragamendoza.chamados.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {

    List<Categoria> findBySetorId(Long setorId);

    boolean existsBySetorId(Long setorId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM chamados WHERE categoria_id = :categoriaId)", nativeQuery = true)
    boolean existsChamadoByCategoriaId(@Param("categoriaId") Long categoriaId);
}
