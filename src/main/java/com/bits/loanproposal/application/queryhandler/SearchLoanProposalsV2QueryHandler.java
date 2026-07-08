package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.SearchLoanProposalsV2Query;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@RegisterQueryHandler
public class SearchLoanProposalsV2QueryHandler
        implements QueryHandler<SearchLoanProposalsV2Query, Page<LoanProposalListItem>> {

    private final LoanProposalReadRepository readRepository;

    public SearchLoanProposalsV2QueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public Page<LoanProposalListItem> handle(SearchLoanProposalsV2Query query) {
        Criteria criteria = LoanProposalFilterCriteria
                .of(query.branchKey(), query.statuses(), query.proposalType())
                .searchTerm(query.searchTerm())
                .eq("villageOrganisationId", query.voId())
                .eq("memberId", query.memberId())
                .eq("loanProductId", query.loanProductId())
                .applicationDateBetween(query.fromDate(), query.toDate())
                .build();

        PageRequest pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        return readRepository.findAll(criteria, pageable).map(LoanProposalReadMapper::toListItem);
    }
}
