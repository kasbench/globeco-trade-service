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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class TradeOrderEnhancedService {
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderEnhancedService.class);
    
    private final TradeOrderRepository tradeOrderRepository;
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    
    public TradeOrderEnhancedService(
            TradeOrderRepository tradeOrderRepository,
            SecurityCacheService securityCacheService,
            PortfolioCacheService portfolioCacheService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.securityCacheService = securityCacheService;
        this.portfolioCacheService = portfolioCacheService;
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
        
        // Execute query with eager fetch of blotter to avoid lazy loading issues.
        // The repository runs this in a short read-only transaction and detaches the
        // results, so the JDBC connection is released before the external-service
        // enrichment below. Never call external services while holding a DB connection.
        Page<TradeOrder> page = tradeOrderRepository.findAllWithBlotterAndSpecification(spec, pageable);
        List<TradeOrder> tradeOrders = page.getContent();
        
        // Pre-resolve all unique portfolio and security IDs on this page with one
        // cache-first lookup per unique ID, rather than one per row. This minimizes the
        // number of external HTTP round-trips (and the time each request occupies a
        // worker thread) when a page contains repeated portfolio/security IDs.
        Map<String, PortfolioDTO> portfoliosById = resolvePortfoliosForPage(tradeOrders);
        Map<String, SecurityDTO> securitiesById = resolveSecuritiesForPage(tradeOrders);
        
        // Convert to enhanced DTOs using the pre-resolved lookup maps.
        List<TradeOrderV2ResponseDTO> enhancedTradeOrders = tradeOrders
            .stream()
            .map(tradeOrder -> convertToV2ResponseDTO(tradeOrder, portfoliosById, securitiesById))
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
     * Resolve every distinct portfolio ID appearing on the page to a PortfolioDTO,
     * performing at most one cache-first lookup per unique ID. Runs outside any
     * database transaction/connection scope.
     */
    private Map<String, PortfolioDTO> resolvePortfoliosForPage(List<TradeOrder> tradeOrders) {
        Set<String> portfolioIds = tradeOrders.stream()
            .map(TradeOrder::getPortfolioId)
            .filter(id -> id != null && !id.trim().isEmpty())
            .collect(Collectors.toSet());
        
        Map<String, PortfolioDTO> resolved = new LinkedHashMap<>();
        for (String portfolioId : portfolioIds) {
            try {
                resolved.put(portfolioId, portfolioCacheService.getPortfolioById(portfolioId));
            } catch (Exception e) {
                logger.warn("Error resolving portfolio {} for page enrichment: {}", portfolioId, e.getMessage());
                resolved.put(portfolioId, new PortfolioDTO(portfolioId, portfolioId));
            }
        }
        return resolved;
    }
    
    /**
     * Resolve every distinct security ID appearing on the page to a SecurityDTO,
     * performing at most one cache-first lookup per unique ID. Runs outside any
     * database transaction/connection scope.
     */
    private Map<String, SecurityDTO> resolveSecuritiesForPage(List<TradeOrder> tradeOrders) {
        Set<String> securityIds = tradeOrders.stream()
            .map(TradeOrder::getSecurityId)
            .filter(id -> id != null && !id.trim().isEmpty())
            .collect(Collectors.toSet());
        
        Map<String, SecurityDTO> resolved = new LinkedHashMap<>();
        for (String securityId : securityIds) {
            try {
                resolved.put(securityId, securityCacheService.getSecurityById(securityId));
            } catch (Exception e) {
                logger.warn("Error resolving security {} for page enrichment: {}", securityId, e.getMessage());
                resolved.put(securityId, new SecurityDTO(securityId, securityId));
            }
        }
        return resolved;
    }
    
    /**
     * Convert TradeOrder entity to enhanced V2 response DTO using pre-resolved
     * portfolio and security lookup maps. Does not perform any external calls itself.
     */
    private TradeOrderV2ResponseDTO convertToV2ResponseDTO(
            TradeOrder tradeOrder,
            Map<String, PortfolioDTO> portfoliosById,
            Map<String, SecurityDTO> securitiesById) {
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
        
        // Enhanced fields from pre-resolved maps (with defensive fallbacks)
        String portfolioId = tradeOrder.getPortfolioId();
        if (portfolioId != null) {
            dto.setPortfolio(portfoliosById.getOrDefault(portfolioId, new PortfolioDTO(portfolioId, portfolioId)));
        }
        
        String securityId = tradeOrder.getSecurityId();
        if (securityId != null) {
            dto.setSecurity(securitiesById.getOrDefault(securityId, new SecurityDTO(securityId, securityId)));
        }
        
        // Blotter information (already available in entity via eager fetch)
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