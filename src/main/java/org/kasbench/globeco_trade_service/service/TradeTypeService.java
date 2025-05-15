package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeType;
import java.util.List;
import java.util.Optional;

public interface TradeTypeService {
    List<TradeType> getAllTradeTypes();
    Optional<TradeType> getTradeTypeById(Integer id);
    TradeType createTradeType(TradeType tradeType);
    TradeType updateTradeType(Integer id, TradeType tradeType);
    void deleteTradeType(Integer id, Integer version);
} 