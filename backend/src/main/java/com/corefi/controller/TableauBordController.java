package com.corefi.controller;

import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.service.interfaces.ITableauBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tableau-bord")
@RequiredArgsConstructor
public class TableauBordController {

    private final ITableauBordService tableauBordService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'RESPONSABLE_FINANCIER', 'PDG', 'COMPTABLE', 'CAISSIER')")
    public ResponseEntity<TableauBordResponse> getKpis(@RequestParam(defaultValue = "30") int forecastDays) {
        return ResponseEntity.ok(tableauBordService.getKpis(forecastDays));
    }

    @PutMapping("/seuil")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR', 'PDG')")
    public ResponseEntity<Void> updateSeuil(@RequestParam BigDecimal montant) {
        tableauBordService.updateSeuil(montant);
        return ResponseEntity.ok().build();
    }
}
