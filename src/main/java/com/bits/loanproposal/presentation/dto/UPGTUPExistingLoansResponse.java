package com.bits.loanproposal.presentation.dto;

import java.math.BigDecimal;

public record UPGTUPExistingLoansResponse(
        String branchKey,
        Long loanProductId,
        Integer existingLoanCount,
        BigDecimal existingLoanAmount
) {}
