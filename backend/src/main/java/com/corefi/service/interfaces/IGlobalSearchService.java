package com.corefi.service.interfaces;

import com.corefi.dto.response.search.SearchResultDTO;
import java.util.List;

public interface IGlobalSearchService {
    List<SearchResultDTO> search(String query);
}
