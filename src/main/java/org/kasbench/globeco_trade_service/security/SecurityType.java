package org.kasbench.globeco_trade_service.security;

public class SecurityType {
    private String securityTypeId;
    private String abbreviation;
    private String description;
    private Integer version;

    public String getSecurityTypeId() {
        return securityTypeId;
    }
    public void setSecurityTypeId(String securityTypeId) {
        this.securityTypeId = securityTypeId;
    }
    public String getAbbreviation() {
        return abbreviation;
    }
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
} 