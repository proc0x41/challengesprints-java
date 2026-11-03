package br.com.fiap.alpha4.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    AMBIENTAL("ambiental"), SOCIAL("social"), GOVERNANCA("governanca");

    private final String value;
    Category(String value) { this.value = value; }
    @JsonValue public String value() { return value; }

    @JsonCreator
    public static Category from(String value) {
        for (Category category : values()) if (category.value.equals(value)) return category;
        throw new IllegalArgumentException("Categoria inválida.");
    }
}
