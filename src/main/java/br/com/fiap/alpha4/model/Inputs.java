package br.com.fiap.alpha4.model;

import jakarta.validation.constraints.*;

/** DTOs de entrada com limites independentes do frontend. */
public final class Inputs {
    private Inputs() { }

    public record MissionInput(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotNull Category category,
        @Min(1) @Max(10000) int points,
        @NotNull Boolean requiresEvidence) { }

    public record ParticipantInput(
        @NotBlank @Size(min = 3, max = 60) String name,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf,
        @NotBlank @Size(min = 3, max = 60) String community) { }

    public record EvidenceInput(@NotBlank @Size(min = 10, max = 2000) String text) { }

    public record ReviewInput(@NotBlank @Pattern(regexp = "aprovada|rejeitada") String status) { }

    public record Session(Participant participant, String token) { }
    public record Message(String message) { }
}
