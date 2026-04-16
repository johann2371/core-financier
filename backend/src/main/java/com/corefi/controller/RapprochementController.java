package com.corefi.controller;

import com.corefi.entity.LigneReleveBancaire;
import com.corefi.entity.RapprochementBancaire;
import com.corefi.service.interfaces.IRapprochementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/rapprochements")
@RequiredArgsConstructor
public class RapprochementController {

    private final IRapprochementService rapprochementService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<RapprochementBancaire> creerSession(
            @RequestParam Long compteId,
            @RequestParam String dateDebut,
            @RequestParam String dateFin,
            @RequestParam Double soldeInitial) {
        return ResponseEntity.ok(rapprochementService.creerSession(compteId, dateDebut, dateFin, soldeInitial));
    }

    @PostMapping("/sessions/{id}/importer")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<List<LigneReleveBancaire>> importerReleve(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(rapprochementService.importerReleve(id, file));
    }

    @PostMapping("/sessions/{id}/auto-match")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<Void> autoMatch(@PathVariable Long id) {
        rapprochementService.rapprochementAutomatique(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/match-manuel")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<Void> matchManuel(
            @RequestParam Long ligneId,
            @RequestParam Long transactionId,
            @RequestParam String typeTransaction) {
        rapprochementService.rapprocherManuellement(ligneId, transactionId, typeTransaction);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{id}/valider")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<Void> valider(@PathVariable Long id, @RequestParam Double soldeFinal) {
        rapprochementService.validerRapprochement(id, soldeFinal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{id}/lignes")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<List<LigneReleveBancaire>> getLignes(@PathVariable Long id) {
        return ResponseEntity.ok(rapprochementService.getLignesParSession(id));
    }

    @GetMapping("/transactions/compte/{compteId}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<List<Object>> getTransactionsNonRapprochees(@PathVariable Long compteId) {
        return ResponseEntity.ok(rapprochementService.getTransactionsNonRapprochees(compteId));
    }

    @GetMapping("/compte/{compteId}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER')")
    public ResponseEntity<List<RapprochementBancaire>> getSessions(@PathVariable Long compteId) {
        return ResponseEntity.ok(rapprochementService.getSessionsParCompte(compteId));
    }
}
