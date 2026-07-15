package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.shared.exception.EntityNotFoundException;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.GetLoanProposalByIdQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoanProposalByIdQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @Spy
    private LoanProposalReadMapper loanProposalReadMapper = Mappers.getMapper(LoanProposalReadMapper.class);

    @InjectMocks
    private GetLoanProposalByIdQueryHandler handler;

    @Test
    void handleReturnsResponseWhenFound() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");
        doc.setBranchCode("0010");
        doc.setLoanProposalStatus(LoanProposalStatus.PENDING);
        doc.setIsActive(true);

        when(readRepository.findByIdAndBranchCodeAndIsActive("proposal-1", "0010", true))
                .thenReturn(Optional.of(doc));

        GetLoanProposalByIdQuery query = new GetLoanProposalByIdQuery("trace-1", "0010", "proposal-1");
        LoanProposalResponse response = handler.handle(query);

        assertThat(response.id()).isEqualTo("proposal-1");
        assertThat(response.loanProposalStatus()).isEqualTo(LoanProposalStatus.PENDING);
        assertThat(response.loanAccountBalance()).isNull();
    }

    @Test
    void handleReturnsDisbursedFinancialsWhenStatusIsDisbursed() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");
        doc.setBranchCode("0010");
        doc.setLoanProposalStatus(LoanProposalStatus.DISBURSED);
        doc.setLoanAccountId(999L);
        doc.setLoanAccountBalance(new BigDecimal("25000.00"));
        doc.setLoanAccountScheduledAmount(new BigDecimal("1200.00"));
        doc.setLoanAccountOverdueAmount(BigDecimal.ZERO);
        doc.setLoanAccountStatus("ACTIVE");
        doc.setIsActive(true);

        when(readRepository.findByIdAndBranchCodeAndIsActive("proposal-1", "0010", true))
                .thenReturn(Optional.of(doc));

        GetLoanProposalByIdQuery query = new GetLoanProposalByIdQuery("trace-1", "0010", "proposal-1");
        LoanProposalResponse response = handler.handle(query);

        assertThat(response.loanAccountBalance()).isEqualByComparingTo("25000.00");
        assertThat(response.loanAccountScheduledAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.loanAccountOverdueAmount()).isEqualByComparingTo("0.00");
        assertThat(response.loanAccountStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void handleThrowsEntityNotFoundExceptionWhenMissing() {
        when(readRepository.findByIdAndBranchCodeAndIsActive("proposal-1", "0010", true))
                .thenReturn(Optional.empty());

        GetLoanProposalByIdQuery query = new GetLoanProposalByIdQuery("trace-1", "0010", "proposal-1");

        assertThatThrownBy(() -> handler.handle(query))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
