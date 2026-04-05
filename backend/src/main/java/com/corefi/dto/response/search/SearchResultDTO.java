package com.corefi.dto.response.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private String id;
    private String title;
    private String subtitle;
    private String type; // USER, TIER, INVOICE, AUDIT
    private String url;
}
