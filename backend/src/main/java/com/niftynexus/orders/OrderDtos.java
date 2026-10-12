package com.niftynexus.orders;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class OrderDtos {
    private OrderDtos() {}
    public record PlaceOrderRequest(@NotBlank String symbol,
                                    @NotNull Order.Side side,
                                    @NotNull Order.Type type,
                                    @NotNull @DecimalMin(value = "1") BigDecimal quantity,
                                    @DecimalMin(value = "0.01") BigDecimal limitPrice,
                                    @DecimalMin(value = "0.01") BigDecimal stopLoss,
                                    @DecimalMin(value = "0.01") BigDecimal targetPrice,
                                    @Size(max = 255) String reason,
                                    @Size(max = 255) String tags,
                                    boolean acknowledgeWarnings) {}

    public record OrderView(Long id, String symbol, String side, String type, BigDecimal quantity, BigDecimal filledQuantity,
                            BigDecimal limitPrice, BigDecimal stopLoss, BigDecimal targetPrice, String status, String rejectReason,
                            String placedAt, String updatedAt) {
        public static OrderView from(Order o) {
            return new OrderView(o.getId(), o.getSymbol(), o.getSide().name(), o.getOrderType().name(), o.getQuantity(), o.getFilledQuantity(),
                    o.getLimitPrice(), o.getStopLoss(), o.getTargetPrice(), o.getStatus().name(), o.getRejectReason(),
                    o.getPlacedAt().toString(), o.getUpdatedAt().toString());
        }
    }
    public record TradeView(Long id, Long orderId, String symbol, String side, BigDecimal fillPrice, BigDecimal fillQuantity, BigDecimal charges, BigDecimal realisedPnl, String executedAt) {
        public static TradeView from(Trade t) {
            return new TradeView(t.getId(), t.getOrderId(), t.getSymbol(), t.getSide().name(), t.getFillPrice(), t.getFillQuantity(), t.getCharges(), t.getRealisedPnl(), t.getExecutedAt().toString());
        }
    }
}
