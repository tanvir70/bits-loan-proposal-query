package com.bits.loanproposal.application.mapper;

import com.bits.loanproposal.application.projection.event.LoanProposalEventPayload;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.presentation.dto.LoanAccountInfo;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse.MonitoringFeedItem;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

public final class LoanProposalReadMapper {

    private LoanProposalReadMapper() {
    }

    public static LoanProposalReadDocument toReadDocument(
            LoanProposalEventPayload event,
            LocalDate creditShieldExpiry,
            LocalDate fireInsuranceExpiry,
            String fireInsuranceProductName) {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        // ponytail: event payload field names mirror the document — property copy instead of ~80 setters
        BeanUtils.copyProperties(event, doc);
        doc.setCreditShieldExpiryDate(creditShieldExpiry);
        doc.setFireInsuranceExpiryDate(fireInsuranceExpiry);
        doc.setFireInsuranceProductName(fireInsuranceProductName);
        return doc;
    }

    public static LoanProposalReadDocument mergeUpdatedFields(
            LoanProposalReadDocument existing,
            LoanProposalEventPayload event,
            LocalDate creditShieldExpiry,
            LocalDate fireInsuranceExpiry,
            String fireInsuranceProductName) {
        BeanUtils.copyProperties(event, existing, "createdAt", "createdBy");
        existing.setCreditShieldExpiryDate(creditShieldExpiry);
        existing.setFireInsuranceExpiryDate(fireInsuranceExpiry);
        existing.setFireInsuranceProductName(fireInsuranceProductName);
        return existing;
    }

    public static LoanProposalResponse toDetailResponse(
            LoanProposalReadDocument doc,
            LocalDate firstRepaymentDate,
            LocalDate creditShieldExpiry,
            LocalDate fireInsuranceExpiry,
            LoanAccountInfo loanAccountInfo,
            String enrollmentStatus) {
        return LoanProposalResponse.builder()
                .id(doc.getId())
                .loanProposalId(doc.getLoanProposalId())
                .proposalNumber(doc.getProposalNumber())
                .proposalReferenceNumber(doc.getProposalReferenceNumber())
                .branchId(doc.getBranchId())
                .branchCode(doc.getBranchCode())
                .projectId(doc.getProjectId())
                .projectCode(doc.getProjectCode())
                .villageOrganisationId(doc.getVillageOrganisationId())
                .villageOrganisationCode(doc.getVillageOrganisationCode())
                .memberId(doc.getMemberId())
                .memberClassificationId(doc.getMemberClassificationId())
                .loanProductId(doc.getLoanProductId())
                .loanProductDetailsId(doc.getLoanProductDetailsId())
                .loanProductPolicyId(doc.getLoanProductPolicyId())
                .schemeId(doc.getSchemeId())
                .sectorId(doc.getSectorId())
                .subSectorId(doc.getSubSectorId())
                .frequencyId(doc.getFrequencyId())
                .proposedLoanAmount(doc.getProposedLoanAmount())
                .approvedLoanAmount(doc.getApprovedLoanAmount())
                .proposedGrantAmount(doc.getProposedGrantAmount())
                .approvedGrantAmount(doc.getApprovedGrantAmount())
                .interestRate(doc.getInterestRate())
                .numberOfInstallments(doc.getNumberOfInstallments())
                .approvedNumberOfInstallments(doc.getApprovedNumberOfInstallments())
                .installmentAmount(doc.getInstallmentAmount())
                .approvedInstallmentAmount(doc.getApprovedInstallmentAmount())
                .proposalDurationInMonths(doc.getProposalDurationInMonths())
                .approvedDurationInMonths(doc.getApprovedDurationInMonths())
                .loanProposalStatus(doc.getLoanProposalStatus())
                .loanProposalType(doc.getLoanProposalType())
                .dataSource(doc.getDataSource())
                .microInsurance(doc.getMicroInsurance())
                .policyTypeId(doc.getPolicyTypeId())
                .insuranceProductId(doc.getInsuranceProductId())
                .premiumAmount(doc.getPremiumAmount())
                .secondInsurer(doc.getSecondInsurer())
                .wantsFireInsurance(doc.getWantsFireInsurance())
                .fireInsuranceProductId(doc.getFireInsuranceProductId())
                .fireInsuranceProductName(doc.getFireInsuranceProductName())
                .fireInsuranceDetails(doc.getFireInsuranceDetails())
                .modeOfPayment(doc.getModeOfPayment())
                .autoDebitCollection(doc.getAutoDebitCollection())
                .isDigitalDisbursement(doc.getIsDigitalDisbursement())
                .nominees(doc.getNominees())
                .guardian(doc.getGuardian())
                .coBorrower(doc.getCoBorrower())
                .guarantors(doc.getGuarantors())
                .countryId(doc.getCountryId())
                .loanApproverId(doc.getLoanApproverId())
                .totalPovertyScore(doc.getTotalPovertyScore())
                .applicationDate(doc.getApplicationDate())
                .disbursementDate(doc.getDisbursementDate())
                .firstRepaymentDate(firstRepaymentDate)
                .creditShieldExpiryDate(creditShieldExpiry)
                .fireInsuranceExpiryDate(fireInsuranceExpiry)
                .loanAccountId(doc.getLoanAccountId())
                .loanAccountBalance(loanAccountInfo != null ? loanAccountInfo.balance() : null)
                .loanAccountScheduledAmount(loanAccountInfo != null ? loanAccountInfo.scheduledAmount() : null)
                .loanAccountOverdueAmount(loanAccountInfo != null ? loanAccountInfo.overdueAmount() : null)
                .loanAccountStatus(loanAccountInfo != null ? loanAccountInfo.status() : null)
                .enrollmentStatus(enrollmentStatus)
                .progotiDocumentChecklist(doc.getProgotiDocumentChecklist())
                .createdAt(doc.getCreatedAt())
                .createdBy(doc.getCreatedBy())
                .lastModifiedAt(doc.getLastModifiedAt())
                .lastModifiedBy(doc.getLastModifiedBy())
                .disbursedAmount(doc.getDisbursedAmount())
                .disbursedBy(doc.getDisbursedBy())
                .build();
    }

