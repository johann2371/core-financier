package com.corefi.mapper;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.request.facture.LigneFactureRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.entity.Facture;
import com.corefi.entity.LigneFacture;
import com.corefi.entity.Tiers;
import com.corefi.enums.StatutFacture;
import com.corefi.entity.Utilisateur;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class FactureMapper {

    /** Requête → Entité */
    public Facture toEntity(FactureCreateRequest request, Tiers tiers) {
        Facture f = new Facture();
        f.setType(request.getType());
        f.setTiers(tiers);
        f.setStatut(StatutFacture.BROUILLON);
        f.setMontantHt(BigDecimal.ZERO);
        f.setMontantTva(BigDecimal.ZERO);
        f.setMontantTtc(BigDecimal.ZERO);

        if (request.getLignes() != null) {
            List<LigneFacture> lignes = new ArrayList<>();
            int num = 1;
            for (LigneFactureRequest lr : request.getLignes()) {
                LigneFacture ligne = toLigneEntity(lr, f, num++);
                lignes.add(ligne);
            }
            f.setLignes(lignes);
            recalculerTotaux(f);
        }
        return f;
    }

    /** LigneRequest → LigneEntité */
    private LigneFacture toLigneEntity(LigneFactureRequest lr, Facture facture, int numero) {
        LigneFacture l = new LigneFacture();
        l.setFacture(facture);
        l.setNumeroLigne(numero);
        l.setDesignation(lr.getDesignation());
        l.setQuantite(lr.getQuantite());
        l.setPrixUnitaire(lr.getPrixUnitaire());
        // TVA camerounaise par défaut : 19.25%
        BigDecimal ht = lr.getQuantite().multiply(lr.getPrixUnitaire());
        BigDecimal tva = ht.multiply(l.getTauxTva()).divide(BigDecimal.valueOf(100));
        l.setMontantHt(ht);
        l.setMontantTva(tva);
        l.setMontantTtc(ht.add(tva));
        return l;
    }

    /** Recalcule les totaux HT/TVA/TTC de la facture */
    private void recalculerTotaux(Facture f) {
        BigDecimal ht = f.getLignes().stream()
                .map(LigneFacture::getMontantHt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tva = f.getLignes().stream()
                .map(LigneFacture::getMontantTva)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        f.setMontantHt(ht);
        f.setMontantTva(tva);
        f.setMontantTtc(ht.add(tva));
    }

    /** Entité → Response */
    public FactureResponse toResponse(Facture f, BigDecimal resteAPayer) {
        FactureResponse r = new FactureResponse();
        r.setId(f.getId());
        r.setNumero(f.getNumero());
        r.setType(f.getType());
        r.setDateFacture(f.getDateFacture());
        r.setTiersId(f.getTiers() != null ? f.getTiers().getId() : null);
        r.setTiersNom(f.getTiers() != null ? f.getTiers().getRaisonSociale() : null);
        r.setMontantTtc(f.getMontantTtc());
        r.setResteAPayer(resteAPayer != null ? resteAPayer : f.getMontantTtc());
        r.setStatut(f.getStatut().name());

        // Traçabilité
        r.setCreeParNom(formatFullUser(f.getCreePar()));
        r.setValideParNom(formatFullUser(f.getValidePar()));
        r.setDateSaisie(f.getDateSaisie());
        r.setDateValidation(f.getDateValidation());

        return r;
    }

    private String formatFullUser(Utilisateur u) {
        if (u == null) return null;
        return (u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : "");
    }
}
