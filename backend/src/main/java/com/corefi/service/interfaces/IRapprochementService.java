package com.corefi.service.interfaces;

import com.corefi.entity.RapprochementBancaire;
import com.corefi.entity.LigneReleveBancaire;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface IRapprochementService {
    RapprochementBancaire creerSession(Long compteId, String dateDebut, String dateFin, Double soldeInitial);
    List<LigneReleveBancaire> importerReleve(Long sessionId, MultipartFile file);
    void rapprochementAutomatique(Long sessionId);
    void rapprocherManuellement(Long ligneId, Long transactionId, String typeTransaction);
    void validerRapprochement(Long sessionId, Double soldeFinalReeve);
    List<RapprochementBancaire> getSessionsParCompte(Long compteId);
    List<LigneReleveBancaire> getLignesParSession(Long sessionId);
    List<Object> getTransactionsNonRapprochees(Long compteId);
}
