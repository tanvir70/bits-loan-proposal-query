package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.application.query.ListLoanProposalsQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListLoanProposalsQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @InjectMocks
    private ListLoanProposalsQueryHandler handler;

    @Test
    void handleFetchesPageOfListItems() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");
        doc.setProposalNumber("LP-123");

        Page<LoanProposalReadDocument> page = new PageImpl<>(List.of(doc));
        when(readRepository.findAll(any(Criteria.class), any(PageRequest.class))).thenReturn(page);

        ListLoanProposalsQuery query = new ListLoanProposalsQuery(
                "trace-1", "0010", null, null, null, null, null, null, null, null, null, 0, 10);

        Page<LoanProposalListItem> result = handler.handle(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("proposal-1");
    }
}
