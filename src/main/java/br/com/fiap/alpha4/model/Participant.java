package br.com.fiap.alpha4.model;

/** A resposta pública nunca contém o hash do token. */
public record Participant(String id, String name, String uf, String community) { }
