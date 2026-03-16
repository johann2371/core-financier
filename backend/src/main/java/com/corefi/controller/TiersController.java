package com.corefi.controller;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.service.interfaces.ITiersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class TiersController {

    private final ITiersService tiersService;

    @GetMapping
    public ResponseEntity<List<TiersResponse>> findAll() {
        return ResponseEntity.ok(tiersService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TiersResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tiersService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TiersResponse> creer(@Valid @RequestBody TiersCreateRequest request) {
        return ResponseEntity.ok(tiersService.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TiersResponse> mettreAJour(@PathVariable Long id,
            @Valid @RequestBody TiersCreateRequest request) {
        return ResponseEntity.ok(tiersService.mettreAJour(id, request));
    }
}
