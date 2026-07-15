package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.GetLoanProposalByIdQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanAccountInfo;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import com.bits.loanproposal.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RegisterQueryHandler
@RequiredArgsConstructor
public class GetLoanProposalByIdQueryHandler
        implements QueryHandler<GetLoanProposalByIdQuery, LoanProposalResponse> {

    private final LoanProposalReadRepository readRepository;
    private final LoanProposalReadMapper loanProposalReadMapper;

    @Override
    public LoanProposalResponse handle(GetLoanProposalByIdQuery query) {
        LoanProposalReadDocument loanProposalDocument = readRepository
                .findByIdAndBranchCodeAndIsActive(query.id(), query.branchKey(), true)
                .orElseThrow(() -> new EntityNotFoundException(
                        query.getQueryIdentifier(), query.getQueryType(), "LoanProposal", query.id()));

        LoanAccountInfo loanAccountInfo = getLoanAccountInfo(loanProposalDocument);

        return loanProposalReadMapper.toDetailResponse(loanProposalDocument, loanAccountInfo);
    }

    private static LoanAccountInfo getLoanAccountInfo(LoanProposalReadDocument loanProposalDocument) {
        LoanAccountInfo loanAccountInfo = null;
        if (LoanProposalStatus.DISBURSED.equals(loanProposalDocument.getLoanProposalStatus()) && loanProposalDocument.getLoanAccountId() != null) {
            loanAccountInfo = new LoanAccountInfo(
                    loanProposalDocument.getLoanAccountBalance(),
                    loanProposalDocument.getLoanAccountScheduledAmount(),
                    loanProposalDocument.getLoanAccountOverdueAmount(),
                    loanProposalDocument.getLoanAccountStatus());
        }
        return loanAccountInfo;
    }
}
