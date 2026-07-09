package com.bits.loanproposal.domain.enums;

import com.bits.ddd.shared.domain.value.DomainStatus;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class LoanProposalStatus extends DomainStatus {

    public static final LoanProposalStatus DRAFT = new LoanProposalStatus("DRAFT");
    public static final LoanProposalStatus BM_APPROVAL_PENDING = new LoanProposalStatus("BM_APPROVAL_PENDING");
    public static final LoanProposalStatus BM_REJECT = new LoanProposalStatus("BM_REJECT");
    public static final LoanProposalStatus PENDING = new LoanProposalStatus("PENDING");
    public static final LoanProposalStatus APPROVED = new LoanProposalStatus("APPROVED");
    public static final LoanProposalStatus REJECTED = new LoanProposalStatus("REJECTED");
    public static final LoanProposalStatus DISBURSED = new LoanProposalStatus("DISBURSED");

    private LoanProposalStatus(String code) {
        super(code);
    }

    @JsonCreator
    public static LoanProposalStatus of(String code) {
        return new LoanProposalStatus(code);
    }
}
