package com.corefi.controller;

import com.corefi.entity.CompteFinancier;
import com.corefi.service.interfaces.ICompteFinancierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptes-financiers")
@RequiredArgsConstructor
public class CompteFinancierController {

    private final ICompteFinancierService compteFinancierService;

    @GetMapping
    public ResponseEntity<List<CompteFinancier>> findAll() {
        return ResponseEntity.ok(compteFinancierService.findAll());
    }
}
