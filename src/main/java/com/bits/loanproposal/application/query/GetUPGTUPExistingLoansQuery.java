package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class GetUPGTUPExistingLoansQuery extends Query {

    @NotBlank
    private final String traceId;
    @NotBlank
    private final String branchKey;
    @NotNull
    private final Long loanProductId;

    public GetUPGTUPExistingLoansQuery(String traceId, String branchKey, Long loanProductId) {
        super(traceId);
        this.traceId = traceId;
        this.branchKey = branchKey;
        this.loanProductId = loanProductId;
    }

    public String traceId() {
        return traceId;
    }

    public String branchKey() {
        return branchKey;
    }

    public Long loanProductId() {
        return loanProductId;
    }
}
