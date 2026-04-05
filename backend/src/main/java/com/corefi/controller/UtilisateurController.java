package com.corefi.controller;

import com.corefi.dto.request.utilisateur.ProfileUpdateRequest;
import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import com.corefi.service.interfaces.IFileStorageService;
import com.corefi.service.interfaces.IUtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final IUtilisateurService utilisateurService;
    private final IFileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<List<UtilisateurResponse>> findAll() {
        return ResponseEntity.ok(utilisateurService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<UtilisateurResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<UtilisateurResponse> mettreAJour(@PathVariable Long id,
            @Valid @RequestBody UtilisateurCreateRequest request) {
        return ResponseEntity.ok(utilisateurService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        utilisateurService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactiver")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<Void> reactiver(@PathVariable Long id) {
        utilisateurService.reactiver(id);
        return ResponseEntity.noContent().build();
    }

    /** Mettre à jour son propre profil (Accessible à tout utilisateur connecté) */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilisateurResponse> updateProfile(Principal principal, @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(utilisateurService.updateProfile(principal.getName(), request));
    }

    /** Importer une photo de profil */
    @PostMapping("/profile/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilisateurResponse> uploadProfilePhoto(Principal principal, @RequestParam("file") MultipartFile file) {
        // 1. Stocker le fichier (retourne un chemin relatif ex: profiles/uuid.jpg)
        String fileName = fileStorageService.storeProfilePhoto(file);
        
        // 2. Récupérer les infos actuelles
        UtilisateurResponse current = utilisateurService.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(principal.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 3. Mettre à jour uniquement la photo (on stocke le chemin relatif)
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setNom(current.getNom());
        updateRequest.setPrenom(current.getPrenom());
        updateRequest.setEmail(current.getEmail());
        updateRequest.setPhotoUrl(fileName);

        return ResponseEntity.ok(utilisateurService.updateProfile(principal.getName(), updateRequest));
    }
}
