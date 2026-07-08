package com.bits.loanproposal.application.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetUPGTUPExistingLoansQuery(
        @NotBlank String traceId,
        @NotBlank String branchKey,
        @NotNull Long loanProductId
) {}
