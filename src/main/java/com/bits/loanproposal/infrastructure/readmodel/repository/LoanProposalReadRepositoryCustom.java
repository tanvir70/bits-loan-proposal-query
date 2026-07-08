package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;
import java.util.List;

public interface LoanProposalReadRepositoryCustom {

    Page<LoanProposalReadDocument> findAll(Criteria criteria, Pageable pageable);

    long countByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses);

    BigDecimal sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses);

    long countByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses);

    BigDecimal sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses);
}
