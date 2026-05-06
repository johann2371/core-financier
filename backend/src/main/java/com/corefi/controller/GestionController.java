package com.corefi.controller;

import com.corefi.entity.Budget;
import com.corefi.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller pour la gestion budgétaire et les factures récurrentes.
 */
@RestController
@RequestMapping("/api/gestion")
@RequiredArgsConstructor
public class GestionController {

    private final BudgetRepository budgetRepository;

    // ═══════════════════════════════════════════════════════════
    // BUDGETS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/budgets")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','RESPONSABLE_FINANCIER','PDG')")
    public ResponseEntity<List<Budget>> getAllBudgets(@RequestParam(defaultValue = "0") int annee) {
        int a = annee > 0 ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(budgetRepository.findByAnnee(a));
    }

    @PostMapping("/budgets")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','RESPONSABLE_FINANCIER')")
    public ResponseEntity<Budget> creerBudget(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetRepository.save(budget));
    }

    @PutMapping("/budgets/{id}")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATEUR','RESPONSABLE_FINANCIER')")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        Budget existing = budgetRepository.findById(id).orElseThrow(() -> 
            new com.corefi.exception.ResourceNotFoundException("Budget introuvable ID: " + id));
        existing.setMontantPlafond(budget.getMontantPlafond());
        existing.setSeuilAlertePourcent(budget.getSeuilAlertePourcent());
        existing.setAlerteEnvoyee(false); // reset alerte si on change le plafond
        return ResponseEntity.ok(budgetRepository.save(existing));
    }

    @DeleteMapping("/budgets/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
