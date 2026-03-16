package com.corefi.dto.request.utilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UtilisateurCreateRequest {
    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    @Email
    private String email;

    private String password; // Optionnel en création (généré?), requis sinon

    @NotBlank
    private String role;
}
