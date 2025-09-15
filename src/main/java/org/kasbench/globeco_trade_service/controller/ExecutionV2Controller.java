package org.kasbench.globeco_trade_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kasbench.globeco_trade_service.dto.ExecutionPageResponseDTO;
import org.kasbench.globeco_trade_service.service.ExecutionEnhancedService;
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
@RequestMapping("/api/v2/executions")
@Validated
@Tag(name = "Executions v2", description = "Enhanced Execution operations with pagination, filtering, and sorting")
public class ExecutionV2Controller {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionV2Controller.class);

    private final ExecutionEnhancedService executionEnhancedService;

    public ExecutionV2Controller(ExecutionEnhancedService executionEnhancedService) {
        this.executionEnhancedService = executionEnhancedService;
    }

    @GetMapping
    @Operation(summary = "Get paginated and filtered executions with enhanced data", description = "Retrieve executions with pagination, filtering, sorting, and enriched trade order data from external services. "
            +
            "Supports advanced filtering by execution status, blotter, trade type, destination, portfolio, security, and quantity ranges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved executions", content = @Content(schema = @Schema(implementation = ExecutionPageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content(schema = @Schema(implementation = TradeOrderV2Controller.ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = TradeOrderV2Controller.ErrorResponse.class)))
    })
    public ResponseEntity<ExecutionPageResponseDTO> getExecutionsV2(
            HttpServletRequest request,

            @Parameter(description = "Maximum number of results to return (1-1000)", example = "50") @RequestParam(required = false, defaultValue = "50") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 1000, message = "Limit cannot exceed 1000") Integer limit,

            @Parameter(description = "Number of results to skip for pagination", example = "0") @RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "Offset must be non-negative") Integer offset,

            @Parameter(description = "Comma-separated sort fields with optional '-' prefix for descending order", example = "executionStatus.abbreviation,-quantityFilled,security.ticker,portfolio.name") @RequestParam(required = false) String sort,

            @Parameter(description = "Filter by execution ID", example = "123") @RequestParam(required = false) Integer id,

            @Parameter(description = "Filter by execution status abbreviation (comma-separated for OR condition)", example = "NEW,SENT,PART") @RequestParam(name = "executionStatus.abbreviation", required = false) String executionStatusAbbreviation,

            @Parameter(description = "Filter by blotter abbreviation (comma-separated for OR condition)", example = "EQ,FI") @RequestParam(name = "blotter.abbreviation", required = false) String blotterAbbreviation,

            @Parameter(description = "Filter by trade type abbreviation (comma-separated for OR condition)", example = "BUY,SELL") @RequestParam(name = "tradeType.abbreviation", required = false) String tradeTypeAbbreviation,

            @Parameter(description = "Filter by trade order ID", example = "456") @RequestParam(required = false) Integer tradeOrderId,

            @Parameter(description = "Filter by destination abbreviation (comma-separated for OR condition)", example = "NYSE,NASDAQ") @RequestParam(name = "destination.abbreviation", required = false) String destinationAbbreviation,

            @Parameter(description = "Filter by portfolio name (comma-separated for OR condition)", example = "Tech Portfolio,Energy Fund") @RequestParam(name = "portfolio.name", required = false) String portfolioName,

            @Parameter(description = "Filter by security ticker (comma-separated for OR condition)", example = "AAPL,GOOGL,MSFT") @RequestParam(name = "security.ticker", required = false) String securityTicker,

            @Parameter(description = "Minimum quantity ordered filter", example = "100.00") @RequestParam(name = "quantityOrdered.min", required = false) BigDecimal quantityOrderedMin,

            @Parameter(description = "Maximum quantity ordered filter", example = "1000.00") @RequestParam(name = "quantityOrdered.max", required = false) BigDecimal quantityOrderedMax,

            @Parameter(description = "Minimum quantity placed filter", example = "50.00") @RequestParam(name = "quantityPlaced.min", required = false) BigDecimal quantityPlacedMin,

            @Parameter(description = "Maximum quantity placed filter", example = "500.00") @RequestParam(name = "quantityPlaced.max", required = false) BigDecimal quantityPlacedMax,

            @Parameter(description = "Minimum quantity filled filter", example = "25.00") @RequestParam(name = "quantityFilled.min", required = false) BigDecimal quantityFilledMin,

            @Parameter(description = "Maximum quantity filled filter", example = "250.00") @RequestParam(name = "quantityFilled.max", required = false) BigDecimal quantityFilledMax,

            @Parameter(description = "Filter by execution service ID (exact match)", example = "789") @RequestParam(name = "executionServiceId", required = false) Integer executionServiceId) {

        // logger.debug("GET /api/v2/executions - IP: {}, limit: {}, offset: {}, sort: {}, filters applied",
        //         request.getRemoteAddr(), limit, offset, sort);

        try {
            // Validate sort fields if provided
            if (sort != null && !sort.trim().isEmpty()) {
                SortingUtils.validateExecutionSortFields(sort);
            }

            // Validate quantity ranges
            validateQuantityRanges(quantityOrderedMin, quantityOrderedMax, "quantityOrdered");
            validateQuantityRanges(quantityPlacedMin, quantityPlacedMax, "quantityPlaced");
            validateQuantityRanges(quantityFilledMin, quantityFilledMax, "quantityFilled");

            // Call enhanced service
            ExecutionPageResponseDTO response = executionEnhancedService.getExecutionsV2(
                    limit, offset, sort, id, executionStatusAbbreviation, blotterAbbreviation,
                    tradeTypeAbbreviation, tradeOrderId, destinationAbbreviation,
                    portfolioName, securityTicker,
                    quantityOrderedMin, quantityOrderedMax, quantityPlacedMin, quantityPlacedMax,
                    quantityFilledMin, quantityFilledMax,
                    executionServiceId);

            logger.debug("Successfully retrieved {} executions out of {} total",
                    response.getExecutions().size(), response.getPagination().getTotalElements());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            throw new BadRequestException("Invalid query parameters: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving executions v2: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Error retrieving executions: " + e.getMessage());
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