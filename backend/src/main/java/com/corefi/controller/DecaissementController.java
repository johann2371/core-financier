package com.corefi.controller;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.service.interfaces.IDecaissementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decaissements")
@RequiredArgsConstructor
public class DecaissementController {

    private final IDecaissementService decaissementService;
    private final com.corefi.service.interfaces.IPdfService pdfService;

    @GetMapping("/{id}/recu/pdf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<byte[]> genererRecuPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.genererRecuDecaissementPdf(id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "bon_decaissement_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<List<DecaissementResponse>> findAll() {
        return ResponseEntity.ok(decaissementService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<DecaissementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(decaissementService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE')")
    public ResponseEntity<DecaissementResponse> creer(@Valid @RequestBody DecaissementCreateRequest request) {
        return ResponseEntity.ok(decaissementService.creer(request));
    }

    @PutMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE')")
    public ResponseEntity<DecaissementResponse> soumettre(@PathVariable Long id) {
        return ResponseEntity.ok(decaissementService.soumettre(id));
    }

    @PutMapping("/{id}/valider-rf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<DecaissementResponse> validerRF(@PathVariable Long id,
            @Valid @RequestBody ValidationRFRequest request) {
        return request.isRejeter()
                ? ResponseEntity.ok(decaissementService.rejeterRF(id, request))
                : ResponseEntity.ok(decaissementService.validerRF(id, request));
    }

    @PutMapping("/{id}/approuver-pdg")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'PDG')")
    public ResponseEntity<DecaissementResponse> approuverPDG(@PathVariable Long id,
            @Valid @RequestBody ApprobationPDGRequest request) {
        return request.isRejeter()
                ? ResponseEntity.ok(decaissementService.rejeterPDG(id, request))
                : ResponseEntity.ok(decaissementService.approuverPDG(id, request));
    }

    @PutMapping("/{id}/executer")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'CAISSIER')")
    public ResponseEntity<DecaissementResponse> executer(@PathVariable Long id,
            @Valid @RequestBody ExecutionCaissierRequest request) {
        return ResponseEntity.ok(decaissementService.executer(id, request));
    }
}
