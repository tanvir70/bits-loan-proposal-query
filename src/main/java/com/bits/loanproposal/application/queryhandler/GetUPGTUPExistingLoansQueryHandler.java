package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.query.GetUPGTUPExistingLoansQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.UPGTUPExistingLoansResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RegisterQueryHandler
public class GetUPGTUPExistingLoansQueryHandler
        implements QueryHandler<GetUPGTUPExistingLoansQuery, UPGTUPExistingLoansResponse> {

    private static final List<LoanProposalStatus> ACTIVE_STATUSES =
            List.of(LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);

    private final LoanProposalReadRepository readRepository;

    public GetUPGTUPExistingLoansQueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public UPGTUPExistingLoansResponse handle(GetUPGTUPExistingLoansQuery query) {
        long existingLoanCount = readRepository.countByBranchCodeAndLoanProductIdAndStatuses(
                query.branchKey(), query.loanProductId(), ACTIVE_STATUSES);
        BigDecimal existingLoanAmount = readRepository.sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses(
                query.branchKey(), query.loanProductId(), ACTIVE_STATUSES);

        return new UPGTUPExistingLoansResponse(
                query.branchKey(),
                query.loanProductId(),
                (int) existingLoanCount,
                existingLoanAmount);
    }
}
