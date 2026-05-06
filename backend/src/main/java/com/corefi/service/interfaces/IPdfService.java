package com.corefi.service.interfaces;

public interface IPdfService {
    byte[] genererFacturePdf(Long factureId);
    byte[] genererRecuEncaissementPdf(Long encaissementId);
    byte[] genererRecuDecaissementPdf(Long decaissementId);
    byte[] genererRapportCloturePdf(Long sessionId);
    byte[] genererReleveCompteTiersPdf(Long tiersId);
}
