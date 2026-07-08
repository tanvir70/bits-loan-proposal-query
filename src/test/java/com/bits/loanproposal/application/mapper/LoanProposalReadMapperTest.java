package com.bits.loanproposal.application.mapper;

import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import com.bits.loanproposal.domain.entity.Nominee;
import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.loanproposal.domain.enums.DomainStatus;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import com.bits.loanproposal.domain.valueobject.FireInsuranceDetails;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanProposalReadMapperTest {

    @Test
    void mergeUpdatedFieldsUpdatesCommandEventFieldsAndPreservesReadModelOnlyFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 30);
        LoanProposalReadDocument existing = new LoanProposalReadDocument();
        existing.setId("proposal-1");
        existing.setCreatedAt(createdAt);
        existing.setCreatedBy("creator");
        existing.setIsActive(true);
        existing.setMemberName("Existing Member");
        existing.setMemberMobileNumber("01700000000");
        existing.setMicroInsurance(true);
        existing.setWantsFireInsurance(true);
        existing.setFireInsuranceProductId(77L);
        existing.setFireInsuranceProductName("Existing Fire Product");
        existing.setLoanAccountBalance(new BigDecimal("1000.00"));
        existing.setEnrollmentStatusOverride("OVERRIDDEN");

        Nominee nominee = Nominee.builder()
                .id("nominee-1")
                .name("Updated Nominee")
                .relationshipId(5L)
                .sharePercentage(100.0)
                .build();
        FireInsuranceDetails fireInsuranceDetails = new FireInsuranceDetails(
                "Business",
                "Address",
                "Phone",
                "email@example.com",
                1L,
                2L,
                3L,
                4L,
                5L,
                new BigDecimal("10.00"),
                new BigDecimal("1000.00"),
                24,
                "Payload Product",
                new BigDecimal("1.00"),
                new BigDecimal("2.00"));

        LoanProposalUpdatedEvent event = new LoanProposalUpdatedEvent();
        event.setId("proposal-1");
        event.setLoanProposalId(1001L);
        event.setProposalNumber("LP-2026-001");
        event.setProposalReferenceNumber("REF-001");
        event.setBranchId(10L);
        event.setBranchCode("0010");
        event.setProjectId(20L);
        event.setMemberId(30L);
        event.setLoanProductId(40L);
        event.setLoanProductDetailsId(41L);
        event.setLoanProductPolicyId(42L);
        event.setSchemeId(50L);
        event.setFrequencyId(60L);
        event.setProposedLoanAmount(new BigDecimal("50000.00"));
        event.setApprovedLoanAmount(new BigDecimal("45000.00"));
        event.setProposedGrantAmount(new BigDecimal("500.00"));
        event.setApprovedGrantAmount(new BigDecimal("400.00"));
        event.setInstallmentAmount(new BigDecimal("2500.00"));
        event.setProposalDurationInMonths(12);
        event.setLoanProposalType(LoanProposalType.NORMAL_LOAN);
        event.setLoanProposalStatus(LoanProposalStatus.PENDING);
        event.setDataSource(ApiDataSource.OTC);
        event.setDomainStatus(DomainStatus.UPDATED);
        event.setIsDigitalDisbursement(false);
        event.setNominees(List.of(nominee));
        event.setFireInsuranceDetails(fireInsuranceDetails);
        event.setApplicationDate(LocalDate.of(2026, 2, 1));

        LoanProposalReadDocument merged = LoanProposalReadMapper.mergeUpdatedFields(existing, event);

        assertThat(merged.getProposalNumber()).isEqualTo("LP-2026-001");
        assertThat(merged.getProposalReferenceNumber()).isEqualTo("REF-001");
        assertThat(merged.getProposedLoanAmount()).isEqualByComparingTo("50000.00");
        assertThat(merged.getApprovedLoanAmount()).isEqualByComparingTo("45000.00");
        assertThat(merged.getProposalDurationInMonths()).isEqualTo(12);
        assertThat(merged.getLoanProposalStatus()).isEqualTo(LoanProposalStatus.PENDING);
        assertThat(merged.getDomainStatus()).isEqualTo(DomainStatus.UPDATED);
        assertThat(merged.getNominees()).containsExactly(nominee);
        assertThat(merged.getFireInsuranceDetails()).isEqualTo(fireInsuranceDetails);
        assertThat(merged.getApplicationDate()).isEqualTo(LocalDate.of(2026, 2, 1));

        assertThat(merged.getCreatedAt()).isEqualTo(createdAt);
        assertThat(merged.getCreatedBy()).isEqualTo("creator");
        assertThat(merged.getIsActive()).isTrue();
        assertThat(merged.getMemberName()).isEqualTo("Existing Member");
        assertThat(merged.getMemberMobileNumber()).isEqualTo("01700000000");
        assertThat(merged.getMicroInsurance()).isTrue();
        assertThat(merged.getWantsFireInsurance()).isTrue();
        assertThat(merged.getFireInsuranceProductId()).isEqualTo(77L);
        assertThat(merged.getFireInsuranceProductName()).isEqualTo("Existing Fire Product");
        assertThat(merged.getLoanAccountBalance()).isEqualByComparingTo("1000.00");
        assertThat(merged.getEnrollmentStatusOverride()).isEqualTo("OVERRIDDEN");
    }
}
