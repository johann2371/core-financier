package com.corefi.dto.response.tableaubord;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TableauBordResponse {
    private BigDecimal soldeTotalCaisses;
    private BigDecimal soldeTotalBanques;
    private long decaissementsEnAttente;
    private BigDecimal totalFacturesImpayees;
}
