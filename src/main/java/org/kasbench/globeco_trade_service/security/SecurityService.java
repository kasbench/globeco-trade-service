package org.kasbench.globeco_trade_service.security;

import java.util.List;

public interface SecurityService {
    List<SecurityType> getSecurityTypes();
    List<Security> getSecurities();
} 