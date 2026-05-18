package com.corefi.config;

import com.corefi.entity.Utilisateur;
import com.corefi.entity.Devise;
import com.corefi.entity.CompteFinancier;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.repository.DeviseRepository;
import com.corefi.repository.CompteFinancierRepository;
import com.corefi.enums.Role;
import com.corefi.enums.TypeCompte;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Initialise les données au démarrage de l'application.
 * Crée l'administrateur par défaut si la table utilisateur est vide.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviseRepository deviseRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final com.corefi.repository.TiersRepository tiersRepository;
    private final com.corefi.repository.FactureRepository factureRepository;
    private final com.corefi.repository.EncaissementRepository encaissementRepository;
    private final com.corefi.repository.DecaissementRepository decaissementRepository;

    @Override
    public void run(String... args) {
        creerAdminParDefaut();
        creerComptableParDefaut();
        creerRFParDefaut();
        creerDeviseEtCompteParDefaut();
        creerDonneesTestDashboard();
        synchroniserDettesHistoriques();
    }

    private void synchroniserDettesHistoriques() {
        log.info("--- Début synchronisation des dettes historiques ---");
        tiersRepository.findAll().forEach(tiers -> {
            // Cumul factures (Dette Totale)
            BigDecimal totalFactures = factureRepository.findByTiersId(tiers.getId()).stream()
                    .filter(f -> f.getStatut() != com.corefi.enums.StatutFacture.ANNULEE)
                    .map(com.corefi.entity.Facture::getMontantTtc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Cumul règlements
            BigDecimal totalReglements = BigDecimal.ZERO;
            if (tiers.getType() == com.corefi.enums.TypeTiers.CLIENT) {
                totalReglements = encaissementRepository.findByClientId(tiers.getId()).stream()
                        .filter(e -> e.getStatut() != com.corefi.enums.StatutEncaissement.ANNULEE)
                        .map(com.corefi.entity.Encaissement::getMontant)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                totalReglements = decaissementRepository.findByFournisseurId(tiers.getId()).stream()
                        .filter(d -> d.getStatut() != com.corefi.enums.StatutDecaissement.ANNULEE)
                        .map(com.corefi.entity.Decaissement::getMontant)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal nouveauSolde = totalFactures.subtract(totalReglements);
            if (nouveauSolde.compareTo(BigDecimal.ZERO) < 0) nouveauSolde = BigDecimal.ZERO;

            tiers.setTotalDette(totalFactures);
            tiers.setSolde(nouveauSolde);
            tiersRepository.save(tiers);
            log.info(" Tier {} : Total {} | Solde {}", tiers.getRaisonSociale(), totalFactures, nouveauSolde);
        });
        log.info("--- Fin synchronisation des dettes historiques ---");
    }

    private void creerAdminParDefaut() {
        // Si l'admin existe déjà → mettre à jour le mot de passe pour corriger un éventuel mauvais hash
        utilisateurRepository.findByEmail("admin@corefi.cm").ifPresentOrElse(
            admin -> {
                admin.setPassword(passwordEncoder.encode("Password123!"));
                admin.setActif(true);
                admin.setTentativesConnexion(0);
                admin.setBloqueJusqua(null);
                utilisateurRepository.save(admin);
                log.info(" Admin déjà présent ");
            },
            () -> {
                Utilisateur admin = new Utilisateur();
                admin.setNom("Admin");
                admin.setPrenom("Système");
                admin.setEmail("admin@corefi.cm");
                admin.setPassword(passwordEncoder.encode("Password123!"));
                admin.setRole(Role.ADMINISTRATEUR);
                admin.setActif(true);
                admin.setTentativesConnexion(0);
                admin.setDateCreation(LocalDateTime.now());
                utilisateurRepository.save(admin);
                log.info(" Admin créé : admin@corefi.cm ");
            }
        );
    }

    private void creerComptableParDefaut() {
        utilisateurRepository.findByEmail("jgouaffo@gmail.com").ifPresentOrElse(
            comptable -> {
                comptable.setPassword(passwordEncoder.encode("Dorcasbemmo123*"));
                comptable.setActif(true);
                comptable.setTentativesConnexion(0);
                comptable.setBloqueJusqua(null);
                utilisateurRepository.save(comptable);
                log.info(" Comptable déjà présent, mis à jour ");
            },
            () -> {
                Utilisateur comptable = new Utilisateur();
                comptable.setNom("Gouaffo");
                comptable.setPrenom("Johann");
                comptable.setEmail("jgouaffo@gmail.com");
                comptable.setPassword(passwordEncoder.encode("Dorcasbemmo123*"));
                comptable.setRole(Role.COMPTABLE);
                comptable.setActif(true);
                comptable.setTentativesConnexion(0);
                comptable.setDateCreation(LocalDateTime.now());
                utilisateurRepository.save(comptable);
                log.info(" Comptable créé : jgouaffo@gmail.com ");
            }
        );
    }

    private void creerRFParDefaut() {
        utilisateurRepository.findByEmail("gouaffojohann5@gmail.com").ifPresentOrElse(
            rf -> {
                rf.setPassword(passwordEncoder.encode("Dorcasbemmo123*"));
                rf.setActif(true);
                rf.setTentativesConnexion(0);
                rf.setBloqueJusqua(null);
                rf.setRole(Role.RESPONSABLE_FINANCIER);
                utilisateurRepository.save(rf);
                log.info(" Responsable Financier déjà présent, mis à jour ");
            },
            () -> {
                Utilisateur rf = new Utilisateur();
                rf.setNom("Gouaffo");
                rf.setPrenom("Albert");
                rf.setEmail("gouaffojohann5@gmail.com");
                rf.setPassword(passwordEncoder.encode("Dorcasbemmo123*"));
                rf.setRole(Role.RESPONSABLE_FINANCIER);
                rf.setActif(true);
                rf.setTentativesConnexion(0);
                rf.setDateCreation(LocalDateTime.now());
                utilisateurRepository.save(rf);
                log.info(" Responsable Financier créé : gouaffojohann5@gmail.com ");
            }
        );
    }

    private void creerDonneesTestDashboard() {
        if (tiersRepository.count() == 0) {
            com.corefi.entity.Tiers client = new com.corefi.entity.Tiers();
            client.setRaisonSociale("Client Test");
            client.setCode("C-TEST-001");
            client.setType(com.corefi.enums.TypeTiers.CLIENT);
            client.setActif(true);
            tiersRepository.save(client);

            com.corefi.entity.Tiers fournisseur = new com.corefi.entity.Tiers();
            fournisseur.setRaisonSociale("Fournisseur Test");
            fournisseur.setCode("F-TEST-001");
            fournisseur.setType(com.corefi.enums.TypeTiers.FOURNISSEUR);
            fournisseur.setActif(true);
            tiersRepository.save(fournisseur);

            if (factureRepository.count() == 0) {
                // Facture VENTE
                com.corefi.entity.Facture fVente = new com.corefi.entity.Facture();
                fVente.setNumero("F-VENTE-001");
                fVente.setType("VENTE");
                fVente.setTiers(client);
                fVente.setMontantHt(new BigDecimal("1000000.00"));
                fVente.setMontantTtc(new BigDecimal("1192500.00"));
                fVente.setStatut(com.corefi.enums.StatutFacture.VALIDEE);
                fVente.setDateFacture(LocalDateTime.now().toLocalDate());
                factureRepository.save(fVente);

                // Facture ACHAT
                com.corefi.entity.Facture fAchat = new com.corefi.entity.Facture();
                fAchat.setNumero("F-ACHAT-001");
                fAchat.setType("ACHAT");
                fAchat.setTiers(fournisseur);
                fAchat.setMontantHt(new BigDecimal("500000.00"));
                fAchat.setMontantTtc(new BigDecimal("596250.00"));
                fAchat.setStatut(com.corefi.enums.StatutFacture.EN_ATTENTE_PAIEMENT);
                fAchat.setDateFacture(LocalDateTime.now().toLocalDate());
                factureRepository.save(fAchat);
                
                log.info("Données de test créées : 1 facture VENTE, 1 facture ACHAT.");
            }
        }
    }

    private void creerDeviseEtCompteParDefaut() {
        if (deviseRepository.count() == 0) {
            Devise d = new Devise();
            d.setCode("XAF");
            d.setLibelle("Franc CFA BEAC");
            d.setSymbole("FCFA");
            d.setDeviseBase(true);
            d.setActif(true);
            deviseRepository.save(d);
            log.info(" Devise XAF créée ");
        }

        if (compteFinancierRepository.count() == 0) {
            Devise d = deviseRepository.findAll().get(0);
            CompteFinancier compte = new CompteFinancier();
            compte.setType(TypeCompte.BANQUE);
            compte.setNumero("CM-100-BANQUE-1");
            compte.setLibelle("Banque Principale");
            compte.setSolde(new BigDecimal("14250000.00"));
            compte.setDevise(d);
            compte.setActif(true);
            compteFinancierRepository.save(compte);
            log.info(" Compte Financier principal créé (14 250 000 XAF) ");
        }
    }
}
