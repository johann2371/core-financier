package com.corefi.controller;

import com.corefi.service.interfaces.ITableauBordService;
import com.corefi.service.interfaces.IRapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
public class RapportController {

    private final IRapportService rapportService;

    @GetMapping("/bilan-mensuel")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'PDG', 'RESPONSABLE_FINANCIER', 'COMPTABLE')")
    public ResponseEntity<Map<String, Object>> getBilanMensuel(
            @RequestParam int mois, @RequestParam int annee) {
        return ResponseEntity.ok(rapportService.genererBilanMensuel(mois, annee));
    }

    @GetMapping(value = "/bilan-mensuel/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'PDG', 'RESPONSABLE_FINANCIER', 'COMPTABLE')")
    public ResponseEntity<byte[]> getBilanMensuelPdf(
            @RequestParam int mois, @RequestParam int annee) {
        byte[] pdf = rapportService.genererBilanMensuelPdf(mois, annee);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "bilan_" + mois + "_" + annee + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
