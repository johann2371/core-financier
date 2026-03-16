package com.corefi.dto.response.tiers;

import lombok.Data;

@Data
public class TiersResponse {
    private Long id;
    private String code;
    private String type;
    private String raisonSociale;
    private String telephone;
    private String email;
    private boolean actif;
}
