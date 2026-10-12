package com.niftynexus.admin;

import com.niftynexus.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Runtime-editable settings (admin console): simulate-open, demo mode, slippage. */
@Service
public class PlatformSettingsService {
    public static final String SIMULATE_OPEN = "SIMULATE_MARKET_OPEN", DEMO_MODE = "DEMO_MODE", SLIPPAGE = "SLIPPAGE_PCT";
    private final PlatformSettingRepository repo; private final AppProperties props;
    public PlatformSettingsService(PlatformSettingRepository repo, AppProperties props) { this.repo = repo; this.props = props; }

    public String get(String key, String def) { return repo.findById(key).map(PlatformSetting::getValue).orElse(def); }
    public boolean isSimulateOpen() { return props.isSimulateOpen() || Boolean.parseBoolean(get(SIMULATE_OPEN, "false")); }
    public boolean isDemoMode() { return Boolean.parseBoolean(get(DEMO_MODE, "true")); }
    public BigDecimal slippagePct() { return new BigDecimal(get(SLIPPAGE, props.getSlippagePercent().toPlainString())); }
    public List<PlatformSetting> all() { return repo.findAll(); }

    @Transactional
    public void set(String key, String value) {
        PlatformSetting s = repo.findById(key).orElseGet(() -> { PlatformSetting n = new PlatformSetting(); n.setKey(key); return n; });
        s.setValue(value); repo.save(s);
    }
}
