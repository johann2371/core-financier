
package com.corefi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreFinancierApplication {

    public static void main(String[] args) {
        // 1. Essayer de charger depuis la racine
        Dotenv rootEnv = Dotenv.configure().directory("./").ignoreIfMissing().load();
        rootEnv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        // 2. Essayer de charger depuis le dossier backend/ (prioritaire ou complémentaire)
        Dotenv backendEnv = Dotenv.configure().directory("./backend").ignoreIfMissing().load();
        backendEnv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        // Vérification
        if (System.getProperty("JWT_SECRET") != null) {
            System.out.println("JWT_SECRET configuré avec succès.");
        } else {
            System.err.println(" Erreur : JWT_SECRET introuvable à la racine ou dans /backend !");
            System.err.println("Vérifiez la présence de JWT_SECRET dans : " + System.getProperty("user.dir") + "\\backend\\.env");
        }

        SpringApplication.run(CoreFinancierApplication.class, args);
    }

}
