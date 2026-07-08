package com.bits.loanproposal.application.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record GetMonitoringFeedQuery(
        @NotBlank String traceId,
        @NotNull LocalDateTime fromDateTime,
        @NotNull LocalDateTime toDateTime
) {}
