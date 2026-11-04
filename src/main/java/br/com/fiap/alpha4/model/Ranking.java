package br.com.fiap.alpha4.model;

import java.util.List;

public record Ranking(String scope, String season, List<Entry> entries) {
    public record Entry(String id, int position, String name, long points) { }
}
