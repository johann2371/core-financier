package com.corefi.dto.response.tableaubord;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TableauBordResponse {
    private BigDecimal soldeTotalCaisses;
    private BigDecimal soldeTotalBanques;
    private long decaissementsEnAttente;
    private BigDecimal totalFacturesImpayees;
    private List<ActiviteResponse> activitesRecentes;
}
