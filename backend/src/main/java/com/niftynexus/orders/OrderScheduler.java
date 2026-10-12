package com.niftynexus.orders;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderScheduler {
    private final OrderService orders;
    public OrderScheduler(OrderService orders) { this.orders = orders; }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void match() { orders.matchOpenOrders(); }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void expire() { orders.expireOpenOrders(); }
}
