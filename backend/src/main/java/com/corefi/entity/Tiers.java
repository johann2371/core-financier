package com.corefi.entity;

import com.corefi.enums.TypeTiers;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tiers")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tiers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeTiers type;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String raisonSociale;

    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String pays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_defaut_id")
    private Devise deviseDefaut;

    private boolean actif = true;
}
