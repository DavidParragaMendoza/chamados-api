package com.davidparragamendoza.chamados.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name ="setor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Setor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String nome;

    @Column(nullable = false, unique = true, length = 50)
    private String sigla;

    @Column(nullable = false)
    private Boolean ativo = true;


}
