package com.corefi.controller;

import com.corefi.dto.response.compte.CompteFinancierResponse;
import com.corefi.service.interfaces.ICompteFinancierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptes-financiers")
@RequiredArgsConstructor
public class CompteFinancierController {

    private final ICompteFinancierService compteFinancierService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<List<CompteFinancierResponse>> findAll() {
        return ResponseEntity.ok(compteFinancierService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<CompteFinancierResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(compteFinancierService.findById(id));
    }
}
