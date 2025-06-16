package org.kasbench.globeco_trade_service.dto;

public class PortfolioDTO {
    private String portfolioId;
    private String name;
    
    public PortfolioDTO() {
    }
    
    public PortfolioDTO(String portfolioId, String name) {
        this.portfolioId = portfolioId;
        this.name = name;
    }
    
    public String getPortfolioId() {
        return portfolioId;
    }
    
    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "PortfolioDTO{" +
                "portfolioId='" + portfolioId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PortfolioDTO that = (PortfolioDTO) o;
        
        if (portfolioId != null ? !portfolioId.equals(that.portfolioId) : that.portfolioId != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }
    
    @Override
    public int hashCode() {
        int result = portfolioId != null ? portfolioId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
} 