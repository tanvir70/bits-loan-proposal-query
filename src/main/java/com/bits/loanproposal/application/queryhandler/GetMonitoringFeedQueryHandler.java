package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.ddd.shared.exception.domain.BusinessRuleViolationException;
import com.bits.ddd.shared.localization.LocalizedMessage;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.GetMonitoringFeedQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
        Duration duration = Duration.between(query.fromDateTime(), query.toDateTime());
        if (duration.compareTo(Duration.ofHours(MAX_WINDOW_HOURS)) > 0) {
            throw new BusinessRuleViolationException(
                    query.getQueryIdentifier(), query.getQueryType(),
                    Map.of("toDateTime", LocalizedMessage.builder()
                            .key("MONITORING_FEED_WINDOW_EXCEEDED")
                            .args(new Object[]{MAX_WINDOW_HOURS})
                            .build()));
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
