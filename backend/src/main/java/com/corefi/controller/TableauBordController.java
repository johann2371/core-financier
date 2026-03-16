package com.corefi.controller;

import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.service.interfaces.ITableauBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tableau-bord")
@RequiredArgsConstructor
public class TableauBordController {

    private final ITableauBordService tableauBordService;

    @GetMapping("/kpis")
    public ResponseEntity<TableauBordResponse> getKpis() {
        return ResponseEntity.ok(tableauBordService.getKpis());
    }
}
