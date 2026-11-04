package br.com.fiap.alpha4.model;

public record Mission(String id, String title, String description, Category category,
                      int points, boolean requiresEvidence, String status) { }
