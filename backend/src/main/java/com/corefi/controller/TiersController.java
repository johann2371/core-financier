package com.corefi.controller;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.service.interfaces.ITiersService;
import com.corefi.service.interfaces.IFileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ÉTAPE 3 — Controller : dépend UNIQUEMENT de ITiersService (interface)
 * Jamais de TiersServiceImpl directement (règle AGENTS.md)
 */
@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class TiersController {

    private final ITiersService tiersService;
    private final IFileStorageService fileStorageService;

    @PostMapping("/upload-photo")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE')")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file, "tiers");
        
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();

        Map<String, String> response = new HashMap<>();
        response.put("url", fileDownloadUri);
        return ResponseEntity.ok(response);
    }

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

    // ═══════════════════════════════════════════════════════════
    // RELEVÉ DE COMPTE TIERS (PDF)
    // ═══════════════════════════════════════════════════════════
    private final com.corefi.service.interfaces.IPdfService pdfService;

    @GetMapping("/{id}/releve-pdf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','COMPTABLE','RESPONSABLE_FINANCIER','PDG')")
    public ResponseEntity<byte[]> telechargerReleve(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.genererReleveCompteTiersPdf(id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline().filename("releve_tiers_" + id + ".pdf").build());
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}

