package com.bits.loanproposal.application.query;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record ListLoanProposalsQuery(
        @NotBlank String traceId,
        @NotBlank String branchKey,
        Long voId,
        Long memberId,
        Long loanProductId,
        Long schemeId,
        Long projectId,
        LocalDate fromDate,
        LocalDate toDate,
        List<LoanProposalStatus> statuses,
        LoanProposalType proposalType,
        @Min(0) Integer page,
        @Min(1) Integer size
) {
    public ListLoanProposalsQuery {
        page = page != null ? page : 0;
        size = size != null ? size : 20;
    }
}
