package com.niftynexus.orders;

import com.niftynexus.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping("/preview") public Map<String, Object> preview(@Valid @RequestBody OrderDtos.PlaceOrderRequest r) { return service.preview(CurrentUser.id(), r); }
    @PostMapping public OrderDtos.OrderView place(@Valid @RequestBody OrderDtos.PlaceOrderRequest r) { return service.place(CurrentUser.id(), r); }
    @GetMapping public List<OrderDtos.OrderView> list() { return service.myOrders(CurrentUser.id()); }
    @GetMapping("/trades") public List<OrderDtos.TradeView> trades() { return service.myTrades(CurrentUser.id()); }
    @DeleteMapping("/{id}") public OrderDtos.OrderView cancel(@PathVariable Long id) { return service.cancel(CurrentUser.id(), id); }
}
