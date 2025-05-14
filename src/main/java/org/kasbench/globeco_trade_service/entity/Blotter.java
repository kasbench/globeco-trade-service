package org.kasbench.globeco_trade_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "blotter")
public class Blotter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String abbreviation;

    @Column(nullable = false, length = 100)
    private String name;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getAbbreviation() {
        return abbreviation;
    }
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
} 