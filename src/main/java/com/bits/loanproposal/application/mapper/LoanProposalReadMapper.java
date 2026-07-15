package com.bits.loanproposal.application.mapper;

import com.bits.loanproposal.application.projection.event.LoanProposalEventPayload;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.presentation.dto.LoanAccountInfo;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse.MonitoringFeedItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanProposalReadMapper {

    @Mapping(target = "creditShieldExpiryDate", source = "creditShieldExpiry")
    @Mapping(target = "fireInsuranceExpiryDate", source = "fireInsuranceExpiry")
    @Mapping(target = "fireInsuranceProductName", source = "fireInsuranceProductName")
    LoanProposalReadDocument toReadDocument(
            LoanProposalEventPayload event,
            LocalDate creditShieldExpiry,
            LocalDate fireInsuranceExpiry,
            String fireInsuranceProductName);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    LoanProposalReadDocument mergeUpdatedFields(
            @MappingTarget LoanProposalReadDocument existing,
            LoanProposalEventPayload event);

    @Mapping(target = "loanAccountBalance", source = "loanAccountInfo.balance")
    @Mapping(target = "loanAccountScheduledAmount", source = "loanAccountInfo.scheduledAmount")
    @Mapping(target = "loanAccountOverdueAmount", source = "loanAccountInfo.overdueAmount")
    @Mapping(target = "loanAccountStatus", source = "loanAccountInfo.status")
    @Mapping(target = "enrollmentStatus", source = "doc.enrollmentStatusOverride")
    LoanProposalResponse toDetailResponse(
            LoanProposalReadDocument doc,
            LoanAccountInfo loanAccountInfo);

    LoanProposalListItem toListItem(LoanProposalReadDocument doc);

    MonitoringFeedItem toMonitoringItem(LoanProposalReadDocument doc);

    @AfterMapping
    default void overwriteFieldsThatPreviouslyAcceptedNulls(
            LoanProposalEventPayload event,
            @MappingTarget LoanProposalReadDocument existing) {
        existing.setId(event.getId());
        existing.setLoanProposalId(event.getLoanProposalId());
        existing.setProposalNumber(event.getProposalNumber());
        existing.setProposalReferenceNumber(event.getProposalReferenceNumber());
        existing.setBranchId(event.getBranchId());
        existing.setBranchCode(event.getBranchCode());
        existing.setProjectId(event.getProjectId());
        existing.setMemberId(event.getMemberId());
        existing.setLoanProductId(event.getLoanProductId());
        existing.setLoanProductDetailsId(event.getLoanProductDetailsId());
        existing.setLoanProductPolicyId(event.getLoanProductPolicyId());
        existing.setSchemeId(event.getSchemeId());
        existing.setFrequencyId(event.getFrequencyId());
        existing.setProposedLoanAmount(event.getProposedLoanAmount());
        existing.setApprovedLoanAmount(event.getApprovedLoanAmount());
        existing.setProposedGrantAmount(event.getProposedGrantAmount());
        existing.setApprovedGrantAmount(event.getApprovedGrantAmount());
        existing.setInstallmentAmount(event.getInstallmentAmount());
        existing.setProposalDurationInMonths(event.getProposalDurationInMonths());
        existing.setLoanProposalType(event.getLoanProposalType());
        existing.setLoanProposalStatus(event.getLoanProposalStatus());
        existing.setDataSource(event.getDataSource());
        existing.setDomainStatus(event.getDomainStatus());
        existing.setFireInsuranceDetails(event.getFireInsuranceDetails());
        existing.setModeOfPayment(event.getModeOfPayment());
        existing.setIsDigitalDisbursement(event.getIsDigitalDisbursement());
        existing.setNominees(event.getNominees());
        existing.setApplicationDate(event.getApplicationDate());
    }
}
