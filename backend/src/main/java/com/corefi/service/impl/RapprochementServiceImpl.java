package com.corefi.service.impl;

import com.corefi.entity.*;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IRapprochementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RapprochementServiceImpl implements IRapprochementService {

    private final RapprochementBancaireRepository rapprochementRepository;
    private final LigneReleveBancaireRepository ligneReleveRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final EncaissementRepository encaissementRepository;
    private final DecaissementRepository decaissementRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public RapprochementBancaire creerSession(Long compteId, String dateDebut, String dateFin, Double soldeInitial) {
        CompteFinancier compte = compteFinancierRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        RapprochementBancaire session = new RapprochementBancaire();
        session.setCompteFinancier(compte);
        session.setDateDebut(LocalDate.parse(dateDebut));
        session.setDateFin(LocalDate.parse(dateFin));
        session.setSoldeInitialReleve(BigDecimal.valueOf(soldeInitial));
        session.setDateCreation(LocalDateTime.now());
        session.setCreePar(getUtilisateurConnecte());
        
        return rapprochementRepository.save(session);
    }

    @Override
    @Transactional
    public List<LigneReleveBancaire> importerReleve(Long sessionId, MultipartFile file) {
        RapprochementBancaire session = rapprochementRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de rapprochement introuvable"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // Skip header
                
                String[] data = line.split(";");
                if (data.length < 3) continue;

                LigneReleveBancaire ligne = new LigneReleveBancaire();
                ligne.setRapprochement(session);
                ligne.setDateOperation(LocalDate.parse(data[0], DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                ligne.setLibelle(data[1]);
                
                BigDecimal montant = new BigDecimal(data[2].replace(",", ".").trim());
                if (montant.compareTo(BigDecimal.ZERO) >= 0) {
                    ligne.setCredit(montant);
                    ligne.setDebit(BigDecimal.ZERO);
                } else {
                    ligne.setDebit(montant.abs());
                    ligne.setCredit(BigDecimal.ZERO);
                }
                
                ligneReleveRepository.save(ligne);
            }
        } catch (Exception e) {
            throw new WorkflowException("Erreur lors de la lecture du fichier CSV : " + e.getMessage());
        }

        return ligneReleveRepository.findByRapprochementId(sessionId);
    }

    @Override
    @Transactional
    public void rapprochementAutomatique(Long sessionId) {
        RapprochementBancaire session = rapprochementRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable"));

        List<LigneReleveBancaire> lignes = ligneReleveRepository.findByRapprochementIdAndMatchedFalse(sessionId);
        
        for (LigneReleveBancaire ligne : lignes) {
            BigDecimal montant = ligne.getCredit().compareTo(BigDecimal.ZERO) > 0 ? ligne.getCredit() : ligne.getDebit();
            LocalDate date = ligne.getDateOperation();
            
            if (ligne.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                // Chercher un Encaissement correspondant
                Optional<Encaissement> match = encaissementRepository.findAll().stream()
                        .filter(e -> !e.isRapproche() && 
                                e.getMontant().compareTo(montant) == 0 &&
                                Math.abs(java.time.temporal.ChronoUnit.DAYS.between(e.getDateEncaissement(), date)) <= 3)
                        .findFirst();
                
                if (match.isPresent()) {
                    rapprocher(ligne, match.get());
                }
            } else {
                // Chercher un Decaissement correspondant
                Optional<Decaissement> match = decaissementRepository.findAll().stream()
                        .filter(d -> !d.isRapproche() && 
                                d.getMontant().compareTo(montant) == 0 &&
                                Math.abs(java.time.temporal.ChronoUnit.DAYS.between(d.getDateDecaissement(), date)) <= 3)
                        .findFirst();
                
                if (match.isPresent()) {
                    rapprocher(ligne, match.get());
                }
            }
        }
    }

    @Override
    @Transactional
    public void rapprocherManuellement(Long ligneId, Long transactionId, String typeTransaction) {
        LigneReleveBancaire ligne = ligneReleveRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de relevé introuvable"));
        
        if ("ENCAISSEMENT".equals(typeTransaction)) {
            Encaissement enc = encaissementRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Encaissement introuvable"));
            rapprocher(ligne, enc);
        } else {
            Decaissement dec = decaissementRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Décaissement introuvable"));
            rapprocher(ligne, dec);
        }
    }

    private void rapprocher(LigneReleveBancaire ligne, Object transaction) {
        ligne.setMatched(true);
        ligneReleveRepository.save(ligne);
        
        if (transaction instanceof Encaissement) {
            Encaissement e = (Encaissement) transaction;
            e.setRapproche(true);
            e.setDateRapprochement(LocalDate.now());
            encaissementRepository.save(e);
        } else if (transaction instanceof Decaissement) {
            Decaissement d = (Decaissement) transaction;
            d.setRapproche(true);
            d.setDateRapprochement(LocalDate.now());
            decaissementRepository.save(d);
        }
    }

    @Override
    @Transactional
    public void validerRapprochement(Long sessionId, Double soldeFinalReleve) {
        RapprochementBancaire session = rapprochementRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable"));
        
        session.setSoldeFinalReleve(BigDecimal.valueOf(soldeFinalReleve));
        session.setValide(true);
        session.setDateValidation(LocalDateTime.now());
        rapprochementRepository.save(session);
    }

    @Override
    public List<RapprochementBancaire> getSessionsParCompte(Long compteId) {
        return rapprochementRepository.findByCompteFinancierIdOrderByDateFinDesc(compteId);
    }

    @Override
    public List<LigneReleveBancaire> getLignesParSession(Long sessionId) {
        return ligneReleveRepository.findByRapprochementId(sessionId);
    }

    @Override
    public List<Object> getTransactionsNonRapprochees(Long compteId) {
        java.util.List<Object> transactions = new java.util.ArrayList<>();
        transactions.addAll(encaissementRepository.findAll().stream()
                .filter(e -> !e.isRapproche() && e.getCompteFinancier().getId().equals(compteId))
                .collect(java.util.stream.Collectors.toList()));
        transactions.addAll(decaissementRepository.findAll().stream()
                .filter(d -> !d.isRapproche() && d.getCompteFinancier().getId().equals(compteId))
                .collect(java.util.stream.Collectors.toList()));
        return transactions;
    }

    private Utilisateur getUtilisateurConnecte() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email).orElse(null);
    }
}
