package br.com.fiap.alpha4.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Evidence(String id, String missionId, String missionTitle, String participantId,
                       String text, String status, LocalDate cycleDay,
                       OffsetDateTime submittedAt, OffsetDateTime reviewedAt, int awardedPoints) { }
