package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "devise")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Devise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 3)
    private String code; // XAF, EUR, USD

    @Column(nullable = false)
    private String libelle;

    private String symbole;
    private boolean deviseBase = false;
    private boolean actif = true;
}
