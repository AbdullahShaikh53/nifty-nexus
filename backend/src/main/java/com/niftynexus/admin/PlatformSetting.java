package com.niftynexus.admin;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_settings")
public class PlatformSetting {
    @Id @Column(name = "setting_key") private String key;
    @Column(name = "setting_value", nullable = false) private String value;
    public String getKey() { return key; } public void setKey(String k) { key = k; }
    public String getValue() { return value; } public void setValue(String v) { value = v; }
}
