package fr.gouv.tacw.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RiskLevel {
    NONE(0),
    TACW_LOW(1),
    ROBERT_LOW(2),
    TACW_HIGH(3),
    ROBERT_HIGH(4);
    
    private final int value;

    RiskLevel(int i) {
        this.value = i;
    }

    @JsonValue
    public int getValue() {
        return this.value;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
