package com.corefi.mapper;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.entity.Tiers;
import com.corefi.enums.TypeTiers;
import org.springframework.stereotype.Component;

@Component
public class TiersMapper {

    /** Requête → Entité (pour la création) */
    public Tiers toEntity(TiersCreateRequest request) {
        Tiers t = new Tiers();
        t.setType(TypeTiers.valueOf(request.getType()));
        t.setRaisonSociale(request.getRaisonSociale());
        t.setTelephone(request.getTelephone());
        t.setEmail(request.getEmail());
        t.setAdresse(request.getAdresse());
        t.setActif(true);
        return t;
    }

    /** Entité → Response */
    public TiersResponse toResponse(Tiers tiers) {
        TiersResponse r = new TiersResponse();
        r.setId(tiers.getId());
        r.setCode(tiers.getCode());
        r.setType(tiers.getType().name());
        r.setRaisonSociale(tiers.getRaisonSociale());
        r.setTelephone(tiers.getTelephone());
        r.setEmail(tiers.getEmail());
        r.setActif(tiers.isActif());
        return r;
    }
}
