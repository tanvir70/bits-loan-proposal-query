package com.bits.loanproposal.application.query;

import com.bits.ddd.shared.messaging.Query;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public final class SearchLoanProposalsV2Query extends Query {

    @NotBlank
    private final String traceId;
    @NotBlank
    private final String branchKey;
    private final String searchTerm;
    private final Long voId;
    private final Long memberId;
    private final Long loanProductId;
    private final List<LoanProposalStatus> statuses;
    private final LoanProposalType proposalType;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer page;
    private final Integer size;

    public SearchLoanProposalsV2Query(
            String traceId,
            String branchKey,
            String searchTerm,
            Long voId,
            Long memberId,
            Long loanProductId,
            List<LoanProposalStatus> statuses,
            LoanProposalType proposalType,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size) {
        super(traceId);
        this.traceId = traceId;
        this.branchKey = branchKey;
        this.searchTerm = searchTerm;
        this.voId = voId;
        this.memberId = memberId;
        this.loanProductId = loanProductId;
        this.statuses = statuses;
        this.proposalType = proposalType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
    }

    public String traceId() {
        return traceId;
    }

    public String branchKey() {
        return branchKey;
    }

    public String searchTerm() {
        return searchTerm;
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

    public List<LoanProposalStatus> statuses() {
        return statuses;
    }

    public LoanProposalType proposalType() {
        return proposalType;
    }

    public LocalDate fromDate() {
        return fromDate;
    }

    public LocalDate toDate() {
        return toDate;
    }

    public Integer page() {
        return page;
    }

    public Integer size() {
        return size;
    }
}
