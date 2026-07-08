package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public final class ListLoanProposalsQuery extends Query {

    @NotBlank
    private final String traceId;
    @NotBlank
    private final String branchKey;
    private final Long voId;
    private final Long memberId;
    private final Long loanProductId;
    private final Long schemeId;
    private final Long projectId;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final List<LoanProposalStatus> statuses;
    private final LoanProposalType proposalType;
    @Min(0)
    private final Integer page;
    @Min(1)
    private final Integer size;

    public ListLoanProposalsQuery(
            String traceId,
            String branchKey,
            Long voId,
            Long memberId,
            Long loanProductId,
            Long schemeId,
            Long projectId,
            LocalDate fromDate,
            LocalDate toDate,
            List<LoanProposalStatus> statuses,
            LoanProposalType proposalType,
            Integer page,
            Integer size) {
        super(traceId);
        this.traceId = traceId;
        this.branchKey = branchKey;
        this.voId = voId;
        this.memberId = memberId;
        this.loanProductId = loanProductId;
        this.schemeId = schemeId;
        this.projectId = projectId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.statuses = statuses;
        this.proposalType = proposalType;
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
    }

    public String traceId() {
        return traceId;
    }

    public String branchKey() {
        return branchKey;
    }

    public Long voId() {
        return voId;
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

    public Long projectId() {
        return projectId;
    }

    public LocalDate fromDate() {
        return fromDate;
    }

    public LocalDate toDate() {
        return toDate;
    }

    public List<LoanProposalStatus> statuses() {
        return statuses;
    }

    public LoanProposalType proposalType() {
        return proposalType;
    }

    public Integer page() {
        return page;
    }

    public Integer size() {
        return size;
    }
}
