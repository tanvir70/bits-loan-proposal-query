package com.bits.loanproposal.presentation.dto.request;

import com.bits.loanproposal.application.query.GetMonitoringFeedQuery;

import java.time.LocalDateTime;

public record GetMonitoringFeedRequest(
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime
) {
    public GetMonitoringFeedQuery toQuery(String traceId) {
        return new GetMonitoringFeedQuery(traceId, fromDateTime, toDateTime);
    }
}
