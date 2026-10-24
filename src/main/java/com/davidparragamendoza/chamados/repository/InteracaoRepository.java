package com.davidparragamendoza.chamados.repository;

import com.davidparragamendoza.chamados.model.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    List<Interacao> findByChamadoIdOrderByDataHoraAsc(Long chamadoId);
}
