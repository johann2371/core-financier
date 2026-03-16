package com.corefi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, VALIDATE, APPROVE, EXECUTE, REJECT

    private String entite;
    private Long entiteId;

    @Column(columnDefinition = "TEXT")
    private String anciennesValeurs; // JSON

    @Column(columnDefinition = "TEXT")
    private String nouvellesValeurs; // JSON

    private String adresseIp;

    @Column(nullable = false)
    private LocalDateTime dateAction;

    @PrePersist
    public void prePersist() {
        dateAction = LocalDateTime.now();
    }
}
