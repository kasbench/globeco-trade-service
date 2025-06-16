package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.ExecutionPageResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionV2ResponseDTO;
import org.kasbench.globeco_trade_service.dto.PaginationDTO;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionSpecification;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.util.SortingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ExecutionEnhancedService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionEnhancedService.class);
    
    private final ExecutionRepository executionRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    
    public ExecutionEnhancedService(
            ExecutionRepository executionRepository,
            TradeOrderRepository tradeOrderRepository,
            SecurityCacheService securityCacheService,
            PortfolioCacheService portfolioCacheService) {
        this.executionRepository = executionRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.securityCacheService = securityCacheService;
        this.portfolioCacheService = portfolioCacheService;
    }
    
    /**
     * Get paginated and filtered executions with enhanced data
     */
    public ExecutionPageResponseDTO getExecutionsV2(
            Integer limit,
            Integer offset,
            String sort,
            Integer id,
            String executionStatusAbbreviation,
            String blotterAbbreviation,
            String tradeTypeAbbreviation,
            Integer tradeOrderId,
            String destinationAbbreviation,
            String portfolioName,
            String securityTicker,
            BigDecimal quantityOrderedMin,
            BigDecimal quantityOrderedMax,
            BigDecimal quantityPlacedMin,
            BigDecimal quantityPlacedMax,
            BigDecimal quantityFilledMin,
            BigDecimal quantityFilledMax) {
        
        logger.debug("Getting executions v2 with filters - limit: {}, offset: {}, sort: {}", limit, offset, sort);
        
        // Validate and parse sorting
        Sort sortObj = SortingUtils.parseExecutionSort(sort);
        
        // Create pageable
        Pageable pageable = PageRequest.of(
            offset != null ? offset / (limit != null ? limit : 50) : 0,
            limit != null ? limit : 50,
            sortObj
        );
        
        // Resolve portfolio names and security tickers to IDs for filtering
        String portfolioId = null;
        String securityId = null;
        
        if (portfolioName != null && !portfolioName.trim().isEmpty()) {
            portfolioId = resolvePortfolioNamesToIds(portfolioName);
        }
        
        if (securityTicker != null && !securityTicker.trim().isEmpty()) {
            securityId = resolveSecurityTickersToIds(securityTicker);
        }
        
        // Build specification for filtering
        Specification<Execution> spec = ExecutionSpecification.buildSpecification(
            id, executionStatusAbbreviation, blotterAbbreviation, tradeTypeAbbreviation,
            tradeOrderId, destinationAbbreviation, portfolioId, securityId,
            quantityOrderedMin, quantityOrderedMax, quantityPlacedMin, quantityPlacedMax,
            quantityFilledMin, quantityFilledMax
        );
        
        // Execute query
        Page<Execution> page = executionRepository.findAll(spec, pageable);
        
        // Convert to enhanced DTOs with external service data
        List<ExecutionV2ResponseDTO> enhancedExecutions = page.getContent()
            .parallelStream()
            .map(this::convertToV2ResponseDTO)
            .toList();
        
        // Create pagination metadata
        PaginationDTO pagination = new PaginationDTO(
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
        
        logger.debug("Retrieved {} executions out of {} total", enhancedExecutions.size(), page.getTotalElements());
        
        return new ExecutionPageResponseDTO(enhancedExecutions, pagination);
    }
    
    /**
     * Convert Execution entity to enhanced V2 response DTO with external service data
     */
    private ExecutionV2ResponseDTO convertToV2ResponseDTO(Execution execution) {
        ExecutionV2ResponseDTO dto = new ExecutionV2ResponseDTO();
        
        // Basic fields
        dto.setId(execution.getId());
        dto.setExecutionTimestamp(execution.getExecutionTimestamp());
        dto.setQuantityOrdered(execution.getQuantityOrdered());
        dto.setQuantityPlaced(execution.getQuantityPlaced());
        dto.setQuantityFilled(execution.getQuantityFilled());
        dto.setLimitPrice(execution.getLimitPrice());
        dto.setExecutionServiceId(execution.getExecutionServiceId());
        dto.setVersion(execution.getVersion());
        
        // Entity relationships (already available)
        if (execution.getExecutionStatus() != null) {
            dto.setExecutionStatus(convertExecutionStatusToResponseDTO(execution.getExecutionStatus()));
        }
        
        if (execution.getBlotter() != null) {
            dto.setBlotter(convertBlotterToResponseDTO(execution.getBlotter()));
        }
        
        if (execution.getTradeType() != null) {
            dto.setTradeType(convertTradeTypeToResponseDTO(execution.getTradeType()));
        }
        
        if (execution.getDestination() != null) {
            dto.setDestination(convertDestinationToResponseDTO(execution.getDestination()));
        }
        
        // Enhanced trade order summary with external service data
        if (execution.getTradeOrder() != null) {
            dto.setTradeOrder(createTradeOrderSummary(execution.getTradeOrder()));
        }
        
        return dto;
    }
    
    /**
     * Create enhanced trade order summary with external service data
     */
    private ExecutionV2ResponseDTO.TradeOrderSummaryDTO createTradeOrderSummary(TradeOrder tradeOrder) {
        try {
            // Get enhanced portfolio and security data
            PortfolioDTO portfolio = null;
            SecurityDTO security = null;
            
            try {
                if (tradeOrder.getPortfolioId() != null) {
                    portfolio = portfolioCacheService.getPortfolioByName(tradeOrder.getPortfolioId());
                }
                
                if (tradeOrder.getSecurityId() != null) {
                    security = securityCacheService.getSecurityByTicker(tradeOrder.getSecurityId());
                }
            } catch (Exception e) {
                logger.warn("Error enriching trade order {} summary with external data: {}", tradeOrder.getId(), e.getMessage());
                // Set fallback data
                if (tradeOrder.getPortfolioId() != null) {
                    portfolio = new PortfolioDTO(tradeOrder.getPortfolioId(), tradeOrder.getPortfolioId());
                }
                if (tradeOrder.getSecurityId() != null) {
                    security = new SecurityDTO(tradeOrder.getSecurityId(), tradeOrder.getSecurityId());
                }
            }
            
            return new ExecutionV2ResponseDTO.TradeOrderSummaryDTO(
                tradeOrder.getId(),
                tradeOrder.getOrderId(),
                portfolio,
                security
            );
        } catch (Exception e) {
            logger.error("Error creating trade order summary for trade order {}: {}", tradeOrder.getId(), e.getMessage());
            // Fallback summary
            return new ExecutionV2ResponseDTO.TradeOrderSummaryDTO(tradeOrder.getId(), null, null, null);
        }
    }
    
    /**
     * Convert ExecutionStatus entity to response DTO
     */
    private org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO convertExecutionStatusToResponseDTO(
            org.kasbench.globeco_trade_service.entity.ExecutionStatus executionStatus) {
        org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO dto = 
            new org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO();
        dto.setId(executionStatus.getId());
        dto.setAbbreviation(executionStatus.getAbbreviation());
        dto.setDescription(executionStatus.getDescription());
        dto.setVersion(executionStatus.getVersion());
        return dto;
    }
    
    /**
     * Convert Blotter entity to response DTO
     */
    private org.kasbench.globeco_trade_service.dto.BlotterResponseDTO convertBlotterToResponseDTO(
            org.kasbench.globeco_trade_service.entity.Blotter blotter) {
        org.kasbench.globeco_trade_service.dto.BlotterResponseDTO dto = 
            new org.kasbench.globeco_trade_service.dto.BlotterResponseDTO();
        dto.setId(blotter.getId());
        dto.setAbbreviation(blotter.getAbbreviation());
        dto.setName(blotter.getName());
        dto.setVersion(blotter.getVersion());
        return dto;
    }
    
    /**
     * Convert TradeType entity to response DTO
     */
    private org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO convertTradeTypeToResponseDTO(
            org.kasbench.globeco_trade_service.entity.TradeType tradeType) {
        org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO dto = 
            new org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO();
        dto.setId(tradeType.getId());
        dto.setAbbreviation(tradeType.getAbbreviation());
        dto.setDescription(tradeType.getDescription());
        dto.setVersion(tradeType.getVersion());
        return dto;
    }
    
    /**
     * Convert Destination entity to response DTO
     */
    private org.kasbench.globeco_trade_service.dto.DestinationResponseDTO convertDestinationToResponseDTO(
            org.kasbench.globeco_trade_service.entity.Destination destination) {
        org.kasbench.globeco_trade_service.dto.DestinationResponseDTO dto = 
            new org.kasbench.globeco_trade_service.dto.DestinationResponseDTO();
        dto.setId(destination.getId());
        dto.setAbbreviation(destination.getAbbreviation());
        dto.setDescription(destination.getDescription());
        dto.setVersion(destination.getVersion());
        return dto;
    }
    
    /**
     * Resolve portfolio names to portfolio IDs for filtering
     */
    private String resolvePortfolioNamesToIds(String portfolioNames) {
        try {
            String[] names = portfolioNames.split(",");
            StringBuilder ids = new StringBuilder();
            
            for (String name : names) {
                PortfolioDTO portfolio = portfolioCacheService.getPortfolioByName(name.trim());
                if (ids.length() > 0) ids.append(",");
                ids.append(portfolio.getPortfolioId());
            }
            
            return ids.toString();
        } catch (Exception e) {
            logger.warn("Error resolving portfolio names to IDs: {}", e.getMessage());
            return portfolioNames; // Fallback to original names
        }
    }
    
    /**
     * Resolve security tickers to security IDs for filtering
     */
    private String resolveSecurityTickersToIds(String securityTickers) {
        try {
            String[] tickers = securityTickers.split(",");
            StringBuilder ids = new StringBuilder();
            
            for (String ticker : tickers) {
                SecurityDTO security = securityCacheService.getSecurityByTicker(ticker.trim());
                if (ids.length() > 0) ids.append(",");
                ids.append(security.getSecurityId());
            }
            
            return ids.toString();
        } catch (Exception e) {
            logger.warn("Error resolving security tickers to IDs: {}", e.getMessage());
            return securityTickers; // Fallback to original tickers
        }
    }
} 