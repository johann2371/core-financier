package com.corefi.controller;

import com.corefi.dto.request.sessioncaisse.SessionCaisseFermetureRequest;
import com.corefi.dto.request.sessioncaisse.SessionCaisseRequest;
import com.corefi.dto.response.sessioncaisse.SessionCaisseResponse;
import com.corefi.service.interfaces.ISessionCaisseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions-caisse")
@RequiredArgsConstructor
public class SessionCaisseController {

    private final ISessionCaisseService sessionCaisseService;
    private final com.corefi.service.interfaces.IPdfService pdfService;

    @PostMapping("/ouvrir")
    @PreAuthorize("hasAuthority('CAISSIER')")
    public ResponseEntity<SessionCaisseResponse> ouvrir(@Valid @RequestBody SessionCaisseRequest request) {
        return ResponseEntity.ok(sessionCaisseService.ouvrirSession(request));
    }

    @PostMapping("/{id}/fermer")
    @PreAuthorize("hasAuthority('CAISSIER')")
    public ResponseEntity<SessionCaisseResponse> fermer(@PathVariable Long id, @Valid @RequestBody SessionCaisseFermetureRequest request) {
        return ResponseEntity.ok(sessionCaisseService.fermerSession(id, request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CAISSIER')")
    public ResponseEntity<SessionCaisseResponse> getActive() {
        return sessionCaisseService.getSessionActiveCurrentCaissier()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/historique/{caissierId}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'RESPONSABLE_FINANCIER', 'PDG')")
    public ResponseEntity<List<SessionCaisseResponse>> getHistorique(@PathVariable Long caissierId) {
        return ResponseEntity.ok(sessionCaisseService.getHistoriqueSessionsCaissier(caissierId));
    }

    @GetMapping("/mon-historique")
    @PreAuthorize("hasAuthority('CAISSIER')")
    public ResponseEntity<List<SessionCaisseResponse>> getMonHistorique() {
        return ResponseEntity.ok(sessionCaisseService.getHistoriqueSessionsCurrentCaissier());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionCaisseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionCaisseService.getById(id));
    }

    @GetMapping("/{id}/rapport-pdf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<byte[]> genererRapportPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.genererRapportCloturePdf(id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline().filename("rapport_cloture_" + id + ".pdf").build());
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
