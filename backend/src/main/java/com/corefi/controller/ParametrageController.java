package com.corefi.controller;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;
import com.corefi.service.interfaces.IParametrageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parametrage")
@RequiredArgsConstructor
public class ParametrageController {

    private final IParametrageService parametrageService;

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
}
