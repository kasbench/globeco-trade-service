package org.kasbench.globeco_trade_service.util;

import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SortingUtils {
    
    // Valid sortable fields for TradeOrder
    private static final Set<String> VALID_TRADE_ORDER_SORT_FIELDS = Set.of(
        "id", "orderId", "orderType", "quantity", "quantitySent", 
        "tradeTimestamp", "submitted", "blotter.abbreviation"
    );
    
    // Valid sortable fields for Execution
    private static final Set<String> VALID_EXECUTION_SORT_FIELDS = Set.of(
        "id", "executionTimestamp", "quantityOrdered", "quantityPlaced", 
        "quantityFilled", "tradeOrderId", "executionStatus.abbreviation",
        "blotter.abbreviation", "tradeType.abbreviation", "destination.abbreviation"
    );
    
    /**
     * Parse sort parameter and create Sort object for TradeOrder
     * @param sortParam Comma-separated sort fields with optional '-' prefix for descending
     * @return Sort object or null if no valid sort fields
     * @throws IllegalArgumentException if invalid sort field is provided
     */
    public static Sort parseTradeOrderSort(String sortParam) {
        return parseSort(sortParam, VALID_TRADE_ORDER_SORT_FIELDS, "TradeOrder");
    }
    
    /**
     * Parse sort parameter and create Sort object for Execution
     * @param sortParam Comma-separated sort fields with optional '-' prefix for descending
     * @return Sort object or null if no valid sort fields
     * @throws IllegalArgumentException if invalid sort field is provided
     */
    public static Sort parseExecutionSort(String sortParam) {
        return parseSort(sortParam, VALID_EXECUTION_SORT_FIELDS, "Execution");
    }
    
    /**
     * Generic sort parsing method
     */
    private static Sort parseSort(String sortParam, Set<String> validFields, String entityName) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.unsorted();
        }
        
        String[] sortFields = sortParam.split(",");
        Sort sort = Sort.unsorted();
        
        for (String field : sortFields) {
            field = field.trim();
            if (field.isEmpty()) continue;
            
            Sort.Direction direction = Sort.Direction.ASC;
            String fieldName = field;
            
            // Check for descending order prefix
            if (field.startsWith("-")) {
                direction = Sort.Direction.DESC;
                fieldName = field.substring(1);
            }
            
            // Validate field name
            if (!validFields.contains(fieldName)) {
                throw new IllegalArgumentException(
                    String.format("Invalid sort field '%s' for %s. Valid fields are: %s", 
                        fieldName, entityName, validFields));
            }
            
            // Handle nested field sorting
            if (fieldName.contains(".")) {
                String[] parts = fieldName.split("\\.");
                sort = sort.and(Sort.by(direction, parts[0] + "." + parts[1]));
            } else {
                sort = sort.and(Sort.by(direction, fieldName));
            }
        }
        
        return sort;
    }
    
    /**
     * Validate that all sort fields are valid for TradeOrder
     */
    public static void validateTradeOrderSortFields(String sortParam) {
        validateSortFields(sortParam, VALID_TRADE_ORDER_SORT_FIELDS, "TradeOrder");
    }
    
    /**
     * Validate that all sort fields are valid for Execution
     */
    public static void validateExecutionSortFields(String sortParam) {
        validateSortFields(sortParam, VALID_EXECUTION_SORT_FIELDS, "Execution");
    }
    
    /**
     * Generic sort field validation
     */
    private static void validateSortFields(String sortParam, Set<String> validFields, String entityName) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return;
        }
        
        String[] sortFields = sortParam.split(",");
        for (String field : sortFields) {
            field = field.trim();
            if (field.isEmpty()) continue;
            
            // Remove direction prefix if present
            String fieldName = field.startsWith("-") ? field.substring(1) : field;
            
            if (!validFields.contains(fieldName)) {
                throw new IllegalArgumentException(
                    String.format("Invalid sort field '%s' for %s. Valid fields are: %s", 
                        fieldName, entityName, validFields));
            }
        }
    }
    
    /**
     * Get list of valid sort fields for TradeOrder
     */
    public static Set<String> getValidTradeOrderSortFields() {
        return new HashSet<>(VALID_TRADE_ORDER_SORT_FIELDS);
    }
    
    /**
     * Get list of valid sort fields for Execution
     */
    public static Set<String> getValidExecutionSortFields() {
        return new HashSet<>(VALID_EXECUTION_SORT_FIELDS);
    }
} 