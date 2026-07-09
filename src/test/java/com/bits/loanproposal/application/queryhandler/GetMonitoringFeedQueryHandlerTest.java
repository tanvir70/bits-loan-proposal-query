package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.shared.exception.domain.BusinessRuleViolationException;
import com.bits.loanproposal.application.query.GetMonitoringFeedQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonitoringFeedQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @InjectMocks
    private GetMonitoringFeedQueryHandler handler;

    @Test
    void handleRetrievesProposalsInWindow() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 2, 8, 0); // 23 hours

        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");

        when(readRepository.findByCreatedAtBetween(from, to)).thenReturn(List.of(doc));

        GetMonitoringFeedQuery query = new GetMonitoringFeedQuery("trace-1", from, to);
        MonitoringFeedResponse response = handler.handle(query);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void handleThrowsExceptionWhenWindowExceeds24Hours() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 2, 10, 0); // 25 hours

        GetMonitoringFeedQuery query = new GetMonitoringFeedQuery("trace-1", from, to);

        assertThatThrownBy(() -> handler.handle(query))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(ex -> {
                    BusinessRuleViolationException br = (BusinessRuleViolationException) ex;
                    assertThat(br.getBusinessRule()).isEqualTo("MONITORING_FEED_WINDOW_EXCEEDED");
                });
    }
}


