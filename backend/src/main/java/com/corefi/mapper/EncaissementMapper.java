package com.corefi.mapper;

import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.entity.Encaissement;
import com.corefi.entity.Tiers;
import com.corefi.entity.CompteFinancier;
import com.corefi.entity.Devise;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutEncaissement;
import org.springframework.stereotype.Component;

@Component
public class EncaissementMapper {

    /** Requête → Entité */
    public Encaissement toEntity(EncaissementCreateRequest request, Tiers client,
                                  CompteFinancier compte, Devise devise) {
        Encaissement e = new Encaissement();
        e.setClient(client);
        e.setMontant(request.getMontant());
        e.setMoyenPaiement(MoyenPaiement.valueOf(request.getMoyenPaiement()));
        e.setCompteFinancier(compte);
        e.setDevise(devise);
        e.setReference(request.getReference());
        e.setBanqueEmettrice(request.getBanqueEmettrice());
        e.setNumeroOperation(request.getNumeroOperation());
        e.setDateOperation(request.getDateOperation());
        e.setTelephone(request.getTelephone());
        e.setStatut(StatutEncaissement.VALIDEE); // Par défaut : VALIDEE
        return e;
    }

    /** Entité → Response */
    public EncaissementResponse toResponse(Encaissement e) {
        EncaissementResponse r = new EncaissementResponse();
        r.setId(e.getId());
        r.setNumero(e.getNumero());
        r.setDateEncaissement(e.getDateEncaissement());
        r.setNomClient(e.getClient() != null ? e.getClient().getRaisonSociale() : null);
        r.setMontant(e.getMontant());
        r.setMoyenPaiement(e.getMoyenPaiement().name());
        r.setStatut(e.getStatut().name());
        return r;
    }
}
