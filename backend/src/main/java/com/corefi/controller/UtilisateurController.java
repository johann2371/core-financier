package com.corefi.controller;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.entity.Utilisateur;
import com.corefi.service.interfaces.IUtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRATEUR')")
public class UtilisateurController {

    private final IUtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<List<Utilisateur>> findAll() {
        return ResponseEntity.ok(utilisateurService.findAll());
    }

    @PostMapping
    public ResponseEntity<Utilisateur> creer(@Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> mettreAJour(@PathVariable Long id,
            @Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.mettreAJour(id, request));
    }
}
