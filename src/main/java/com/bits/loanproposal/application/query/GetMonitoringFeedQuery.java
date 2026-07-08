package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class GetMonitoringFeedQuery extends Query {

    @NotBlank
    private final String traceId;
    @NotNull
    private final LocalDateTime fromDateTime;
    @NotNull
    private final LocalDateTime toDateTime;

    public GetMonitoringFeedQuery(String traceId, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        super(traceId);
        this.traceId = traceId;
        this.fromDateTime = fromDateTime;
        this.toDateTime = toDateTime;
    }

    public String traceId() {
        return traceId;
    }

    public LocalDateTime fromDateTime() {
        return fromDateTime;
    }

    public LocalDateTime toDateTime() {
        return toDateTime;
    }
}
