package com.niftynexus.market;

public record FeedStatus(String provider, boolean connected, boolean simulated, boolean stale, String lastTick, boolean marketOpen, String nextOpen, boolean simulateOpen) {}
