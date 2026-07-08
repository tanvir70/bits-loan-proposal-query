package com.bits.loanproposal.presentation.dto;

import java.math.BigDecimal;

public record LoanAccountInfo(
        BigDecimal balance,
        BigDecimal scheduledAmount,
        BigDecimal overdueAmount,
        String status
) {}
