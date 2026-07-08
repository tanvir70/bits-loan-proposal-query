package com.bits.loanproposal.presentation.dto.request;

import com.bits.loanproposal.application.query.ListLoanProposalsQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;

import java.time.LocalDate;
import java.util.List;

public record ListLoanProposalsRequest(
        Long voId,
        Long memberId,
        Long loanProductId,
        Long schemeId,
        Long projectId,
        LocalDate fromDate,
        LocalDate toDate,
        List<LoanProposalStatus> statuses,
        LoanProposalType proposalType,
        Integer page,
        Integer size
) {
    public ListLoanProposalsQuery toQuery(String traceId, String branchKey) {
        return new ListLoanProposalsQuery(
                traceId,
                branchKey,
                voId,
                memberId,
                loanProductId,
                schemeId,
                projectId,
                fromDate,
                toDate,
                statuses,
                proposalType,
                page,
                size);
    }
}
