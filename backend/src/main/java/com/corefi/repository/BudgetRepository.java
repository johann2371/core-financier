package com.corefi.repository;

import com.corefi.entity.Budget;
import com.corefi.enums.CategorieDecaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByAnnee(int annee);
    List<Budget> findByAnneeAndMois(int annee, int mois);
    Optional<Budget> findByCategorieAndAnneeAndMois(CategorieDecaissement categorie, int annee, int mois);
}
