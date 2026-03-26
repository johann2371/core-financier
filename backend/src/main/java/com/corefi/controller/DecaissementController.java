package com.corefi.controller;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.entity.Decaissement;
import com.corefi.entity.JustificatifDecaissement;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.JustificatifRepository;
import com.corefi.service.interfaces.IDecaissementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/decaissements")
@RequiredArgsConstructor
public class DecaissementController {

    private final IDecaissementService decaissementService;
    private final com.corefi.service.interfaces.IPdfService pdfService;
    private final DecaissementRepository decaissementRepository;
    private final JustificatifRepository justificatifRepository;

    @Value("${app.upload.dir:uploads/justificatifs}")
    private String uploadDir;

    @GetMapping("/{id}/recu/pdf")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<byte[]> genererRecuPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.genererRecuDecaissementPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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

    // ═══════════════════════════════════════════════════════════════
    // JUSTIFICATIFS : Upload, Listing, Download
    // ═══════════════════════════════════════════════════════════════

    /** Upload un ou plusieurs justificatifs pour un décaissement */
    @PostMapping("/{id}/justificatifs")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE')")
    public ResponseEntity<Map<String, Object>> uploadJustificatifs(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        Decaissement decaissement = decaissementRepository.findById(id)
                .orElseThrow(() -> new com.corefi.exception.ResourceNotFoundException("Décaissement introuvable : " + id));

        // Créer le répertoire de stockage s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<Map<String, Object>> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // Générer un nom unique pour éviter les collisions
            String ext = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID() + ext;

            // Sauvegarder sur le disque
            Path filePath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Sauvegarder en BDD
            JustificatifDecaissement j = new JustificatifDecaissement();
            j.setNomFichier(storedName);
            j.setNomOriginal(originalName);
            j.setTypeFichier(file.getContentType());
            j.setTailleFichier(file.getSize());
            j.setDateUpload(LocalDateTime.now());
            j.setDecaissement(decaissement);
            justificatifRepository.save(j);

            Map<String, Object> info = new HashMap<>();
            info.put("id", j.getId());
            info.put("nomOriginal", originalName);
            info.put("taille", file.getSize());
            uploaded.add(info);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", uploaded.size() + " fichier(s) uploadé(s)");
        result.put("fichiers", uploaded);
        return ResponseEntity.ok(result);
    }

    /** Lister les justificatifs d'un décaissement */
    @GetMapping("/{id}/justificatifs")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<List<Map<String, Object>>> listJustificatifs(@PathVariable Long id) {
        List<JustificatifDecaissement> justificatifs = justificatifRepository.findByDecaissementId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (JustificatifDecaissement j : justificatifs) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", j.getId());
            info.put("nomOriginal", j.getNomOriginal());
            info.put("typeFichier", j.getTypeFichier());
            info.put("tailleFichier", j.getTailleFichier());
            info.put("dateUpload", j.getDateUpload());
            result.add(info);
        }
        return ResponseEntity.ok(result);
    }

    /** Télécharger un justificatif par son ID */
    @GetMapping("/justificatifs/{fileId}/download")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'CAISSIER')")
    public ResponseEntity<Resource> downloadJustificatif(@PathVariable Long fileId) throws IOException {
        JustificatifDecaissement j = justificatifRepository.findById(fileId)
                .orElseThrow(() -> new com.corefi.exception.ResourceNotFoundException("Justificatif introuvable : " + fileId));

        Path filePath = Paths.get(uploadDir).resolve(j.getNomFichier());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new com.corefi.exception.ResourceNotFoundException("Fichier introuvable sur le serveur");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(j.getTypeFichier() != null ? j.getTypeFichier() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + j.getNomOriginal() + "\"")
                .body(resource);
    }
}
