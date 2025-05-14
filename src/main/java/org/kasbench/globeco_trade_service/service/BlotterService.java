package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Blotter;
import java.util.List;
import java.util.Optional;

public interface BlotterService {
    List<Blotter> getAllBlotters();
    Optional<Blotter> getBlotterById(Integer id);
    Blotter createBlotter(Blotter blotter);
    Blotter updateBlotter(Integer id, Blotter blotter);
    void deleteBlotter(Integer id, Integer version);
} 