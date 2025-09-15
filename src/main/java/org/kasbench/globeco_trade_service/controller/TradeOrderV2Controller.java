package org.kasbench.globeco_trade_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kasbench.globeco_trade_service.dto.TradeOrderPageResponseDTO;
import org.kasbench.globeco_trade_service.service.TradeOrderEnhancedService;
import org.kasbench.globeco_trade_service.util.SortingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v2/tradeOrders")
@Validated
@Tag(name = "Trade Orders v2", description = "Enhanced Trade Order operations with pagination, filtering, and sorting")
public class TradeOrderV2Controller {
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderV2Controller.class);
    
    private final TradeOrderEnhancedService tradeOrderEnhancedService;
    
    public TradeOrderV2Controller(TradeOrderEnhancedService tradeOrderEnhancedService) {
        this.tradeOrderEnhancedService = tradeOrderEnhancedService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get paginated and filtered trade orders with enhanced data",
        description = "Retrieve trade orders with pagination, filtering, sorting, and enriched data from external services. " +
                     "Supports advanced filtering by portfolio names, security tickers, quantity ranges, and more."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully retrieved trade orders",
            content = @Content(schema = @Schema(implementation = TradeOrderPageResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid query parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<TradeOrderPageResponseDTO> getTradeOrdersV2(
            @Parameter(description = "Maximum number of results to return (1-1000)", example = "50")
            @RequestParam(required = false, defaultValue = "50") 
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 1000, message = "Limit cannot exceed 1000") 
            Integer limit,
            
            @Parameter(description = "Number of results to skip for pagination", example = "0")
            @RequestParam(required = false, defaultValue = "0") 
            @Min(value = 0, message = "Offset must be non-negative")
            Integer offset,
            
            @Parameter(description = "Comma-separated sort fields with optional '-' prefix for descending order", 
                      example = "security.ticker,-quantity,tradeTimestamp")
            @RequestParam(required = false) 
            String sort,
            
            @Parameter(description = "Filter by trade order ID", example = "123")
            @RequestParam(required = false) 
            Integer id,
            
            @Parameter(description = "Filter by order ID", example = "456")
            @RequestParam(required = false) 
            Integer orderId,
            
            @Parameter(description = "Filter by order type (comma-separated for OR condition)", example = "BUY,SELL")
            @RequestParam(required = false) 
            String orderType,
            
            @Parameter(description = "Filter by portfolio name (comma-separated for OR condition)", 
                      example = "Growth Fund,Tech Portfolio")
            @RequestParam(name = "portfolio.name", required = false) 
            String portfolioName,
            
            @Parameter(description = "Filter by security ticker (comma-separated for OR condition)", 
                      example = "AAPL,MSFT,GOOGL")
            @RequestParam(name = "security.ticker", required = false) 
            String securityTicker,
            
            @Parameter(description = "Minimum quantity filter", example = "100.00")
            @RequestParam(name = "quantity.min", required = false) 
            BigDecimal quantityMin,
            
            @Parameter(description = "Maximum quantity filter", example = "1000.00")
            @RequestParam(name = "quantity.max", required = false) 
            BigDecimal quantityMax,
            
            @Parameter(description = "Minimum quantity sent filter", example = "50.00")
            @RequestParam(name = "quantitySent.min", required = false) 
            BigDecimal quantitySentMin,
            
            @Parameter(description = "Maximum quantity sent filter", example = "500.00")
            @RequestParam(name = "quantitySent.max", required = false) 
            BigDecimal quantitySentMax,
            
            @Parameter(description = "Filter by blotter abbreviation (comma-separated for OR condition)", 
                      example = "EQ,FI")
            @RequestParam(name = "blotter.abbreviation", required = false) 
            String blotterAbbreviation,
            
            @Parameter(description = "Filter by submission status", example = "true")
            @RequestParam(required = false) 
            Boolean submitted) {
        
        logger.debug("GET /api/v2/tradeOrders - limit: {}, offset: {}, sort: {}, filters applied", 
                   limit, offset, sort);
        
        try {
            // Validate sort fields if provided
            if (sort != null && !sort.trim().isEmpty()) {
                SortingUtils.validateTradeOrderSortFields(sort);
            }
            
            // Validate quantity ranges
            validateQuantityRanges(quantityMin, quantityMax, "quantity");
            validateQuantityRanges(quantitySentMin, quantitySentMax, "quantitySent");
            
            // Call enhanced service
            TradeOrderPageResponseDTO response = tradeOrderEnhancedService.getTradeOrdersV2(
                limit, offset, sort, id, orderId, orderType, portfolioName, securityTicker,
                quantityMin, quantityMax, quantitySentMin, quantitySentMax, 
                blotterAbbreviation, submitted
            );
            
            logger.debug("Successfully retrieved {} trade orders out of {} total", 
                       response.getTradeOrders().size(), response.getPagination().getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            throw new BadRequestException("Invalid query parameters: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving trade orders v2: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Error retrieving trade orders: " + e.getMessage());
        }
    }
    
    /**
     * Validate quantity range parameters
     */
    private void validateQuantityRanges(BigDecimal min, BigDecimal max, String fieldName) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("%s.min (%s) cannot be greater than %s.max (%s)", 
                    fieldName, min, fieldName, max));
        }
        
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("%s.min (%s) cannot be negative", fieldName, min));
        }
        
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("%s.max (%s) cannot be negative", fieldName, max));
        }
    }
    
    /**
     * Error response DTO for API documentation
     */
    @Schema(description = "Error response")
    public static class ErrorResponse {
        @Schema(description = "Error type", example = "Bad Request")
        private String error;
        
        @Schema(description = "Error message", example = "Invalid query parameters")
        private String message;
        
        @Schema(description = "Additional error details")
        private Object details;
        
        // Constructors, getters, and setters
        public ErrorResponse() {}
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
        
        public ErrorResponse(String error, String message, Object details) {
            this.error = error;
            this.message = message;
            this.details = details;
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
    }
    
    /**
     * Custom exception for bad requests
     */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for internal server errors
     */
    public static class InternalServerErrorException extends RuntimeException {
        public InternalServerErrorException(String message) {
            super(message);
        }
    }
} 