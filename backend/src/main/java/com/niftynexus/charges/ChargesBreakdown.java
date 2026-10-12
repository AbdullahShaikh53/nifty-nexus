package com.niftynexus.charges;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Full cost transparency for one order (FR-18). */
public record ChargesBreakdown(BigDecimal turnover, BigDecimal brokerage, BigDecimal stt, BigDecimal exchange, BigDecimal sebi,
                               BigDecimal stampDuty, BigDecimal gst, BigDecimal total, BigDecimal breakEvenPrice,
                               List<Map<String, Object>> brokerComparison) {}
