package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.SearchLoanProposalsV2Query;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDate;
import java.util.List;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchLoanProposalsV2QueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @Spy
    private LoanProposalReadMapper loanProposalReadMapper = Mappers.getMapper(LoanProposalReadMapper.class);

    @InjectMocks
    private SearchLoanProposalsV2QueryHandler handler;

    @Test
    void handleExecutesSearchQuery() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");

        Page<LoanProposalReadDocument> page = new PageImpl<>(List.of(doc));
        when(readRepository.findAll(any(Criteria.class), any(PageRequest.class))).thenReturn(page);

        SearchLoanProposalsV2Query query = new SearchLoanProposalsV2Query(
                "trace-1", "0010", " LP-1 ", null, null, null, null, null, null, null, 0, 10);

        Page<LoanProposalListItem> result = handler.handle(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("proposal-1");
    }

    @Test
    void handleExecutesSearchQueryWithOptionalFilters() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-2");

        Page<LoanProposalReadDocument> page = new PageImpl<>(List.of(doc));
        when(readRepository.findAll(any(Criteria.class), any(PageRequest.class))).thenReturn(page);

        SearchLoanProposalsV2Query query = new SearchLoanProposalsV2Query(
                "trace-1",
                "0010",
                "LP-123",
                10L,
                20L,
                30L,
                List.of(LoanProposalStatus.PENDING),
                LoanProposalType.NORMAL_LOAN,
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                0,
                10
        );

        Page<LoanProposalListItem> result = handler.handle(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("proposal-2");
    }
}
