package com.corefi.config;

import com.corefi.entity.Utilisateur;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.enums.Role;
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

    @Override
    public void run(String... args) {
        creerAdminParDefaut();
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
}
