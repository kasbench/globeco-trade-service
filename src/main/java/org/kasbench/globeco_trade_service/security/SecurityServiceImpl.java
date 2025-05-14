package org.kasbench.globeco_trade_service.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Service
public class SecurityServiceImpl implements SecurityService {

    private final RestTemplate restTemplate;
    private final String securityServiceBaseUrl;

    public SecurityServiceImpl(
            RestTemplate restTemplate,
            @Value("${security.service.base-url:http://globeco-security-service:8000}") String securityServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.securityServiceBaseUrl = securityServiceBaseUrl;
    }

    @Override
    @Cacheable(value = "securityTypes", cacheManager = "cacheManager")
    public List<SecurityType> getSecurityTypes() {
        String url = securityServiceBaseUrl + "/api/v1/securityTypes";
        SecurityType[] types = restTemplate.getForObject(url, SecurityType[].class);
        return Arrays.asList(types != null ? types : new SecurityType[0]);
    }

    @Override
    @Cacheable(value = "securities", cacheManager = "cacheManager")
    public List<Security> getSecurities() {
        String url = securityServiceBaseUrl + "/api/v1/securities";
        Security[] securities = restTemplate.getForObject(url, Security[].class);
        return Arrays.asList(securities != null ? securities : new Security[0]);
    }
} 