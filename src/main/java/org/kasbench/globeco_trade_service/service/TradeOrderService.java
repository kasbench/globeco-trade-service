package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import java.util.List;
import java.util.Optional;

public interface TradeOrderService {
    List<TradeOrder> getAllTradeOrders();
    Optional<TradeOrder> getTradeOrderById(Integer id);
    TradeOrder createTradeOrder(TradeOrder tradeOrder);
    TradeOrder updateTradeOrder(Integer id, TradeOrder tradeOrder);
    void deleteTradeOrder(Integer id, Integer version);
} 