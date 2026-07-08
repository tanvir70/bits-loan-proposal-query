package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import jakarta.validation.constraints.NotBlank;

public final class GetLoanProposalByIdQuery extends Query {

    @NotBlank
    private final String traceId;
    @NotBlank
    private final String branchKey;
    @NotBlank
    private final String id;

    public GetLoanProposalByIdQuery(String traceId, String branchKey, String id) {
        super(traceId);
        this.traceId = traceId;
        this.branchKey = branchKey;
        this.id = id;
    }

    public String traceId() {
        return traceId;
    }

    public String branchKey() {
        return branchKey;
    }

    public String id() {
        return id;
    }
}
