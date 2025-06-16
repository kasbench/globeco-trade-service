package org.kasbench.globeco_trade_service.dto;

public class SecurityDTO {
    private String securityId;
    private String ticker;
    
    public SecurityDTO() {
    }
    
    public SecurityDTO(String securityId, String ticker) {
        this.securityId = securityId;
        this.ticker = ticker;
    }
    
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
    
    @Override
    public String toString() {
        return "SecurityDTO{" +
                "securityId='" + securityId + '\'' +
                ", ticker='" + ticker + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        SecurityDTO that = (SecurityDTO) o;
        
        if (securityId != null ? !securityId.equals(that.securityId) : that.securityId != null) return false;
        return ticker != null ? ticker.equals(that.ticker) : that.ticker == null;
    }
    
    @Override
    public int hashCode() {
        int result = securityId != null ? securityId.hashCode() : 0;
        result = 31 * result + (ticker != null ? ticker.hashCode() : 0);
        return result;
    }
} 