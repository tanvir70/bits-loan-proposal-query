package com.bits.loanproposal.presentation.dto.request;

import com.bits.loanproposal.application.query.GetSchemeDetailsQuery;

public record GetSchemeDetailsRequest(
        Long memberId,
        Long loanProductId,
        Long schemeId,
        Long branchId,
        Long voId
) {
    public GetSchemeDetailsQuery toQuery(String traceId) {
        return new GetSchemeDetailsQuery(traceId, memberId, loanProductId, schemeId, branchId, voId);
    }
}
