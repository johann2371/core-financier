package com.corefi.mapper;

import com.corefi.dto.request.decaissement.DecaissementCreateRequest;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.entity.Decaissement;
import com.corefi.entity.JustificatifDecaissement;
import com.corefi.entity.Tiers;
import com.corefi.entity.Devise;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutDecaissement;
import com.corefi.entity.Utilisateur;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DecaissementMapper {

    /** Requête → Entité */
    public Decaissement toEntity(DecaissementCreateRequest request, Tiers fournisseur, Devise devise) {
        Decaissement d = new Decaissement();
        d.setFournisseur(fournisseur);
        d.setMontant(request.getMontant());
        d.setBeneficiaire(request.getBeneficiaire());
        d.setMotif(request.getMotif());
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
        r.setFournisseurId(d.getFournisseur() != null ? d.getFournisseur().getId() : null);
        r.setFournisseurNom(d.getFournisseur() != null ? d.getFournisseur().getRaisonSociale() : null);
        r.setBeneficiaire(d.getBeneficiaire());
        r.setMotif(d.getMotif());
        r.setMontant(d.getMontant());
        r.setStatut(d.getStatut().name());
        r.setSeuilPdgRequis(d.isSeuilPdgRequis());
        r.setCategorie(d.getCategorie() != null ? d.getCategorie().name() : null);
        r.setMoyenPaiement(d.getMoyenPaiement() != null ? d.getMoyenPaiement().name() : null);

        // Justificatifs
        if (d.getJustificatifs() != null) {
            r.setNbJustificatifs(d.getJustificatifs().size());
            List<DecaissementResponse.JustificatifInfo> infos = d.getJustificatifs().stream().map((JustificatifDecaissement j) -> {
                DecaissementResponse.JustificatifInfo info = new DecaissementResponse.JustificatifInfo();
                info.setId(j.getId());
                info.setNomOriginal(j.getNomOriginal());
                info.setTypeFichier(j.getTypeFichier());
                info.setTailleFichier(j.getTailleFichier());
                return info;
            }).collect(Collectors.toList());
            r.setJustificatifs(infos);
        } else {
            r.setNbJustificatifs(0);
            r.setJustificatifs(new ArrayList<>());
        }

        // Traçabilité
        r.setSaisiParNom(formatFullUser(d.getSaisiPar()));
        r.setValideParNom(formatFullUser(d.getValidePar()));
        r.setApprouveParPdgNom(formatFullUser(d.getApprouveParPdg()));
        r.setExecuteParNom(formatFullUser(d.getExecutePar()));
        r.setSessionId(d.getSessionCaisse() != null ? d.getSessionCaisse().getId() : null);

        return r;
    }

    private String formatFullUser(Utilisateur u) {
        if (u == null) return null;
        return (u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : "");
    }
}
