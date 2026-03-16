package com.corefi.dto.request.decaissement;

import lombok.Data;

@Data
public class ApprobationPDGRequest {
    private boolean rejeter; // true si c'est un rejet
    private String motif; // obligatoire si rejet
}
