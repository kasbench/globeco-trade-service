package org.kasbench.globeco_trade_service.dto;

/**
 * Individual trade order result within a bulk response.
 * Contains the status and details for each trade order in a bulk operation.
 */
public class TradeOrderResultDTO {
    
    public enum ResultStatus {
        SUCCESS, FAILURE
    }
    
    private Integer requestIndex;
    private ResultStatus status;
    private String message;
    private TradeOrderResponseDTO tradeOrder;
    
    public TradeOrderResultDTO() {
    }
    
    public TradeOrderResultDTO(Integer requestIndex, ResultStatus status, String message, TradeOrderResponseDTO tradeOrder) {
        this.requestIndex = requestIndex;
        this.status = status;
        this.message = message;
        this.tradeOrder = tradeOrder;
    }
    
    public Integer getRequestIndex() {
        return requestIndex;
    }
    
    public void setRequestIndex(Integer requestIndex) {
        this.requestIndex = requestIndex;
    }
    
    public ResultStatus getStatus() {
        return status;
    }
    
    public void setStatus(ResultStatus status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public TradeOrderResponseDTO getTradeOrder() {
        return tradeOrder;
    }
    
    public void setTradeOrder(TradeOrderResponseDTO tradeOrder) {
        this.tradeOrder = tradeOrder;
    }
    
    @Override
    public String toString() {
        return "TradeOrderResultDTO{" +
                "requestIndex=" + requestIndex +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", tradeOrder=" + (tradeOrder != null ? "TradeOrderResponseDTO{id=" + tradeOrder.getId() + "}" : "null") +
                '}';
    }
}