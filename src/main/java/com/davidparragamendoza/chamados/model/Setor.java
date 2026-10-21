package com.davidparragamendoza.chamados.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "setores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 50)
    private String sigla;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;
}
