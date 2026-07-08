package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.ListLoanProposalsQuery;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@RegisterQueryHandler
public class ListLoanProposalsQueryHandler
        implements QueryHandler<ListLoanProposalsQuery, Page<LoanProposalListItem>> {

    private final LoanProposalReadRepository readRepository;

    public ListLoanProposalsQueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public Page<LoanProposalListItem> handle(ListLoanProposalsQuery query) {
        Criteria criteria = LoanProposalFilterCriteria
                .of(query.branchKey(), query.statuses(), query.proposalType())
                .eq("villageOrganisationId", query.voId())
                .eq("memberId", query.memberId())
                .eq("loanProductId", query.loanProductId())
                .eq("schemeId", query.schemeId())
                .eq("projectId", query.projectId())
                .applicationDateBetween(query.fromDate(), query.toDate())
                .build();

        PageRequest pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        return readRepository.findAll(criteria, pageable).map(LoanProposalReadMapper::toListItem);
    }
}
