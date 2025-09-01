package org.kasbench.globeco_trade_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for bulk trade order creation operations.
 * Contains an array of TradeOrderPostDTO objects to be created in a single atomic transaction.
 */
public class BulkTradeOrderRequestDTO {
    
    @NotNull(message = "Trade orders list cannot be null")
    @Size(min = 1, max = 1000, message = "Bulk size must be between 1 and 1000")
    private List<@Valid TradeOrderPostDTO> tradeOrders;
    
    public BulkTradeOrderRequestDTO() {
    }
    
    public BulkTradeOrderRequestDTO(List<TradeOrderPostDTO> tradeOrders) {
        this.tradeOrders = tradeOrders;
    }
    
    public List<TradeOrderPostDTO> getTradeOrders() {
        return tradeOrders;
    }
    
    public void setTradeOrders(List<TradeOrderPostDTO> tradeOrders) {
        this.tradeOrders = tradeOrders;
    }
    
    @Override
    public String toString() {
        return "BulkTradeOrderRequestDTO{" +
                "tradeOrders=" + (tradeOrders != null ? tradeOrders.size() + " items" : "null") +
                '}';
    }
}