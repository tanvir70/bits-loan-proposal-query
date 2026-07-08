package com.bits.loanproposal.presentation.dto.request;

import com.bits.loanproposal.application.query.SearchLoanProposalsV2Query;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;

import java.time.LocalDate;
import java.util.List;

public record SearchLoanProposalsV2Request(
        String searchTerm,
        Long voId,
        Long memberId,
        Long loanProductId,
        List<LoanProposalStatus> statuses,
        LoanProposalType proposalType,
        LocalDate fromDate,
        LocalDate toDate,
        Integer page,
        Integer size
) {
    public SearchLoanProposalsV2Query toQuery(String traceId, String branchKey) {
        return new SearchLoanProposalsV2Query(
                traceId,
                branchKey,
                searchTerm,
                voId,
                memberId,
                loanProductId,
                statuses,
                proposalType,
                fromDate,
                toDate,
                page,
                size);
    }
}
