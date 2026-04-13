package com.corefi.dto.response.sessioncaisse;

import com.corefi.enums.StatutSessionCaisse;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SessionCaisseResponse {
    private Long id;
    private Long caisseId;
    private String caisseNom;
    private String caissierNom;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinalTheorique;
    private BigDecimal soldeFinalReel;
    private BigDecimal ecart;
    private String motifEcart;
    private StatutSessionCaisse statut;
}
