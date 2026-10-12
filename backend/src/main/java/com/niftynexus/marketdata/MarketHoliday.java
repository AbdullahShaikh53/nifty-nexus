package com.niftynexus.marketdata;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "market_holidays")
public class MarketHoliday {
    @Id @Column(name = "holiday_date") private LocalDate date;
    @Column(nullable = false) private String description;
    public LocalDate getDate() { return date; } public void setDate(LocalDate d) { date = d; }
    public String getDescription() { return description; } public void setDescription(String d) { description = d; }
}
