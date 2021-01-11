package fr.gouv.tacw.database.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RiskLevel {
    NONE(0),
    LOW(1),
    MODERATE_LOW(2),
    MODERATE_HIGH(3),
    HIGH(4);
    
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
