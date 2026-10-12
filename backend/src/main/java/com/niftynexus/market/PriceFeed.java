package com.niftynexus.market;

/** Abstraction over the market data provider so Mock and Angel One can be swapped (SRS section 14). */
public interface PriceFeed {
    String name();
    void start();
    void stop();
    boolean isConnected();
    /** true when prices are simulated and must be labelled as such in the UI. */
    boolean isSimulated();
}
