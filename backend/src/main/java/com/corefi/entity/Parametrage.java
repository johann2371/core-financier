package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "parametrage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parametrage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cle;

    @Column(nullable = false)
    private String valeur;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par")
    private Utilisateur modifiePar;
}
