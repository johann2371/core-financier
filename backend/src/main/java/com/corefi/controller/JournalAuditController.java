package com.corefi.controller;

import com.corefi.entity.JournalAudit;
import com.corefi.service.interfaces.IJournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journal-audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRATEUR')")
public class JournalAuditController {

    private final IJournalAuditService journalAuditService;

    @GetMapping
    public ResponseEntity<Page<JournalAudit>> findAll(Pageable pageable) {
        return ResponseEntity.ok(journalAuditService.findAll(pageable));
    }
}
