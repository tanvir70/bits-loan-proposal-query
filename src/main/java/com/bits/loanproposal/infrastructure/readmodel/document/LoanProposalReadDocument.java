package com.bits.loanproposal.infrastructure.readmodel.document;

import com.bits.loanproposal.domain.entity.*;
import com.bits.loanproposal.domain.enums.*;
import com.bits.loanproposal.domain.valueobject.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Denormalized read model mirroring the LoanProposal aggregate,
 * plus computed / enriched query-side fields (DDD-REQ-Q001).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "loan_proposal_read")
public class LoanProposalReadDocument {

    @Id
    private String id;
    private Long loanProposalId;
    private String proposalNumber;
    private String proposalReferenceNumber;
    private Long branchId;
    private String branchCode;
    private Long projectId;
    private String projectCode;
    private Long villageOrganisationId;
    private String villageOrganisationCode;
    private Long memberId;
    private String memberName;
    private String memberMobileNumber;
    private Long memberClassificationId;
    private Long loanProductId;
    private Long loanProductDetailsId;
    private Long loanProductPolicyId;
    private Long schemeId;
    private Long sectorId;
    private Long subSectorId;
    private Long frequencyId;
    private BigDecimal proposedLoanAmount;
    private BigDecimal approvedLoanAmount;
    private BigDecimal proposedGrantAmount;
    private BigDecimal approvedGrantAmount;
    private BigDecimal preProposedLoanAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private Integer approvedNumberOfInstallments;
    private BigDecimal installmentAmount;
    private BigDecimal approvedInstallmentAmount;
    private Integer proposalDurationInMonths;
    private Integer approvedDurationInMonths;
    private LoanProposalStatus loanProposalStatus;
    private LoanProposalType loanProposalType;
    private String approvalFlowStatus;
    private String approvalStatus;
    private ApiDataSource dataSource;
    private DomainStatus domainStatus;
    private Boolean microInsurance;
    private Long policyTypeId;
    private Long insuranceProductId;
    private BigDecimal premiumAmount;
    private SecondInsurer secondInsurer;
    private Boolean wantsFireInsurance;
    private Long fireInsuranceProductId;
    private FireInsuranceDetails fireInsuranceDetails;
    private OtcModeOfPayment modeOfPayment;
    private AutoDebitCollection autoDebitCollection;
    private Boolean isDigitalDisbursement;
    private String transactionDescription;
    private List<Nominee> nominees;
    private Guardian guardian;
    private CoBorrower coBorrower;
    private List<Guarantor> guarantors;
    private List<String> specialSavingsAccountIds;
    private List<String> specialSavingsAccountNumbers;
    private Long countryId;
    private Long loanApproverId;
    private BigDecimal totalPovertyScore;
    private Long fieldOfficerId;
    private BigDecimal loanSecurityAmount;
    private BigDecimal loanSecurityBalance;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private LocalDate voDisbursementDate;
    private LocalDate firstRepaymentDate;
    private ProgotiDocumentChecklist progotiDocumentChecklist;
    private Long loanAccountId;
    private BigDecimal disbursedAmount;
    private String disbursedBy;

    // --- Query-side additional fields (DDD-REQ-Q001) ---
    private LocalDate creditShieldExpiryDate;
    private LocalDate fireInsuranceExpiryDate;
    private String fireInsuranceProductName;
    private BigDecimal loanAccountBalance;
    private BigDecimal loanAccountScheduledAmount;
    private BigDecimal loanAccountOverdueAmount;
    private String loanAccountStatus;
    private String enrollmentStatusOverride;
    private Boolean isActive;

    // --- Audit ---
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;
}
