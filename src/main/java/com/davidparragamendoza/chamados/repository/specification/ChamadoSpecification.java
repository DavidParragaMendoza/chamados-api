package com.davidparragamendoza.chamados.repository.specification;

import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.Usuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class ChamadoSpecification {

    private ChamadoSpecification() {
    }

    public static Specification<Chamado> comFiltros(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim) {

        Specification<Chamado> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();

        if (status != null) {
            specification = specification.and(comStatus(status));
        }
        if (prioridade != null) {
            specification = specification.and(comPrioridade(prioridade));
        }
        if (setorId != null) {
            specification = specification.and(comSetor(setorId));
        }
        if (categoriaId != null) {
            specification = specification.and(comCategoria(categoriaId));
        }
        if (solicitanteId != null) {
            specification = specification.and(comSolicitante(solicitanteId));
        }
        if (tecnicoId != null) {
            specification = specification.and(comTecnico(tecnicoId));
        }
        if (dataInicio != null) {
            specification = specification.and(comDataInicio(dataInicio));
        }
        if (dataFim != null) {
            specification = specification.and(comDataFim(dataFim));
        }

        return specification;
    }

    public static Specification<Chamado> porUsuario(Long usuarioId, StatusChamado status) {
        Specification<Chamado> specification = (root, query, criteriaBuilder) -> {
            Join<Chamado, Usuario> tecnico = root.join("tecnico", JoinType.LEFT);
            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("solicitante").get("id"), usuarioId),
                    criteriaBuilder.equal(tecnico.get("id"), usuarioId));
        };

        if (status != null) {
            specification = specification.and(comStatus(status));
        }

        return specification;
    }

    public static Specification<Chamado> comStatus(StatusChamado status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Chamado> comPrioridade(PrioridadeChamado prioridade) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("prioridade"), prioridade);
    }

    public static Specification<Chamado> comSetor(Long setorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("categoria").get("setor").get("id"), setorId);
    }

    public static Specification<Chamado> comCategoria(Long categoriaId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Chamado> comSolicitante(Long solicitanteId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("solicitante").get("id"), solicitanteId);
    }

    public static Specification<Chamado> comTecnico(Long tecnicoId) {
        return (root, query, criteriaBuilder) -> {
            Join<Chamado, Usuario> tecnico = root.join("tecnico", JoinType.LEFT);
            return criteriaBuilder.equal(tecnico.get("id"), tecnicoId);
        };
    }

    public static Specification<Chamado> comDataInicio(LocalDateTime dataInicio) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("dataAbertura"), dataInicio);
    }

    public static Specification<Chamado> comDataFim(LocalDateTime dataFim) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("dataAbertura"), dataFim);
    }
}
