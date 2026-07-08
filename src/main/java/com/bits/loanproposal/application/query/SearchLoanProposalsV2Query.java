package com.bits.loanproposal.application.query;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record SearchLoanProposalsV2Query(
        @NotBlank String traceId,
        @NotBlank String branchKey,
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
    public SearchLoanProposalsV2Query {
        page = page != null ? page : 0;
        size = size != null ? size : 20;
    }
}
