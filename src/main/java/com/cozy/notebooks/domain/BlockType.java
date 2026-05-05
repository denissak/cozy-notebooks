package com.cozy.notebooks.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum BlockType {
    PARAGRAPH,
    HEADING,
    TODO,
    CHECKLIST,
    QUOTE,
    CALLOUT,
    DIVIDER,
    IMAGE,
    FILE,
    AUDIO,
    CODE,
    TABLE,
    DATE,
    MOOD,
    HABIT,
    RATING,
    SPACER;

    @JsonValue
    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static BlockType fromWire(String value) {
        if (value == null) {
            return null;
        }
        return BlockType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
