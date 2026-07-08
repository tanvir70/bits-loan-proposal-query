package com.bits.loanproposal.presentation.dto;

import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MonitoringFeedResponse(
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime,
        Integer totalCount,
        List<MonitoringFeedItem> items
) {
    @Builder
    public record MonitoringFeedItem(
            String id,
            String proposalNumber,
            Long memberId,
            String memberName,
            String branchCode,
            String projectCode,
            BigDecimal proposedLoanAmount,
            LoanProposalStatus loanProposalStatus,
            ApiDataSource dataSource,
            LocalDateTime createdAt
    ) {}
}
