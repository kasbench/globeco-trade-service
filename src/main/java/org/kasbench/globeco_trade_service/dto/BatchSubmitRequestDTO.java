package org.kasbench.globeco_trade_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class BatchSubmitRequestDTO {
    
    @NotNull
    @Size(min = 1, max = 100, message = "Batch size must be between 1 and 100")
    private List<@Valid TradeOrderSubmissionDTO> submissions;
    
    public BatchSubmitRequestDTO() {
    }
    
    public BatchSubmitRequestDTO(List<TradeOrderSubmissionDTO> submissions) {
        this.submissions = submissions;
    }
    
    public List<TradeOrderSubmissionDTO> getSubmissions() {
        return submissions;
    }
    
    public void setSubmissions(List<TradeOrderSubmissionDTO> submissions) {
        this.submissions = submissions;
    }
    
    @Override
    public String toString() {
        return "BatchSubmitRequestDTO{" +
                "submissions=" + (submissions != null ? submissions.size() + " items" : "null") +
                '}';
    }
    
    /**
     * Individual trade order submission within a batch
     */
    public static class TradeOrderSubmissionDTO {
        @NotNull
        private Integer tradeOrderId;
        
        @NotNull
        private BigDecimal quantity;
        
        @NotNull
        private Integer destinationId;
        
        public TradeOrderSubmissionDTO() {
        }
        
        public TradeOrderSubmissionDTO(Integer tradeOrderId, BigDecimal quantity, Integer destinationId) {
            this.tradeOrderId = tradeOrderId;
            this.quantity = quantity;
            this.destinationId = destinationId;
        }
        
        public Integer getTradeOrderId() {
            return tradeOrderId;
        }
        
        public void setTradeOrderId(Integer tradeOrderId) {
            this.tradeOrderId = tradeOrderId;
        }
        
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        public Integer getDestinationId() {
            return destinationId;
        }
        
        public void setDestinationId(Integer destinationId) {
            this.destinationId = destinationId;
        }
        
        @Override
        public String toString() {
            return "TradeOrderSubmissionDTO{" +
                    "tradeOrderId=" + tradeOrderId +
                    ", quantity=" + quantity +
                    ", destinationId=" + destinationId +
                    '}';
        }
    }
} 