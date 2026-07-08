package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.ddd.shared.exception.domain.DomainValidationException;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.GetLoanProposalByIdQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanAccountInfo;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import org.springframework.stereotype.Service;

@Service
@RegisterQueryHandler
public class GetLoanProposalByIdQueryHandler
        implements QueryHandler<GetLoanProposalByIdQuery, LoanProposalResponse> {

    private final LoanProposalReadRepository readRepository;

    public GetLoanProposalByIdQueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public LoanProposalResponse handle(GetLoanProposalByIdQuery query) {
        LoanProposalReadDocument doc = readRepository
                .findByIdAndBranchCodeAndIsActive(query.id(), query.branchKey(), true)
                .orElseThrow(() -> new DomainValidationException(
                        "NOT_FOUND", "Loan proposal not found with id: " + query.id()));

        LoanAccountInfo loanAccountInfo = null;
        if (doc.getLoanProposalStatus() == LoanProposalStatus.DISBURSED && doc.getLoanAccountId() != null) {
            loanAccountInfo = new LoanAccountInfo(
                    doc.getLoanAccountBalance(),
                    doc.getLoanAccountScheduledAmount(),
                    doc.getLoanAccountOverdueAmount(),
                    doc.getLoanAccountStatus());
        }

        // ponytail: first repayment date and enrollment override are projected onto the
        // read document; enrich from external services here if OQ-Q002/OQ-Q003 resolve otherwise
        return LoanProposalReadMapper.toDetailResponse(
                doc,
                doc.getFirstRepaymentDate(),
                doc.getCreditShieldExpiryDate(),
                doc.getFireInsuranceExpiryDate(),
                loanAccountInfo,
                doc.getEnrollmentStatusOverride());
    }
}
