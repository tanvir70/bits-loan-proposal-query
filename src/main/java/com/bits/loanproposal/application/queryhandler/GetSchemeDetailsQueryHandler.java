package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.ddd.shared.exception.domain.BusinessRuleViolationException;
import com.bits.ddd.shared.localization.LocalizedMessage;
import com.bits.loanproposal.application.query.GetSchemeDetailsQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.MemberSnapshotRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.SchemeSnapshotRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.VillageOrganisationSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.VillageOrganisationSnapshotRepository;
import com.bits.loanproposal.presentation.dto.SchemeDetailsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RegisterQueryHandler
public class GetSchemeDetailsQueryHandler
        implements QueryHandler<GetSchemeDetailsQuery, SchemeDetailsResponse> {

    private static final List<LoanProposalStatus> ACTIVE_STATUSES =
            List.of(LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);

    private final LoanProposalReadRepository readRepository;
    private final MemberSnapshotRepository memberSnapshotRepository;
    private final VillageOrganisationSnapshotRepository villageOrganisationSnapshotRepository;
    private final SchemeSnapshotRepository schemeSnapshotRepository;

    public GetSchemeDetailsQueryHandler(
            LoanProposalReadRepository readRepository,
            MemberSnapshotRepository memberSnapshotRepository,
            VillageOrganisationSnapshotRepository villageOrganisationSnapshotRepository,
            SchemeSnapshotRepository schemeSnapshotRepository) {
        this.readRepository = readRepository;
        this.memberSnapshotRepository = memberSnapshotRepository;
        this.villageOrganisationSnapshotRepository = villageOrganisationSnapshotRepository;
        this.schemeSnapshotRepository = schemeSnapshotRepository;
    }

    @Override
    public SchemeDetailsResponse handle(GetSchemeDetailsQuery query) {
        if (memberSnapshotRepository.findById(query.memberId()).isEmpty()) {
            throw new BusinessRuleViolationException(
                    query.getQueryIdentifier(), query.getQueryType(),
                    Map.of("memberId", LocalizedMessage.builder()
                            .key("MEMBER_NOT_FOUND")
                            .args(new Object[]{query.memberId()})
                            .build()));
        }

        String voCategory = null;
        if (query.voId() != null) {
            voCategory = villageOrganisationSnapshotRepository.findById(query.voId())
                    .map(VillageOrganisationSnapshotDocument::getCategory)
                    .orElse(null);
        }

        long existingLoanCount = readRepository.countByMemberIdAndSchemeIdAndStatuses(
                query.memberId(), query.schemeId(), ACTIVE_STATUSES);
        BigDecimal existingLoanTotal = readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(
                query.memberId(), query.schemeId(), ACTIVE_STATUSES);

        String voCategoryFinal = voCategory;
        BigDecimal assetGrantPercentage = schemeSnapshotRepository.findById(query.schemeId())
                .map(scheme -> scheme.assetGrantPercentageForVoCategory(voCategoryFinal))
                .orElse(BigDecimal.ZERO);

        return new SchemeDetailsResponse(
                query.schemeId(),
                true,
                voCategory,
                (int) existingLoanCount,
                existingLoanTotal,
                assetGrantPercentage);
    }
}
