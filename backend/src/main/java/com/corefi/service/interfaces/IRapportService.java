package com.corefi.service.interfaces;

import java.util.Map;

public interface IRapportService {
    Map<String, Object> genererBilanMensuel(int mois, int annee);
    byte[] genererBilanMensuelPdf(int mois, int annee);
}
