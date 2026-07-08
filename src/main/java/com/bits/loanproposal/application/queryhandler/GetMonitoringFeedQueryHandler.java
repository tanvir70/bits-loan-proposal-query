package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.ddd.shared.exception.domain.BusinessRuleViolationException;
import com.bits.ddd.shared.exception.enums.ErrorCode;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.GetMonitoringFeedQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RegisterQueryHandler
public class GetMonitoringFeedQueryHandler
        implements QueryHandler<GetMonitoringFeedQuery, MonitoringFeedResponse> {

    private static final long MAX_WINDOW_HOURS = 24;

    private final LoanProposalReadRepository readRepository;

    public GetMonitoringFeedQueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public MonitoringFeedResponse handle(GetMonitoringFeedQuery query) {
        long windowHours = Duration.between(query.fromDateTime(), query.toDateTime()).toHours();
        if (windowHours > MAX_WINDOW_HOURS) {
            throw new BusinessRuleViolationException(
                    ErrorCode.INVALID_REQUEST,
                    "MONITORING_FEED_WINDOW_EXCEEDED",
                    "Monitoring feed window must not exceed " + MAX_WINDOW_HOURS + " hours.");
        }

        List<LoanProposalReadDocument> proposals =
                readRepository.findByCreatedAtBetween(query.fromDateTime(), query.toDateTime());

        return new MonitoringFeedResponse(
                query.fromDateTime(),
                query.toDateTime(),
                proposals.size(),
                proposals.stream().map(LoanProposalReadMapper::toMonitoringItem).toList());
    }
}
