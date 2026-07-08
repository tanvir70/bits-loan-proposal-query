package com.bits.loanproposal.presentation.dto;

import java.math.BigDecimal;

public record SchemeDetailsResponse(
        Long schemeId,
        Boolean memberFound,
        String voCategory,
        Integer existingLoanCount,
        BigDecimal existingLoanTotal,
        BigDecimal assetGrantPercentage
) {}
