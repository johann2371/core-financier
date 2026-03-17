package com.corefi.controller;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
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
    public ResponseEntity<List<UtilisateurResponse>> findAll() {
        return ResponseEntity.ok(utilisateurService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurResponse> mettreAJour(@PathVariable Long id,
            @Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        utilisateurService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
