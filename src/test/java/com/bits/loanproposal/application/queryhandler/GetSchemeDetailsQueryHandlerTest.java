package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.shared.exception.domain.BusinessRuleViolationException;
import com.bits.loanproposal.application.query.GetSchemeDetailsQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.MemberSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.MemberSnapshotRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.SchemeSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.SchemeSnapshotRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.VillageOrganisationSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.VillageOrganisationSnapshotRepository;
import com.bits.loanproposal.presentation.dto.SchemeDetailsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSchemeDetailsQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;
    @Mock
    private MemberSnapshotRepository memberSnapshotRepository;
    @Mock
    private VillageOrganisationSnapshotRepository villageOrganisationSnapshotRepository;
    @Mock
    private SchemeSnapshotRepository schemeSnapshotRepository;

    @InjectMocks
    private GetSchemeDetailsQueryHandler handler;

    @Test
    void handleFetchesSchemeDetailsAndAssetGrant() {
        when(memberSnapshotRepository.findById(30L)).thenReturn(Optional.of(new MemberSnapshotDocument()));
        
        VillageOrganisationSnapshotDocument vo = new VillageOrganisationSnapshotDocument();
        vo.setVoId(15L);
        vo.setCategory("CAT_A");
        when(villageOrganisationSnapshotRepository.findById(15L)).thenReturn(Optional.of(vo));

        SchemeSnapshotDocument scheme = new SchemeSnapshotDocument();
        scheme.setAssetGrantPercentageByVoCategory(Map.of("CAT_A", new BigDecimal("50.00")));
        when(schemeSnapshotRepository.findById(50L)).thenReturn(Optional.of(scheme));

        List<LoanProposalStatus> activeStatuses = List.of(
                LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);
        when(readRepository.countByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(2L);
        when(readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(new BigDecimal("100000.00"));

        GetSchemeDetailsQuery query = new GetSchemeDetailsQuery("trace-1", 30L, 40L, 50L, 10L, 15L);
        SchemeDetailsResponse response = handler.handle(query);

        assertThat(response.schemeId()).isEqualTo(50L);
        assertThat(response.memberFound()).isTrue();
        assertThat(response.voCategory()).isEqualTo("CAT_A");
        assertThat(response.existingLoanCount()).isEqualTo(2);
        assertThat(response.existingLoanTotal()).isEqualByComparingTo("100000.00");
        assertThat(response.assetGrantPercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void handleThrowsExceptionWhenMemberNotFound() {
        when(memberSnapshotRepository.findById(30L)).thenReturn(Optional.empty());

        GetSchemeDetailsQuery query = new GetSchemeDetailsQuery("trace-1", 30L, 40L, 50L, 10L, 15L);

        assertThatThrownBy(() -> handler.handle(query))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void handleReturnsDetailsWhenVoIdIsNull() {
        when(memberSnapshotRepository.findById(30L)).thenReturn(Optional.of(new MemberSnapshotDocument()));

        SchemeSnapshotDocument scheme = new SchemeSnapshotDocument();
        scheme.setAssetGrantPercentageByVoCategory(Map.of("CAT_A", new BigDecimal("50.00")));
        when(schemeSnapshotRepository.findById(50L)).thenReturn(Optional.of(scheme));

        List<LoanProposalStatus> activeStatuses = List.of(
                LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);
        when(readRepository.countByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(2L);
        when(readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(new BigDecimal("100000.00"));

        GetSchemeDetailsQuery query = new GetSchemeDetailsQuery("trace-1", 30L, 40L, 50L, 10L, null);
        SchemeDetailsResponse response = handler.handle(query);

        assertThat(response.schemeId()).isEqualTo(50L);
        assertThat(response.memberFound()).isTrue();
        assertThat(response.voCategory()).isNull();
        assertThat(response.existingLoanCount()).isEqualTo(2);
        assertThat(response.existingLoanTotal()).isEqualByComparingTo("100000.00");
        assertThat(response.assetGrantPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void handleReturnsZeroAssetGrantWhenVoNotFound() {
        when(memberSnapshotRepository.findById(30L)).thenReturn(Optional.of(new MemberSnapshotDocument()));
        when(villageOrganisationSnapshotRepository.findById(15L)).thenReturn(Optional.empty());

        SchemeSnapshotDocument scheme = new SchemeSnapshotDocument();
        scheme.setAssetGrantPercentageByVoCategory(Map.of("CAT_A", new BigDecimal("50.00")));
        when(schemeSnapshotRepository.findById(50L)).thenReturn(Optional.of(scheme));

        List<LoanProposalStatus> activeStatuses = List.of(
                LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);
        when(readRepository.countByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(2L);
        when(readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(new BigDecimal("100000.00"));

        GetSchemeDetailsQuery query = new GetSchemeDetailsQuery("trace-1", 30L, 40L, 50L, 10L, 15L);
        SchemeDetailsResponse response = handler.handle(query);

        assertThat(response.schemeId()).isEqualTo(50L);
        assertThat(response.memberFound()).isTrue();
        assertThat(response.voCategory()).isNull();
        assertThat(response.existingLoanCount()).isEqualTo(2);
        assertThat(response.existingLoanTotal()).isEqualByComparingTo("100000.00");
        assertThat(response.assetGrantPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void handleReturnsZeroAssetGrantWhenSchemeNotFound() {
        when(memberSnapshotRepository.findById(30L)).thenReturn(Optional.of(new MemberSnapshotDocument()));
        
        VillageOrganisationSnapshotDocument vo = new VillageOrganisationSnapshotDocument();
        vo.setVoId(15L);
        vo.setCategory("CAT_A");
        when(villageOrganisationSnapshotRepository.findById(15L)).thenReturn(Optional.of(vo));

        when(schemeSnapshotRepository.findById(50L)).thenReturn(Optional.empty());

        List<LoanProposalStatus> activeStatuses = List.of(
                LoanProposalStatus.PENDING, LoanProposalStatus.APPROVED, LoanProposalStatus.DISBURSED);
        when(readRepository.countByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(2L);
        when(readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(30L, 50L, activeStatuses))
                .thenReturn(new BigDecimal("100000.00"));

        GetSchemeDetailsQuery query = new GetSchemeDetailsQuery("trace-1", 30L, 40L, 50L, 10L, 15L);
        SchemeDetailsResponse response = handler.handle(query);

        assertThat(response.schemeId()).isEqualTo(50L);
        assertThat(response.memberFound()).isTrue();
        assertThat(response.voCategory()).isEqualTo("CAT_A");
        assertThat(response.existingLoanCount()).isEqualTo(2);
        assertThat(response.existingLoanTotal()).isEqualByComparingTo("100000.00");
        assertThat(response.assetGrantPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
