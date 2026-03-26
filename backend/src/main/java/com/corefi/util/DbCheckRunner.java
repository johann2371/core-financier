package com.corefi.util;

import com.corefi.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbCheckRunner implements CommandLineRunner {
    private final CompteFinancierRepository compteFinancierRepository;
    private final FactureRepository factureRepository;
    private final TiersRepository tiersRepository;
    private final DecaissementRepository decaissementRepository;

    @Override
    public void run(String... args) {
        log.info("=== DB CHECK ===");
        log.info("Comptes: {}", compteFinancierRepository.count());
        log.info("Factures: {}", factureRepository.count());
        log.info("Tiers: {}", tiersRepository.count());
        log.info("Decaissements: {}", decaissementRepository.count());
        log.info("================");
    }
}
