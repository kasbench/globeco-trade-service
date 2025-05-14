package org.kasbench.globeco_trade_service.security;

public class Security {
    private String securityId;
    private String ticker;
    private String description;
    private String securityTypeId;
    private Integer version;
    private SecurityType securityType;

    public String getSecurityId() {
        return securityId;
    }
    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }
    public String getTicker() {
        return ticker;
    }
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getSecurityTypeId() {
        return securityTypeId;
    }
    public void setSecurityTypeId(String securityTypeId) {
        this.securityTypeId = securityTypeId;
    }
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
    public SecurityType getSecurityType() {
        return securityType;
    }
    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }
} 