package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.PaginationDTO;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderPageResponseDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderV2ResponseDTO;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderSpecification;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TradeOrderEnhancedService {
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderEnhancedService.class);
    
    private final TradeOrderRepository tradeOrderRepository;
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    private final ExecutorService executorService;
    
    public TradeOrderEnhancedService(
            TradeOrderRepository tradeOrderRepository,
            SecurityCacheService securityCacheService,
            PortfolioCacheService portfolioCacheService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.securityCacheService = securityCacheService;
        this.portfolioCacheService = portfolioCacheService;
        this.executorService = Executors.newFixedThreadPool(10);
    }
    
    /**
     * Get paginated and filtered trade orders with enhanced data
     */
    public TradeOrderPageResponseDTO getTradeOrdersV2(
            Integer limit,
            Integer offset,
            String sort,
            Integer id,
            Integer orderId,
            String orderType,
            String portfolioName,
            String securityTicker,
            BigDecimal quantityMin,
            BigDecimal quantityMax,
            BigDecimal quantitySentMin,
            BigDecimal quantitySentMax,
            String blotterAbbreviation,
            Boolean submitted) {
        
        logger.debug("Getting trade orders v2 with filters - limit: {}, offset: {}, sort: {}", limit, offset, sort);
        
        // Validate and parse sorting
        Sort sortObj = SortingUtils.parseTradeOrderSort(sort);
        
        // Create pageable
        Pageable pageable = PageRequest.of(
            offset != null ? offset / (limit != null ? limit : 50) : 0,
            limit != null ? limit : 50,
            sortObj
        );
        
        // Build specification for filtering
        // Note: For v2 API, we'll need to resolve portfolio names and security tickers to IDs
        String portfolioId = null;
        String securityId = null;
        
        if (portfolioName != null && !portfolioName.trim().isEmpty()) {
            portfolioId = resolvePortfolioNamesToIds(portfolioName);
        }
        
        if (securityTicker != null && !securityTicker.trim().isEmpty()) {
            securityId = resolveSecurityTickersToIds(securityTicker);
        }
        
        Specification<TradeOrder> spec = TradeOrderSpecification.buildSpecification(
            id, orderId, orderType, portfolioId, securityId,
            quantityMin, quantityMax, quantitySentMin, quantitySentMax,
            blotterAbbreviation, submitted
        );
        
        // Execute query
        Page<TradeOrder> page = tradeOrderRepository.findAll(spec, pageable);
        
        // Convert to enhanced DTOs with external service data
        List<TradeOrderV2ResponseDTO> enhancedTradeOrders = page.getContent()
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
        
        logger.debug("Retrieved {} trade orders out of {} total", enhancedTradeOrders.size(), page.getTotalElements());
        
        return new TradeOrderPageResponseDTO(enhancedTradeOrders, pagination);
    }
    
    /**
     * Convert TradeOrder entity to enhanced V2 response DTO with external service data
     */
    private TradeOrderV2ResponseDTO convertToV2ResponseDTO(TradeOrder tradeOrder) {
        TradeOrderV2ResponseDTO dto = new TradeOrderV2ResponseDTO();
        
        // Basic fields
        dto.setId(tradeOrder.getId());
        dto.setOrderId(tradeOrder.getOrderId());
        dto.setOrderType(tradeOrder.getOrderType());
        dto.setQuantity(tradeOrder.getQuantity());
        dto.setQuantitySent(tradeOrder.getQuantitySent());
        dto.setLimitPrice(tradeOrder.getLimitPrice());
        dto.setTradeTimestamp(tradeOrder.getTradeTimestamp());
        dto.setSubmitted(tradeOrder.getSubmitted());
        dto.setVersion(tradeOrder.getVersion());
        
        // Enhanced fields with external service data
        try {
            // Get portfolio information
            if (tradeOrder.getPortfolioId() != null) {
                PortfolioDTO portfolio = portfolioCacheService.getPortfolioById(tradeOrder.getPortfolioId());
                dto.setPortfolio(portfolio);
            }
            
            // Get security information
            if (tradeOrder.getSecurityId() != null) {
                SecurityDTO security = securityCacheService.getSecurityById(tradeOrder.getSecurityId());
                dto.setSecurity(security);
            }
        } catch (Exception e) {
            logger.warn("Error enriching trade order {} with external data: {}", tradeOrder.getId(), e.getMessage());
            // Set fallback data
            if (tradeOrder.getPortfolioId() != null) {
                dto.setPortfolio(new PortfolioDTO(tradeOrder.getPortfolioId(), tradeOrder.getPortfolioId()));
            }
            if (tradeOrder.getSecurityId() != null) {
                dto.setSecurity(new SecurityDTO(tradeOrder.getSecurityId(), tradeOrder.getSecurityId()));
            }
        }
        
        // Blotter information (already available in entity)
        if (tradeOrder.getBlotter() != null) {
            dto.setBlotter(convertBlotterToResponseDTO(tradeOrder.getBlotter()));
        }
        
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
} 