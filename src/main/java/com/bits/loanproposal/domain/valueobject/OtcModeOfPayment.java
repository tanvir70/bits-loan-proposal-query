package com.bits.loanproposal.domain.valueobject;

import com.bits.loanproposal.domain.enums.ModeOfPaymentSubType;
import java.time.LocalDate;

public record OtcModeOfPayment(
    Long modeOfPaymentId,
    ModeOfPaymentSubType subType,
    String bankAccountNumber,
    String bankRoutingNumber,
    Long bankId,
    Long bankBranchId,
    String paymentSubTypeNumber,
    LocalDate paymentSubTypeDate,
    String bkashWalletNumber,
    String rocketWalletNumber,
    Long premiumModeOfPaymentId,
    Long digitalDisbursementModeId
) {}
