package com.corefi.controller;

import com.corefi.dto.response.search.SearchResultDTO;
import com.corefi.service.interfaces.IGlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final IGlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<List<SearchResultDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(globalSearchService.search(query));
    }
}
