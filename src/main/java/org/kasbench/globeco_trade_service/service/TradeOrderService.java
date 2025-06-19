package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import java.util.List;
import java.util.Optional;

public interface TradeOrderService {
    List<TradeOrder> getAllTradeOrders();
    
    /**
     * Get all trade orders with pagination for v1 API backward compatibility
     * @param limit Maximum number of results to return (null for unlimited)
     * @param offset Number of results to skip (null for 0)
     * @return Paginated result with trade orders and total count
     */
    PaginatedResult<TradeOrder> getAllTradeOrders(Integer limit, Integer offset);
    
    Optional<TradeOrder> getTradeOrderById(Integer id);
    TradeOrder createTradeOrder(TradeOrder tradeOrder);
    TradeOrder updateTradeOrder(Integer id, TradeOrder tradeOrder);
    void deleteTradeOrder(Integer id, Integer version);
    
    /**
     * Submit a trade order for execution
     * @param tradeOrderId The ID of the trade order to submit
     * @param dto The submission details
     * @param noExecuteSubmit When false (default), automatically submits to execution service; when true, only creates local execution
     * @return The created execution record
     */
    Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto, boolean noExecuteSubmit);
    
    /**
     * Submit a trade order for execution with default behavior (automatically submits to execution service)
     * @param tradeOrderId The ID of the trade order to submit
     * @param dto The submission details
     * @return The created execution record
     */
    default Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto) {
        return submitTradeOrder(tradeOrderId, dto, false);
    }
    
    /**
     * Result wrapper for paginated data
     */
    class PaginatedResult<T> {
        private final List<T> data;
        private final long totalCount;
        
        public PaginatedResult(List<T> data, long totalCount) {
            this.data = data;
            this.totalCount = totalCount;
        }
        
        public List<T> getData() {
            return data;
        }
        
        public long getTotalCount() {
            return totalCount;
        }
    }
} 