package com.bits.loanproposal.presentation.dto;

import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Trimmed summary for list/search views (DDD-REQ-Q016).
 */
@Builder
public record LoanProposalListItem(
        String id,
        String proposalNumber,
        Long memberId,
        String memberName,
        String memberMobileNumber,
        Long branchId,
        String branchCode,
        Long projectId,
        String projectCode,
        Long villageOrganisationId,
        String villageOrganisationCode,
        Long loanProductId,
        Long schemeId,
        BigDecimal proposedLoanAmount,
        BigDecimal approvedLoanAmount,
        LoanProposalStatus loanProposalStatus,
        LoanProposalType loanProposalType,
        LocalDate applicationDate,
        String approvalFlowStatus,
        ApiDataSource dataSource,
        Boolean isDigitalDisbursement,
        LocalDateTime createdAt
) {}
