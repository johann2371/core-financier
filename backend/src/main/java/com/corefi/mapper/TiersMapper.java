package com.corefi.mapper;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.entity.Client;
import com.corefi.entity.Fournisseur;
import com.corefi.entity.Tiers;
import com.corefi.enums.TypeTiers;
import org.springframework.stereotype.Component;

@Component
public class TiersMapper {

    /** Requête → Entité de base (les champs spécifiques sont gérés dans le service) */
    public Tiers toEntity(TiersCreateRequest request) {
        Tiers t = new Tiers();
        t.setType(TypeTiers.valueOf(request.getType().toUpperCase()));
        t.setRaisonSociale(request.getRaisonSociale());
        t.setTelephone(request.getTelephone());
        t.setEmail(request.getEmail());
        t.setAdresse(request.getAdresse());
        t.setVille(request.getVille());
        t.setPays(request.getPays() != null ? request.getPays() : "Cameroun");
        t.setActif(true);
        return t;
    }

    /** Entité → Response (gère polymorphisme Client/Fournisseur) */
    public TiersResponse toResponse(Tiers tiers) {
        TiersResponse r = new TiersResponse();
        r.setId(tiers.getId());
        r.setCode(tiers.getCode());
        r.setType(tiers.getType().name());
        r.setRaisonSociale(tiers.getRaisonSociale());
        r.setTelephone(tiers.getTelephone());
        r.setEmail(tiers.getEmail());
        r.setAdresse(tiers.getAdresse());
        r.setVille(tiers.getVille());
        r.setPays(tiers.getPays());
        r.setActif(tiers.isActif());
        r.setSolde(tiers.getSolde());
        r.setTotalDette(tiers.getTotalDette());
        r.setNui(tiers.getNui());
        r.setRccm(tiers.getRccm());

        // Champs spécifiques Client
        if (tiers instanceof Client client) {
            r.setTypeClient(client.getTypeClient());
            r.setCni(client.getCni());
            r.setPhotoUrl(client.getPhotoUrl());
            r.setCreditLimite(client.getCreditLimite() != null
                    ? client.getCreditLimite().doubleValue() : null);
            r.setDelaiPaiement(client.getDelaiPaiement());
        }

        // Champs spécifiques Fournisseur
        if (tiers instanceof Fournisseur fournisseur) {
            r.setNumeroCompte(fournisseur.getNumeroCompte());
            r.setIban(fournisseur.getIban());
        }

        return r;
    }
}
