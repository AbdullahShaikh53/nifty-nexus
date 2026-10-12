package com.niftynexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Typed view of the nexus.* block in application.yml. */
@ConfigurationProperties(prefix = "nexus")
public class AppProperties {
    private String jwtSecret;
    private int jwtExpiryHours = 12;
    private boolean cookieSecure = false;
    private String corsOrigins = "http://localhost:5173";
    private String feedMode = "mock";
    private boolean simulateOpen = false;
    private BigDecimal slippagePercent = new BigDecimal("0.05");
    private BigDecimal defaultWallet = new BigDecimal("1000000");
    private AngelOne angelone = new AngelOne();
    private Coach coach = new Coach();

    public static class AngelOne {
        private String apiKey = ""; private String clientId = ""; private String pin = ""; private String totpSecret = "";
        public String getApiKey() { return apiKey; } public void setApiKey(String v) { apiKey = v; }
        public String getClientId() { return clientId; } public void setClientId(String v) { clientId = v; }
        public String getPin() { return pin; } public void setPin(String v) { pin = v; }
        public String getTotpSecret() { return totpSecret; } public void setTotpSecret(String v) { totpSecret = v; }
        public boolean isConfigured() { return !apiKey.isBlank() && !clientId.isBlank(); }
    }
    public static class Coach {
        private boolean llmEnabled = false; private String llmApiKey = "";
        public boolean isLlmEnabled() { return llmEnabled; } public void setLlmEnabled(boolean v) { llmEnabled = v; }
        public String getLlmApiKey() { return llmApiKey; } public void setLlmApiKey(String v) { llmApiKey = v; }
    }

    public String getJwtSecret() { return jwtSecret; } public void setJwtSecret(String v) { jwtSecret = v; }
    public int getJwtExpiryHours() { return jwtExpiryHours; } public void setJwtExpiryHours(int v) { jwtExpiryHours = v; }
    public boolean isCookieSecure() { return cookieSecure; } public void setCookieSecure(boolean v) { cookieSecure = v; }
    public String getCorsOrigins() { return corsOrigins; } public void setCorsOrigins(String v) { corsOrigins = v; }
    public String getFeedMode() { return feedMode; } public void setFeedMode(String v) { feedMode = v; }
    public boolean isSimulateOpen() { return simulateOpen; } public void setSimulateOpen(boolean v) { simulateOpen = v; }
    public BigDecimal getSlippagePercent() { return slippagePercent; } public void setSlippagePercent(BigDecimal v) { slippagePercent = v; }
    public BigDecimal getDefaultWallet() { return defaultWallet; } public void setDefaultWallet(BigDecimal v) { defaultWallet = v; }
    public AngelOne getAngelone() { return angelone; } public void setAngelone(AngelOne v) { angelone = v; }
    public Coach getCoach() { return coach; } public void setCoach(Coach v) { coach = v; }
}
