package org.kasbench.globeco_trade_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.kasbench.globeco_trade_service.dto.BatchSubmitRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchSubmitResponseDTO;
import org.kasbench.globeco_trade_service.service.BatchTradeOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tradeOrders")
@Validated
@Tag(name = "Trade Orders Batch", description = "Batch operations for trade order submissions")
public class BatchTradeOrderController {
    private static final Logger logger = LoggerFactory.getLogger(BatchTradeOrderController.class);
    
    private final BatchTradeOrderService batchTradeOrderService;
    
    public BatchTradeOrderController(BatchTradeOrderService batchTradeOrderService) {
        this.batchTradeOrderService = batchTradeOrderService;
    }
    
    @PostMapping("/batch/submit")
    @Operation(
        summary = "Submit multiple trade orders in batch",
        description = "Submit up to 100 trade orders in a single batch operation with parallel processing. " +
                     "Returns detailed results for each submission including success/failure status and execution details."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "All trade orders submitted successfully",
            content = @Content(schema = @Schema(implementation = BatchSubmitResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "207", 
            description = "Partial success - some trade orders submitted successfully",
            content = @Content(schema = @Schema(implementation = BatchSubmitResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request format or batch size exceeded",
            content = @Content(schema = @Schema(implementation = TradeOrderV2Controller.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "413", 
            description = "Payload too large - batch size exceeds 100 items",
            content = @Content(schema = @Schema(implementation = TradeOrderV2Controller.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = TradeOrderV2Controller.ErrorResponse.class))
        )
    })
    public ResponseEntity<BatchSubmitResponseDTO> submitTradeOrdersBatch(
            @Parameter(description = "Batch submission request containing trade order submissions", required = true)
            @Valid @RequestBody BatchSubmitRequestDTO request,
            @Parameter(description = "When false (default), automatically submits to execution service; when true, only creates local executions")
            @org.springframework.web.bind.annotation.RequestParam(value = "noExecuteSubmit", required = false, defaultValue = "false") boolean noExecuteSubmit) {
        
        logger.info("POST /api/v1/tradeOrders/batch/submit - Processing batch of {} trade orders, noExecuteSubmit={}", 
                   request.getSubmissions() != null ? request.getSubmissions().size() : 0, noExecuteSubmit);
        
        try {
            // Validate batch size at controller level for early rejection
            if (request.getSubmissions() != null && request.getSubmissions().size() > 100) {
                logger.warn("Batch size {} exceeds maximum allowed (100)", request.getSubmissions().size());
                throw new PayloadTooLargeException(
                    String.format("Batch size (%d) exceeds maximum allowed (100)", 
                        request.getSubmissions().size()));
            }
            
            // Process batch submission
            BatchSubmitResponseDTO response = batchTradeOrderService.submitTradeOrdersBatch(request, noExecuteSubmit);
            
            // Determine HTTP status based on batch results
            HttpStatus status = determineHttpStatus(response);
            
            logger.info("Batch submission completed - Status: {}, HTTP: {}, Successful: {}, Failed: {}", 
                       response.getStatus(), status, response.getSuccessful(), response.getFailed());
            
            return ResponseEntity.status(status).body(response);
            
        } catch (PayloadTooLargeException e) {
            logger.warn("Payload too large: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch request: {}", e.getMessage());
            throw new BadRequestException("Invalid batch request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing batch submission: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Error processing batch submission: " + e.getMessage());
        }
    }
    
    /**
     * Determine appropriate HTTP status code based on batch results
     */
    private HttpStatus determineHttpStatus(BatchSubmitResponseDTO response) {
        switch (response.getStatus()) {
            case SUCCESS:
                return HttpStatus.OK; // 200
            case PARTIAL:
                return HttpStatus.MULTI_STATUS; // 207
            case FAILURE:
                return HttpStatus.OK; // 200 (still a valid response, just all failed)
            default:
                return HttpStatus.OK;
        }
    }
    
    /**
     * Custom exception for payload too large
     */
    public static class PayloadTooLargeException extends RuntimeException {
        public PayloadTooLargeException(String message) {
            super(message);
        }
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