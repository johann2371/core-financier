package com.corefi.dto.request.decaissement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidationRFRequest {
    private boolean rejeter; // true si c'est un rejet
    private String motif; // obligatoire si rejet
}
