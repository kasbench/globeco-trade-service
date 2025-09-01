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
    
    /**
     * Get all trade orders with optional filtering by order_id and pagination
     * @param limit Maximum number of results to return (null for unlimited)
     * @param offset Number of results to skip (null for 0)
     * @param orderId Filter by order_id (null for no filtering)
     * @return Paginated result with trade orders and total count
     */
    PaginatedResult<TradeOrder> getAllTradeOrders(Integer limit, Integer offset, Integer orderId);
    
    Optional<TradeOrder> getTradeOrderById(Integer id);
    TradeOrder createTradeOrder(TradeOrder tradeOrder);
    
    /**
     * Creates multiple trade orders in a single atomic transaction.
     * 
     * <p>This method processes all trade orders as a single database transaction,
     * ensuring atomicity - either all trade orders are successfully created or
     * none are persisted if any validation or database error occurs.</p>
     * 
     * <p>All trade orders in the list are validated before any database operations
     * are performed. If validation fails for any order, the entire operation is
     * rejected without persisting any data.</p>
     * 
     * @param tradeOrders List of TradeOrder entities to create. Must not be null or empty.
     * @return List of created TradeOrder entities with generated IDs and timestamps,
     *         in the same order as the input list
     * @throws IllegalArgumentException if tradeOrders is null, empty, or contains invalid data
     * @throws org.springframework.dao.DataIntegrityViolationException if database constraints are violated
     * @throws org.springframework.transaction.TransactionException if the transaction fails
     * @since 1.0
     */
    List<TradeOrder> createTradeOrdersBulk(List<TradeOrder> tradeOrders);
    
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