package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.application.query.GetUPGTUPExistingLoansQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.UPGTUPExistingLoansResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUPGTUPExistingLoansQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @InjectMocks
    private GetUPGTUPExistingLoansQueryHandler handler;

    @Test
    void handleCalculatesBranchAverages() {
        List<LoanProposalStatus> activeStatuses = List.of(
                LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);
        
        when(readRepository.countByBranchCodeAndLoanProductIdAndStatuses("0010", 40L, activeStatuses))
                .thenReturn(5L);
        when(readRepository.sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses("0010", 40L, activeStatuses))
                .thenReturn(new BigDecimal("250000.00"));

        GetUPGTUPExistingLoansQuery query = new GetUPGTUPExistingLoansQuery("trace-1", "0010", 40L);
        UPGTUPExistingLoansResponse response = handler.handle(query);

        assertThat(response.branchKey()).isEqualTo("0010");
        assertThat(response.loanProductId()).isEqualTo(40L);
        assertThat(response.existingLoanCount()).isEqualTo(5);
        assertThat(response.existingLoanAmount()).isEqualByComparingTo("250000.00");
    }
}
