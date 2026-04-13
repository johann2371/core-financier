package com.corefi.service.interfaces;

import com.corefi.dto.request.sessioncaisse.SessionCaisseFermetureRequest;
import com.corefi.dto.request.sessioncaisse.SessionCaisseRequest;
import com.corefi.dto.response.sessioncaisse.SessionCaisseResponse;

import java.util.List;
import java.util.Optional;

public interface ISessionCaisseService {
    SessionCaisseResponse ouvrirSession(SessionCaisseRequest request);
    SessionCaisseResponse fermerSession(Long sessionId, SessionCaisseFermetureRequest request);
    Optional<SessionCaisseResponse> getSessionActiveCurrentCaissier();
    List<SessionCaisseResponse> getHistoriqueSessionsCaissier(Long caissierId);
    List<SessionCaisseResponse> getHistoriqueSessionsCurrentCaissier();
    SessionCaisseResponse getById(Long id);
}
