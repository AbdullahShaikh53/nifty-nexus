package com.niftynexus.charges;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "charges_config")
public class ChargesConfig {
    @Id @Column(name = "charge_key") private String key;
    @Column(nullable = false, precision = 12, scale = 6) private BigDecimal rate;
    @Column(nullable = false) private String description;
    public String getKey() { return key; }
    public BigDecimal getRate() { return rate; } public void setRate(BigDecimal r) { rate = r; }
    public String getDescription() { return description; }
}
