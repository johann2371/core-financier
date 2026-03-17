package com.corefi.dto.response.utilisateur;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UtilisateurResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dernierAcces;
}
