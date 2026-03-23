package com.corefi.mapper;

import com.corefi.dto.request.decaissement.DecaissementCreateRequest;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.entity.Decaissement;
import com.corefi.entity.Tiers;
import com.corefi.entity.Devise;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutDecaissement;
import org.springframework.stereotype.Component;

@Component
public class DecaissementMapper {

    /** Requête → Entité */
    public Decaissement toEntity(DecaissementCreateRequest request, Tiers fournisseur, Devise devise) {
        Decaissement d = new Decaissement();
        d.setFournisseur(fournisseur);
        d.setMontant(request.getMontant());
        d.setBeneficiaire(request.getBeneficiaire());
        d.setMotif(request.getMotif()); // Raison de la demande
        d.setMoyenPaiement(MoyenPaiement.valueOf(request.getMoyenPaiement()));
        d.setBanqueEmettrice(request.getBanqueEmettrice());
        d.setNumeroOperation(request.getNumeroOperation());
        d.setDateOperation(request.getDateOperation());
        d.setTelephone(request.getTelephone());
        d.setDevise(devise);
        d.setStatut(StatutDecaissement.BROUILLON);
        return d;
    }

    /** Entité → Response */
    public DecaissementResponse toResponse(Decaissement d) {
        DecaissementResponse r = new DecaissementResponse();
        r.setId(d.getId());
        r.setNumero(d.getNumero());
        r.setDateDecaissement(d.getDateDecaissement());
        r.setDateCreation(d.getDateSaisie());
        r.setFournisseurNom(d.getFournisseur() != null ? d.getFournisseur().getRaisonSociale() : null);
        r.setBeneficiaire(d.getBeneficiaire());
        r.setMotif(d.getMotif());
        r.setMontant(d.getMontant());
        r.setStatut(d.getStatut().name());
        r.setSeuilPdgRequis(d.isSeuilPdgRequis());
        return r;
    }
}
