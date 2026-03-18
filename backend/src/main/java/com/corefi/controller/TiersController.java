package com.corefi.controller;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.service.interfaces.ITiersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ÉTAPE 3 — Controller : dépend UNIQUEMENT de ITiersService (interface)
 * Jamais de TiersServiceImpl directement (règle AGENTS.md)
 */
@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class TiersController {

    private final ITiersService tiersService; // ← interface ✅

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG','CAISSIER')")
    public ResponseEntity<List<TiersResponse>> findAll() {
        return ResponseEntity.ok(tiersService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG','CAISSIER')")
    public ResponseEntity<TiersResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tiersService.findById(id));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG','CAISSIER')")
    public ResponseEntity<List<TiersResponse>> findByType(@PathVariable String type) {
        return ResponseEntity.ok(tiersService.findByType(type));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE')")
    public ResponseEntity<TiersResponse> creer(@Valid @RequestBody TiersCreateRequest request) {
        return ResponseEntity.ok(tiersService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE')")
    public ResponseEntity<TiersResponse> mettreAJour(@PathVariable Long id,
            @Valid @RequestBody TiersCreateRequest request) {
        return ResponseEntity.ok(tiersService.mettreAJour(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        tiersService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
