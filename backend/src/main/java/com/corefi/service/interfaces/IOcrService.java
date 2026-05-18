package com.corefi.service.interfaces;

import com.corefi.dto.response.ocr.FactureExtractionResult;
import org.springframework.web.multipart.MultipartFile;

public interface IOcrService {
    /**
     * Extrait les informations clés d'une facture (PDF ou Image)
     * @param fichier Le fichier uploadé
     * @return Les données extraites (fournisseur, montant, date, etc.)
     */
    FactureExtractionResult extraireDonnees(MultipartFile fichier);
}
