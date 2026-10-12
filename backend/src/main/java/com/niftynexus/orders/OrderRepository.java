package com.niftynexus.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByPlacedAtDesc(Long userId);
    List<Order> findByStatusIn(List<Order.Status> statuses);
    long countByStatus(Order.Status status);
    List<Order> findByUserIdAndSymbolAndStatus(Long userId, String symbol, Order.Status status);
}
