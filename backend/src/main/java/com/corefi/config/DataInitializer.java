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

    @Override
    public void run(String... args) {
        creerAdminParDefaut();
        creerComptableParDefaut();
        creerDeviseEtCompteParDefaut();
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
