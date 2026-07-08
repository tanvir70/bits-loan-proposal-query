package com.bits.loanproposal.presentation.dto;

import com.bits.loanproposal.domain.entity.*;
import com.bits.loanproposal.domain.enums.*;
import com.bits.loanproposal.domain.valueobject.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full-detail response for getById (DDD-REQ-Q015).
 */
@Builder
public record LoanProposalResponse(
        String id,
        Long loanProposalId,
        String proposalNumber,
        String proposalReferenceNumber,
        Long branchId,
        String branchCode,
        Long projectId,
        String projectCode,
        Long villageOrganisationId,
        String villageOrganisationCode,
        Long memberId,
        Long memberClassificationId,
        Long loanProductId,
        Long loanProductDetailsId,
        Long loanProductPolicyId,
        Long schemeId,
        Long sectorId,
        Long subSectorId,
        Long frequencyId,
        BigDecimal proposedLoanAmount,
        BigDecimal approvedLoanAmount,
        BigDecimal proposedGrantAmount,
        BigDecimal approvedGrantAmount,
        BigDecimal interestRate,
        Integer numberOfInstallments,
        Integer approvedNumberOfInstallments,
        BigDecimal installmentAmount,
        BigDecimal approvedInstallmentAmount,
        Integer proposalDurationInMonths,
        Integer approvedDurationInMonths,
        LoanProposalStatus loanProposalStatus,
        LoanProposalType loanProposalType,
        ApiDataSource dataSource,
        Boolean microInsurance,
        Long policyTypeId,
        Long insuranceProductId,
        BigDecimal premiumAmount,
        SecondInsurer secondInsurer,
        Boolean wantsFireInsurance,
        Long fireInsuranceProductId,
        String fireInsuranceProductName,
        FireInsuranceDetails fireInsuranceDetails,
        OtcModeOfPayment modeOfPayment,
        AutoDebitCollection autoDebitCollection,
        Boolean isDigitalDisbursement,
        List<Nominee> nominees,
        Guardian guardian,
        CoBorrower coBorrower,
        List<Guarantor> guarantors,
        Long countryId,
        Long loanApproverId,
        BigDecimal totalPovertyScore,
        LocalDate applicationDate,
        LocalDate disbursementDate,
        LocalDate firstRepaymentDate,
        LocalDate creditShieldExpiryDate,
        LocalDate fireInsuranceExpiryDate,
        Long loanAccountId,
        BigDecimal loanAccountBalance,
        BigDecimal loanAccountScheduledAmount,
        BigDecimal loanAccountOverdueAmount,
        String loanAccountStatus,
        String enrollmentStatus,
        ProgotiDocumentChecklist progotiDocumentChecklist,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime lastModifiedAt,
        String lastModifiedBy,
        BigDecimal disbursedAmount,
        String disbursedBy
) {}
