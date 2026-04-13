package com.corefi.controller;

import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.service.interfaces.IEncaissementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encaissements")
@RequiredArgsConstructor
public class EncaissementController {

    private final IEncaissementService encaissementService;
    private final com.corefi.service.interfaces.IPdfService pdfService;

    @GetMapping("/{id}/recu/pdf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<byte[]> genererRecuPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.genererRecuEncaissementPdf(id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline().filename("recu_encaissement_" + id + ".pdf").build());
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<List<EncaissementResponse>> findAll() {
        return ResponseEntity.ok(encaissementService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<EncaissementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(encaissementService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'CAISSIER')")
    public ResponseEntity<EncaissementResponse> creer(@Valid @RequestBody EncaissementCreateRequest request) {
        return ResponseEntity.ok(encaissementService.creer(request));
    }

    @PostMapping("/{id}/affecter")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'CAISSIER')")
    public ResponseEntity<Void> affecter(@PathVariable Long id, @Valid @RequestBody List<com.corefi.dto.request.encaissement.AffectationRequest> affectations) {
        encaissementService.affecter(id, affectations);
        return ResponseEntity.ok().build();
    }
}
