package com.bits.loanproposal.application.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetSchemeDetailsQuery(
        @NotBlank String traceId,
        @NotNull Long memberId,
        @NotNull Long loanProductId,
        @NotNull Long schemeId,
        @NotNull Long branchId,
        Long voId
) {}
