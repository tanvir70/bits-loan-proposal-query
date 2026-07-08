package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class GetSchemeDetailsQuery extends Query {

    @NotBlank
    private final String traceId;
    @NotNull
    private final Long memberId;
    @NotNull
    private final Long loanProductId;
    @NotNull
    private final Long schemeId;
    @NotNull
    private final Long branchId;
    private final Long voId;

    public GetSchemeDetailsQuery(
            String traceId,
            Long memberId,
            Long loanProductId,
            Long schemeId,
            Long branchId,
            Long voId) {
        super(traceId);
        this.traceId = traceId;
        this.memberId = memberId;
        this.loanProductId = loanProductId;
        this.schemeId = schemeId;
        this.branchId = branchId;
        this.voId = voId;
    }

    public String traceId() {
        return traceId;
    }

    public Long memberId() {
        return memberId;
    }

    public Long loanProductId() {
        return loanProductId;
    }

    public Long schemeId() {
        return schemeId;
    }

    public Long branchId() {
        return branchId;
    }

    public Long voId() {
        return voId;
    }
}
