package com.corefi.service.interfaces;

import com.corefi.dto.response.tableaubord.TableauBordResponse;
import java.math.BigDecimal;

public interface ITableauBordService {
    TableauBordResponse getKpis(int forecastDays);
    void updateSeuil(BigDecimal nouveauSeuil);
}
