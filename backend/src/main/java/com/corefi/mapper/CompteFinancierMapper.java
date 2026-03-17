package com.corefi.mapper;

import com.corefi.dto.response.compte.CompteFinancierResponse;
import com.corefi.entity.CompteFinancier;
import org.springframework.stereotype.Component;

@Component
public class CompteFinancierMapper {

    /** Entité → Response */
    public CompteFinancierResponse toResponse(CompteFinancier c) {
        CompteFinancierResponse r = new CompteFinancierResponse();
        r.setId(c.getId());
        r.setType(c.getType().name());
        r.setNumero(c.getNumero());
        r.setLibelle(c.getLibelle());
        r.setSolde(c.getSolde());
        r.setDeviseCode(c.getDevise() != null ? c.getDevise().getCode() : "XAF");
        r.setActif(c.isActif());
        return r;
    }
}
