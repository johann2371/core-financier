package com.corefi.controller;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.service.interfaces.IFactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {

    private final IFactureService factureService;

    @GetMapping
    public ResponseEntity<List<FactureResponse>> findAll() {
        return ResponseEntity.ok(factureService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.findById(id));
    }

    @PostMapping
    public ResponseEntity<FactureResponse> creer(@Valid @RequestBody FactureCreateRequest request) {
        return ResponseEntity.ok(factureService.creer(request));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<FactureResponse> valider(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.valider(id));
    }
}
