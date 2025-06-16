package org.kasbench.globeco_trade_service.dto;

import java.util.List;

public class TradeOrderPageResponseDTO {
    private List<TradeOrderV2ResponseDTO> tradeOrders;
    private PaginationDTO pagination;
    
    public TradeOrderPageResponseDTO() {
    }
    
    public TradeOrderPageResponseDTO(List<TradeOrderV2ResponseDTO> tradeOrders, PaginationDTO pagination) {
        this.tradeOrders = tradeOrders;
        this.pagination = pagination;
    }
    
    public List<TradeOrderV2ResponseDTO> getTradeOrders() {
        return tradeOrders;
    }
    
    public void setTradeOrders(List<TradeOrderV2ResponseDTO> tradeOrders) {
        this.tradeOrders = tradeOrders;
    }
    
    public PaginationDTO getPagination() {
        return pagination;
    }
    
    public void setPagination(PaginationDTO pagination) {
        this.pagination = pagination;
    }
    
    @Override
    public String toString() {
        return "TradeOrderPageResponseDTO{" +
                "tradeOrders=" + (tradeOrders != null ? tradeOrders.size() + " items" : "null") +
                ", pagination=" + pagination +
                '}';
    }
} 