package com.corefi.dto.response.tableaubord;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ActiviteResponse {
    private String type; // FACTURE, ENCAISSEMENT, DECAISSEMENT, TIERS, SYSTEM, AUTH
    private String action; // CREATE, UPDATE, DELETE, etc.
    private String message;
    private LocalDateTime date;
    private String utilisateur;
}
