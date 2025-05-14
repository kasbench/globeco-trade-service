package org.kasbench.globeco_trade_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "execution_status")
public class ExecutionStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String abbreviation;

    @Column(nullable = false, length = 60)
    private String description;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
} 