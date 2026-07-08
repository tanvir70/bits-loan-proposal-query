package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

public class LoanProposalReadRepositoryImpl implements LoanProposalReadRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public LoanProposalReadRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<LoanProposalReadDocument> findAll(Criteria criteria, Pageable pageable) {
        Query query = new Query(criteria);
        long total = mongoTemplate.count(query, LoanProposalReadDocument.class);
        List<LoanProposalReadDocument> content =
                mongoTemplate.find(query.with(pageable), LoanProposalReadDocument.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long countByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses) {
        return mongoTemplate.count(new Query(memberSchemeCriteria(memberId, schemeId, statuses)),
                LoanProposalReadDocument.class);
    }

    @Override
    public BigDecimal sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses) {
        return sumProposedLoanAmount(memberSchemeCriteria(memberId, schemeId, statuses));
    }

    @Override
    public long countByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses) {
        return mongoTemplate.count(new Query(branchProductCriteria(branchCode, loanProductId, statuses)),
                LoanProposalReadDocument.class);
    }

    @Override
    public BigDecimal sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses) {
        return sumProposedLoanAmount(branchProductCriteria(branchCode, loanProductId, statuses));
    }

    private Criteria memberSchemeCriteria(Long memberId, Long schemeId, List<LoanProposalStatus> statuses) {
        return Criteria.where("memberId").is(memberId)
                .and("schemeId").is(schemeId)
                .and("loanProposalStatus").in(statuses)
                .and("isActive").is(true);
    }

    private Criteria branchProductCriteria(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses) {
        return Criteria.where("branchCode").is(branchCode)
                .and("loanProductId").is(loanProductId)
                .and("loanProposalStatus").in(statuses)
                .and("isActive").is(true);
    }

    private BigDecimal sumProposedLoanAmount(Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("proposedLoanAmount").as("total"));
        Document result = mongoTemplate.aggregate(aggregation, LoanProposalReadDocument.class, Document.class)
                .getUniqueMappedResult();
        if (result == null || result.get("total") == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(result.get("total").toString());
    }
}
