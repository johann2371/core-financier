package com.corefi.controller;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.service.interfaces.IFactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller des factures (Achat & Vente)
 */
@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {

    private final IFactureService factureService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG')")
    public ResponseEntity<List<FactureResponse>> findAll() {
        return ResponseEntity.ok(factureService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG')")
    public ResponseEntity<FactureResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE')")
    public ResponseEntity<FactureResponse> creer(@Valid @RequestBody FactureCreateRequest request) {
        return ResponseEntity.ok(factureService.creer(request));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','RESPONSABLE_FINANCIER')")
    public ResponseEntity<FactureResponse> valider(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.valider(id));
    }

    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','RESPONSABLE_FINANCIER')")
    public ResponseEntity<FactureResponse> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.annuler(id));
    }
}