    public static LoanProposalListItem toListItem(LoanProposalReadDocument doc) {
        return LoanProposalListItem.builder()
                .id(doc.getId())
                .proposalNumber(doc.getProposalNumber())
                .memberId(doc.getMemberId())
                .memberName(doc.getMemberName())
                .memberMobileNumber(doc.getMemberMobileNumber())
                .branchId(doc.getBranchId())
                .branchCode(doc.getBranchCode())
                .projectId(doc.getProjectId())
                .projectCode(doc.getProjectCode())
                .villageOrganisationId(doc.getVillageOrganisationId())
                .villageOrganisationCode(doc.getVillageOrganisationCode())
                .loanProductId(doc.getLoanProductId())
                .schemeId(doc.getSchemeId())
                .proposedLoanAmount(doc.getProposedLoanAmount())
                .approvedLoanAmount(doc.getApprovedLoanAmount())
                .loanProposalStatus(doc.getLoanProposalStatus())
                .loanProposalType(doc.getLoanProposalType())
                .applicationDate(doc.getApplicationDate())
                .approvalFlowStatus(doc.getApprovalFlowStatus())
                .dataSource(doc.getDataSource())
                .isDigitalDisbursement(doc.getIsDigitalDisbursement())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public static MonitoringFeedItem toMonitoringItem(LoanProposalReadDocument doc) {
        return MonitoringFeedItem.builder()
                .id(doc.getId())
                .proposalNumber(doc.getProposalNumber())
                .memberId(doc.getMemberId())
                .memberName(doc.getMemberName())
                .branchCode(doc.getBranchCode())
                .projectCode(doc.getProjectCode())
                .proposedLoanAmount(doc.getProposedLoanAmount())
                .loanProposalStatus(doc.getLoanProposalStatus())
                .dataSource(doc.getDataSource())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
