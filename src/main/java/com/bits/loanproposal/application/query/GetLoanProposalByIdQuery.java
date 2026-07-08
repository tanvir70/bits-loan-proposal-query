package com.bits.loanproposal.application.query;

import jakarta.validation.constraints.NotBlank;

public record GetLoanProposalByIdQuery(
        @NotBlank String traceId,
        @NotBlank String branchKey,
        @NotBlank String id
) {}
