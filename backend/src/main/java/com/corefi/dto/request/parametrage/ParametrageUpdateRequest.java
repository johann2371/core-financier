package com.corefi.dto.request.parametrage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParametrageUpdateRequest {
    @NotBlank(message = "La valeur est obligatoire")
    private String valeur;
    private String description;
}
