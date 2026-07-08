package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shared filter building for list and searchV2 (DDD-REQ-Q006 / Q008).
 * Fixed filters: dataSource=OTC, exclude GOOD_LOAN, isActive=true,
 * default status exclusions unless statuses explicitly supplied.
 */
final class LoanProposalFilterCriteria {

    private static final List<LoanProposalStatus> DEFAULT_EXCLUDED_STATUSES =
            List.of(LoanProposalStatus.BM_APPROVAL_PENDING, LoanProposalStatus.DRAFT, LoanProposalStatus.BM_REJECT);

    private final List<Criteria> parts = new ArrayList<>();

    private LoanProposalFilterCriteria(String branchKey, List<LoanProposalStatus> statuses, LoanProposalType proposalType) {
        parts.add(Criteria.where("branchCode").is(branchKey));
        parts.add(Criteria.where("dataSource").is(ApiDataSource.OTC));
        parts.add(Criteria.where("isActive").is(true));
        parts.add(proposalType != null
                ? Criteria.where("loanProposalType").is(proposalType)
                : Criteria.where("loanProposalType").ne(LoanProposalType.GOOD_LOAN));
        parts.add(statuses == null || statuses.isEmpty()
                ? Criteria.where("loanProposalStatus").nin(DEFAULT_EXCLUDED_STATUSES)
                : Criteria.where("loanProposalStatus").in(statuses));
    }

    static LoanProposalFilterCriteria of(String branchKey, List<LoanProposalStatus> statuses, LoanProposalType proposalType) {
        return new LoanProposalFilterCriteria(branchKey, statuses, proposalType);
    }

    LoanProposalFilterCriteria eq(String field, Object value) {
        if (value != null) {
            parts.add(Criteria.where(field).is(value));
        }
        return this;
    }

    LoanProposalFilterCriteria applicationDateBetween(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null || toDate != null) {
            Criteria dateCriteria = Criteria.where("applicationDate");
            if (fromDate != null) {
                dateCriteria = dateCriteria.gte(fromDate);
            }
            if (toDate != null) {
                dateCriteria = dateCriteria.lte(toDate);
            }
            parts.add(dateCriteria);
        }
        return this;
    }

    LoanProposalFilterCriteria searchTerm(String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(searchTerm.trim()), Pattern.CASE_INSENSITIVE);
            parts.add(new Criteria().orOperator(
                    Criteria.where("proposalNumber").regex(pattern),
                    Criteria.where("memberName").regex(pattern)));
        }
        return this;
    }

    Criteria build() {
        return new Criteria().andOperator(parts);
    }
}
