package com.corefi.controller;

import com.corefi.dto.response.ocr.FactureExtractionResult;
import com.corefi.service.interfaces.IOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final IOcrService ocrService;

    @PostMapping("/factures")
    @PreAuthorize("hasAnyAuthority('COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG')")
    public ResponseEntity<FactureExtractionResult> extraireFacture(@RequestParam("file") MultipartFile file) {
        FactureExtractionResult result = ocrService.extraireDonnees(file);
        return ResponseEntity.ok(result);
    }
}
