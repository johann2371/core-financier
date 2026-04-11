package com.corefi.controller;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;
import com.corefi.service.interfaces.IFileStorageService;
import com.corefi.service.interfaces.IParametrageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parametrage")
@RequiredArgsConstructor
public class ParametrageController {

    private final IParametrageService parametrageService;
    private final IFileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'PDG')")
    public ResponseEntity<List<ParametrageResponse>> getAll() {
        return ResponseEntity.ok(parametrageService.getAll());
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<ParametrageResponse> update(@PathVariable String cle, @Valid @RequestBody ParametrageUpdateRequest request) {
        return ResponseEntity.ok(parametrageService.update(cle, request));
    }

    @PostMapping("/logo/{type}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> uploadLogo(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        // Stocker le fichier dans le sous-dossier "logos"
        String relativePath = fileStorageService.storeFile(file, "logos");
        
        // Déterminer la clé de paramétrage
        String cle = "app".equalsIgnoreCase(type) ? "APP_LOGO_URL" : "INVOICE_LOGO_URL";
        
        // Mettre à jour le paramètre en BDD
        ParametrageUpdateRequest updateReq = new ParametrageUpdateRequest();
        updateReq.setValeur("/uploads/" + relativePath);
        parametrageService.update(cle, updateReq);
        
        return ResponseEntity.ok(Map.of("url", "/uploads/" + relativePath));
    }
}
