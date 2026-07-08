# Loan Proposal — DDD/CQRS/Event-Sourcing Business Requirements Specification (EARS) — Command Side

> **Format:** Easy Approach to Requirements Syntax (EARS) — DDD/CQRS/ES Edition
> **Source:** Converted from layered-architecture EARS spec `LoanProposalOTC-EARS-review-2-resolved.md`
> **Architecture:** bits.ddd command-side service
> **Aggregate:** `LoanProposal` — MongoDB document, `loan_proposal` collection
> **Output path:** `output/loan-proposal/LoanProposal-Command-DDD-EARS.md`
> **Traceability:** Every DDD-REQ cites the source EARS section it was derived from,
>   in the format: 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "{Section / Sub-section heading}"

---

## Planning Phase Gate Decisions

> This section records every architectural decision confirmed by the product owner / domain expert
> during the interactive planning phase (Phases 0–5 of the SKILL.md process). Each gate
> decision is binding: the DDD-REQ requirements below must implement exactly the choice made here.
> No gate may be re-opened without explicit sign-off.
>
> **Source:** Gate Decision Ledger maintained by the agent throughout Phases 0–5.

### Gate 0a — Source File Approach
**Phase:** 0 — Source File Confirmation
**Question asked:** "Is your EARS source a single file or multiple files?"
**Decision:** ✅ Single file
**Architectural effect:** All `📎 Source:` citations use the single filename `LoanProposalOTC-EARS-review-2-resolved.md` throughout; no deduplication registry needed.

### Gate 0b — Source File Path
**Phase:** 0 — Source File Confirmation
**Question asked:** "Please confirm the exact path to the source EARS file."
**Decision:** ✅ `source/LoanProposalOTC-EARS-review-2-resolved.md`
**Architectural effect:** All section citations reference this file by its base filename.

### Gate 1a — Domain Name, Aggregate Root, and Domain Slug
**Phase:** 1 — Domain & Boundary Discovery
**Question asked:** "Please confirm or override the Domain Name, Aggregate Root name, and domain slug."
**Decision:** ✅ Domain Name: `Loan Proposal`; Aggregate Root: `LoanProposal`; Domain Slug: `loan-proposal`
**Architectural effect:** Output files written under `output/loan-proposal/`; AR class named `LoanProposal`; MongoDB collection named `loan_proposal`.

### Gate 1b — AR Behaviors and Query Operations
**Phase:** 1 — Domain & Boundary Discovery
**Question asked:** "Please confirm the Aggregate Root behaviors. Are any behavior names incorrect, or are any behaviors completely missing?"
**Decision:** ✅ Command-side AR behaviors: `create`, `update`, `delete`. Query-side operations: `getById`, `list`, `searchV2`, `getSchemeDetails`, `getUPGTUPExistingLoans`, `monitoringFeed`. Seven async/pipeline/maintenance operations moved to Out of Scope.
**Architectural effect:** Three Command Handlers, three AR methods, three domain events. Six Query Handlers and one Projection Handler on the query side.

### Gate 2a — Entity Roles
**Phase:** 2 — Structural Schema Definition
**Question asked:** "Please confirm or override the suggested role for each entity."
**Decision:** ✅ 1 AGGREGATE_ROOT (`LoanProposal`); 5 ENTITY (`Nominee`, `Guardian`, `CoBorrower`, `SecondInsurer`, `Guarantor`); 4 VALUE_OBJECT (`FireInsuranceDetails`, `OtcModeOfPayment`, `AutoDebitCollection`, `ProgotiDocumentChecklist`); 12 SOURCE_DATA (`Member`, `LoanProduct`, `LoanProductDetails`, `LoanProductPolicy`, `Scheme`, `Project`, `ProjectPolicy`, `Branch`, `VillageOrganisation`, `InsuranceProduct`, `Country`, `Bank`)
**Architectural effect:** Governs the AR schema and embedded document structure, value object immutability, and the source data service entity type map.

### Gate 2b — Source Data Sourcing Mechanism
**Phase:** 2 — Structural Schema Definition
**Question asked:** "How will source data be collected? Options: [1] EVENT or [2] HTTP."
**Decision:** ✅ EVENT — RabbitMQ listener captures external entity events into local MongoDB snapshot collections.
**Architectural effect:** Source data pipeline uses `@RabbitListener`-based snapshot ingestion; no HTTP client calls generated. Twelve listener + repository DDD-REQs produced.

### Gate 2c — Source Data Event Listener Implementation
**Phase:** 2 — Structural Schema Definition
**Question asked:** "Do you want to implement the listeners to source data through events now?"
**Decision:** ✅ Yes — generate full listener pipeline specifications for all 12 SOURCE_DATA entities.
**Architectural effect:** DDD-REQ-040 covers all 12 `@RabbitListener` event handler + MongoDB repository specifications.

### Gate 3a — Request Entry Points per Behavior
**Phase:** 3 — Action-by-Action Breakdown
**Question asked:** "How does the request for each behavior arrive? Options: [1] REST API or [2] Event Listener."
**Decision:** ✅ Create → REST API (POST); Update → Event Listener (`@RabbitListener`); Delete → Both REST API + Event Listener (`@RabbitListener`)
**Architectural effect:** `LoanProposalCommandController` exposes POST (create) and DELETE endpoints. `LoanProposalCommandListener` handles Update and Delete events. Delete has dual entry points.

### Gate 3b — Domain Specification Categories and Composition
**Phase:** 3 — Action-by-Action Breakdown
**Question asked:** "What is the logical composition strategy for specifications? Please confirm the 21 specification categories."
**Decision:** ✅ 21 domain specification categories confirmed (strict AND chain for `create` and `update`; HANDLER_GUARD only for `delete`).
**Architectural effect:** 21 `Specification<ValidationContext>` classes chained via `.and()` in AR `create()` and `update()` behavior pseudocode. `delete()` has status guard only — no `ValidationContext` constructed, no source data fetched.

### Gate 4 — Output Format
**Phase:** 4 — Query & Read Model Definition (CQRS Split)
**Question asked:** "Would you like to generate a single unified specification file, or split the output into two separate architecture files?"
**Decision:** ✅ Split — `output/loan-proposal/LoanProposal-Command-DDD-EARS.md` and `output/loan-proposal/LoanProposal-Query-DDD-EARS.md`
**Architectural effect:** This file covers the full command side. The query side (read model, projection handler, query handlers, query controller) is in the companion Query file.

### Gate 5a — Saga Management
**Phase:** 5 — Saga, Integration & Cross-Cutting Concerns
**Question asked:** "Does this domain require Saga management or Process Managers for distributed transactions?"
**Decision:** ✅ No Saga — this domain owns its full transaction boundary; no Process Manager needed.
**Architectural effect:** No Saga or Process Manager DDD-REQs generated. Each command handler is a self-contained transaction.

### Gate 5b — Security and Idempotency
**Phase:** 5 — Saga, Integration & Cross-Cutting Concerns
**Question asked:** "Are there any specific idempotency keys, security token policies, or audit-logging structures required?"
**Decision:** ✅ OAuth2 bearer token authentication; database-driven path-and-role access control; client-supplied proposal ID serves as idempotency key (duplicate ID rejected with "Buffer Loan Proposal already exists with given id."); standard creation/modification/deletion audit trail per Cross-Cutting Requirements.
**Architectural effect:** Security and audit DDD-REQs copied verbatim from EARS Cross-Cutting sections. Unique index on `_id` enforces idempotency at the database level.

---

## Document Conventions

| Marker | Meaning |
|--------|---------|
| `[INFERRED]` | Required by bits.ddd pattern; no explicit requirement in source EARS |
| `[UNCHANGED]` | Copied verbatim from the original EARS (cross-cutting concern) |
| `[PSEUDOCODE]` | Java-adjacent pseudocode block — describes behaviour without full implementation syntax |
| `[READ-MODEL]` | CQRS query-side requirement — covered in companion Query file |
| `[ASYNC]` | Asynchronous side effect — out of scope for this DDD specification |
| `[OUT-OF-SCOPE]` | Identified in source EARS but excluded from this DDD specification |
| `[DISABLED]` | Feature exists in source EARS but is currently switched off |

**Source citation format (all DDD-REQs use this):**
```
📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "{Section / Sub-section heading}"
```

---

## Behavior Cross-Reference Index

> This index cross-references every behavior to its related DDD-REQ numbers across all layers.
> Use it to find all specifications for a given behavior without scanning the full document.
> Query-side operations are covered in the companion `LoanProposal-Query-DDD-EARS.md` file.

### Command-Side Behaviors

| Behavior | Entry Point | Command | Handler | Aggregate Method | Event Emitted | Read Model Effect | DDD-REQ Numbers |
|----------|------------|---------|---------|-----------------|--------------|-------------------|-----------------|
| Create | POST `/api/loan-proposals` | `CreateLoanProposalCommand` | `CreateLoanProposalCommandHandler` | `.create(creationData)` | `LoanProposalCreatedEvent` | Full upsert to read model | DDD-REQ-001, 002, 009–029, 030, 031, 033, 036, 037, 038, 039, 040, 041, 045, 046, 047, 050, 053 |
| Update | `@RabbitListener` (update queue) | `UpdateLoanProposalCommand` | `UpdateLoanProposalCommandHandler` | `.update(updateData)` | `LoanProposalUpdatedEvent` | Upsert updated fields | DDD-REQ-001, 002, 009–029, 030, 031, 034, 036, 037, 038, 039, 040, 042, 044, 045, 046, 048, 051, 054 |
| Delete | DELETE `/api/loan-proposals/{id}` + `@RabbitListener` (delete queue) | `DeleteLoanProposalCommand` | `DeleteLoanProposalCommandHandler` | `.delete(deletionData)` | `LoanProposalDeletedEvent` | Mark inactive (minimal record) | DDD-REQ-001, 002, 031, 035, 043, 044, 045, 046, 049, 052, 053, 054 |

### Query-Side Operations

> Full query-side behavior cross-reference is in `LoanProposal-Query-DDD-EARS.md`.

| Operation | Entry Point | Query | Handler |
|-----------|------------|-------|---------|
| Get by ID | GET `/api/loan-proposals/{branchKey}/{id}` | `GetLoanProposalByIdQuery` | `GetLoanProposalByIdQueryHandler` |
| List | GET `/api/loan-proposals/{branchKey}` | `ListLoanProposalsQuery` | `ListLoanProposalsQueryHandler` |
| Search V2 | GET `/api/loan-proposals/v2/{branchKey}` | `SearchLoanProposalsV2Query` | `SearchLoanProposalsV2QueryHandler` |
| Get Scheme Details | GET `/api/loan-proposals/scheme-details` | `GetSchemeDetailsQuery` | `GetSchemeDetailsQueryHandler` |
| Get UPG/TUP Existing Loans | GET `/api/loan-proposals/upg-tup/{branchKey}` | `GetUPGTUPExistingLoansQuery` | `GetUPGTUPExistingLoansQueryHandler` |
| Monitoring Feed | GET `/api/loan-proposals/monitor` | `GetMonitoringFeedQuery` | `GetMonitoringFeedQueryHandler` |

---

## Domain Layer

### Command-Side Inventory Matrix

| # | Item Type | Count | Items |
|---|-----------|-------|-------|
| 1 | Aggregate Roots | 1 | `LoanProposal` |
| 2 | Value Objects | 4 | `FireInsuranceDetails`, `OtcModeOfPayment`, `AutoDebitCollection`, `ProgotiDocumentChecklist` |
| 3 | Child Entities | 5 | `Nominee`, `Guardian`, `CoBorrower`, `SecondInsurer`, `Guarantor` |
| 4 | Source Data Elements | 12 | `Member`, `LoanProduct`, `LoanProductDetails`, `LoanProductPolicy`, `Scheme`, `Project`, `ProjectPolicy`, `Branch`, `VillageOrganisation`, `InsuranceProduct`, `Country`, `Bank` |
| 5 | Validation Specification Categories | 21 | `MemberEligibilitySpecification`, `BranchProjectVoConsistencySpecification`, `LoanProductPolicySpecification`, `RepaymentFrequencyModeOfPaymentSpecification`, `LoanAmountGrantInstallmentSpecification`, `LoanExposureLimitSpecification`, `CoBorrowerSpecification`, `InsurancePolicyTypeSecondInsurerSpecification`, `NomineeSpecification`, `SpecialSavingsLienSpecification`, `ProjectSpecificRulesSpecification`, `ParallelCoExistingLoanSpecification`, `InstallmentConfigurationSpecification`, `ModeOfPaymentRocketWalletSpecification`, `DigitalDisbursementSpecification`, `MigrationCountrySpecification`, `FireInsuranceSpecification`, `AgeLimitSpecification`, `MoneyPlantSpecification`, `SchemeSectorMappingSpecification`, `BankModeOfPaymentSpecification` |
| 6 | Commands | 3 | `CreateLoanProposalCommand`, `UpdateLoanProposalCommand`, `DeleteLoanProposalCommand` |
| 7 | Command Handlers | 3 | `CreateLoanProposalCommandHandler`, `UpdateLoanProposalCommandHandler`, `DeleteLoanProposalCommandHandler` |
| 8 | Domain Events (success) | 3 | `LoanProposalCreatedEvent`, `LoanProposalUpdatedEvent`, `LoanProposalDeletedEvent` |
| 9 | Domain Events (failure) | 1 | `LoanProposalFailedEvent` |

Every item declared in this matrix MUST have its complete schema in the DDD-REQs below.

---

### DDD-REQ-001 — AR Behavior Map: Events Originated & Specifications Used

> **Rule:** Every `addEvent(...)` call MUST live inside the AR behavior method listed in the
> "AR Method" column. Handlers MUST NEVER call `addEvent(...)` — they only call
> `messagingProcessor.publish(aggregate.getEvents())` after persistence.

| # | Behavior | AR Method | Event Originated (inside method) | Specifications Used | Source EARS |
|---|----------|-----------|----------------------------------|---------------------|-------------|
| 1 | Create | `.create(creationData)` | `LoanProposalCreatedEvent` | All 21 specs — strict AND chain | `LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation"` |
| 2 | Update | `.update(updateData)` | `LoanProposalUpdatedEvent` | All 21 specs — strict AND chain | `LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Update"` |
| 3 | Delete | `.delete(deletionData)` | `LoanProposalDeletedEvent` | None — HANDLER_GUARD only; no ValidationContext constructed | `LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Deletion"` |

---
### DDD-REQ-002 — Aggregate Root: LoanProposal

The Loan Proposal system shall implement `LoanProposal` as the aggregate root,
extending `AggregateRoot<String>` from `com.bits.ddd.domain.aggregate`. The aggregate
shall be annotated with `@Document(collection = "loan_proposal")`. Its identity shall be a
globally unique distributed (snowflake) string identifier generated at creation time.

**Field schema:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Distributed snowflake identifier. MongoDB `_id`. Not null, not updatable. |
| `loanProposalId` | `Long` | Distributed numeric proposal ID. Generated. |
| `proposalNumber` | `String` | Human-readable proposal number: `{YYYY}{MM}-{seq:5}` e.g. `"202406-00123"`. Generated. |
| `proposalReferenceNumber` | `String` | External reference number. Optional. |
| `branchId` | `Long` | Owning branch identifier. Required. |
| `branchCode` | `String` | Branch code. Required. |
| `projectId` | `Long` | Project identifier. Required. |
| `projectCode` | `String` | Project code. Required. |
| `villageOrganisationId` | `Long` | Village-organisation identifier. Required for group projects. |
| `villageOrganisationCode` | `String` | Village-organisation code. Required for group projects. |
| `memberId` | `Long` | Borrowing member identifier. Required. |
| `memberClassificationId` | `Long` | Member classification identifier. Required. |
| `loanProductId` | `Long` | Loan product identifier. Required. |
| `loanProductDetailsId` | `Long` | Loan product details identifier. Required. |
| `loanProductPolicyId` | `Long` | Loan product policy identifier. Required. |
| `schemeId` | `Long` | Scheme identifier. Required. |
| `sectorId` | `Long` | Sector identifier. Required (≥1). |
| `subSectorId` | `Long` | Sub-sector identifier. Optional (≥0). |
| `frequencyId` | `Long` | Repayment frequency identifier. Required. |
| `proposedLoanAmount` | `BigDecimal` | Requested loan amount. Required, not negative. |
| `approvedLoanAmount` | `BigDecimal` | Approved loan amount. Initialised to proposed amount on create. |
| `proposedGrantAmount` | `BigDecimal` | Proposed grant amount. Not negative. Must match policy grant setup when non-zero. |
| `approvedGrantAmount` | `BigDecimal` | Approved grant amount. Not negative. |
| `preProposedLoanAmount` | `BigDecimal` | Pre-proposal loan amount. Optional, not negative. |
| `interestRate` | `BigDecimal` | Loan interest rate. Must match product interest-rate record. |
| `numberOfInstallments` | `Integer` | Installment count. Must match product details. |
| `approvedNumberOfInstallments` | `Integer` | Approved installment count. |
| `installmentAmount` | `BigDecimal` | Per-installment amount. Must match recalculated amount. Greater than zero. |
| `approvedInstallmentAmount` | `BigDecimal` | Approved per-installment amount. Set to recalculated value. |
| `proposalDurationInMonths` | `Integer` | Loan tenure in months. Must match product details. ≥1. |
| `approvedDurationInMonths` | `Integer` | Approved duration in months. |
| `loanProposalStatus` | `LoanProposalStatus` | Business lifecycle status. Set to `PENDING` on creation. |
| `loanProposalType` | `LoanProposalType` | Proposal type (NORMAL_LOAN, RF, RS, GOOD_LOAN, UPG). Defaults to `NORMAL_LOAN`. |
| `approvalFlowStatus` | `String` | Approval workflow state. |
| `approvalStatus` | `String` | Approval status. |
| `dataSource` | `ApiDataSource` | Originating channel. Set to `OTC` on creation. |
| `domainStatus` | `DomainStatus` | bits.ddd lifecycle status. `ACTIVE` on creation, `INACTIVE` on delete. |
| `microInsurance` | `Boolean` | Micro-insurance requested flag. |
| `policyTypeId` | `Long` | Policy type (SINGLE=1, DOUBLE=2). |
| `insuranceProductId` | `Long` | Insurance product identifier. |
| `premiumAmount` | `BigDecimal` | Insurance premium amount. |
| `secondInsurer` | `SecondInsurer` | Embedded second-insurer entity. Required for double policy with micro-insurance. |
| `wantsFireInsurance` | `Boolean` | Fire-insurance election flag. |
| `fireInsuranceProductId` | `Long` | Fire-insurance product identifier. |
| `fireInsuranceDetails` | `FireInsuranceDetails` | Embedded fire-insurance value object. |
| `modeOfPayment` | `OtcModeOfPayment` | Embedded disbursement/collection method value object. |
| `autoDebitCollection` | `AutoDebitCollection` | Embedded automated collection value object. |
| `isDigitalDisbursement` | `Boolean` | Digital-disbursement flag. Derived from mode of payment. |
| `transactionDescription` | `String` | Digital-disbursement customer reference. Derived. |
| `nominees` | `List<Nominee>` | List of beneficiary nominees. Shares total 100%. |
| `guardian` | `Guardian` | Embedded guardian entity. Linked to first nominee. |
| `coBorrower` | `CoBorrower` | Embedded co-borrower entity. Conditional on product. |
| `guarantors` | `List<Guarantor>` | Embedded guarantor list. Attached from member record. |
| `specialSavingsAccountIds` | `List<String>` | Linked special-savings account IDs. Lien/money-plant only. |
| `specialSavingsAccountNumbers` | `List<String>` | Linked special-savings account numbers. |
| `countryId` | `Long` | Migration country identifier. Required for migration loans. |
| `loanApproverId` | `Long` | Approving role identifier. Role-gated by amount/project. |
| `totalPovertyScore` | `BigDecimal` | Total poverty score. Not negative. |
| `fieldOfficerId` | `Long` | Assigned field officer (PO) identifier. ≥0. |
| `loanSecurityAmount` | `BigDecimal` | Loan security amount. Defaults to 0. |
| `loanSecurityBalance` | `BigDecimal` | Loan security balance. Defaults to 0. |
| `spousePrimaryIncomeSource` | `String` | Spouse primary income source. Max 100 chars. |
| `spouseSecondaryIncomeSource` | `String` | Spouse secondary income source. Max 100 chars. |
| `firstChildName` | `String` | First child name. Max 100 chars. |
| `secondChildName` | `String` | Second child name. Max 100 chars. |
| `largeGroupLeaderName` | `String` | Large-group-leader name. Max 100 chars. |
| `largeGroupLeaderImage` | `String` | Large-group-leader image reference. Max 100 chars. |
| `assetBufferId` | `String` | Linked asset-buffer record identifier. |
| `applicationDate` | `LocalDate` | Loan application date. |
| `disbursementDate` | `LocalDate` | Disbursement date. |
| `voDisbursementDate` | `LocalDate` | Village-organisation disbursement date. |
| `firstRepaymentDate` | `LocalDate` | First repayment date. |
| `approvalCode` | `String` | Approval code. |
| `transactionNumber` | `String` | Transaction number. |
| `scannedFileName` | `String` | Scanned document file name. |
| `flag` | `Integer` | Internal processing flag. |
| `cohortMappingId` | `Long` | Cohort mapping identifier. |
| `assetPurchaseId` | `Long` | Asset-purchase record identifier. |
| `disbursementSubStatus` | `Integer` | Disbursement sub-status (coded). |
| `longitude` | `String` | Capture-location longitude. |
| `latitude` | `String` | Capture-location latitude. |
| `reasonForLoan` | `String` | Stated reason for the loan. |
| `numberOfChildGoToSchool` | `Integer` | Number of children going to school. |
| `noOfPreviousLoanFromBrac` | `Integer` | Prior BRAC loan count. |
| `rcaEnabled` | `Boolean` | Risk/credit-assessment enabled flag. |
| `memberMobileNumber` | `String` | Member mobile number. |
| `address` | `String` | Address. |
| `contactNo` | `String` | Contact number. |
| `voLeaderId` | `Long` | Village-organisation leader identifier. |
| `voLeaderName` | `String` | Village-organisation leader name. |
| `spouseContactNumber` | `String` | Spouse contact number. |
| `earner` | `Integer` | Number of earners in household. |
| `ownIncome` | `BigDecimal` | Member's own income. |
| `loanUser` | `Integer` | Loan user/utiliser (coded). |
| `ageType` | `Integer` | Age-type classification (coded). |
| `isNewInsurer` | `Boolean` | Newly registered second-insurer flag. |
| `loanRecommenderId` | `Long` | Loan recommender identifier. |
| `disbursementRetryCount` | `Integer` | Digital-disbursement retry count. Default 0. |
| `disbursedBy` | `String` | Actor who disbursed. |
| `bmNotVerifiedDisbursementReason` | `String` | Reason for disbursing without BM verification. |
| `loanAccountFound` | `Boolean` | Loan-account-found flag (set on disbursement callback). |
| `digitalDisbursementStatusId` | `Integer` | Digital-disbursement status (coded). |
| `digitalDisbursementHoApprovalDate` | `LocalDateTime` | Head-office approval date. |
| `digitalDisbursementSignConsent` | `Boolean` | Sign-consent flag. |
| `digitalDisbursementBankInstructionDate` | `LocalDateTime` | Bank-instruction date. |
| `digitalDisbursementHoApprovalBy` | `String` | Head-office approver. |
| `loanReferrerName` | `String` | Loan referrer name. |
| `loanReferrerContactNo` | `String` | Loan referrer contact number. |
| `voToSpotDistanceInstruction` | `String` | VO-to-spot distance instruction. |
| `rejectionReason` | `String` | Rejection reason. |
| `signConsent` | `Boolean` | Consent capture flag. |
| `consentUrl` | `String` | Consent URL. |
| `progotiDocumentChecklist` | `ProgotiDocumentChecklist` | Embedded Progoti document checklist. Progoti/ADP only. |
| `loanAccountId` | `Long` | Post-disbursement linked loan-account identifier. |
| `disbursedAmount` | `BigDecimal` | Post-disbursement disbursed amount. |
| `approvalLogId` | `Long` | Approval-log identifier. |
| `changeLogId` | `Long` | Change-log identifier. |

**Index candidates:**

| Index name | Fields | Type | Query / guard this supports |
|-----------|--------|------|-----------------------------|
| `_id` | `_id` | Default (auto) | Aggregate load by ID (`domainRepository.findById`) |
| `idx_proposal_number_branch` | `proposalNumber ASC, branchId ASC` | Unique sparse | Business-key uniqueness guard; human lookup by proposal number |
| `idx_member_status` | `memberId ASC, loanProposalStatus ASC` | Compound | Duplicate-pending-proposal guard on create/update |
| `idx_branch_data_source` | `branchId ASC, dataSource ASC` | Compound | Branch-scoped OTC proposal retrieval |

**Behaviour — create(creationData):** [PSEUDOCODE]

> Source rule: "When a new loan proposal is submitted, the Loan Proposal system shall run the full structural, digital-disbursement, and domain validation chain before persisting the proposal." (§ "Proposal Creation")
> → ALL 21 category specifications run. Structural/field validation happens at the presentation layer (DTO `@Valid`); the spec chain below is the domain-invariant layer.

```pseudocode
create(creationData):
  // ── Pre-population guards ────────────────────────────────────────────────
  IF creationData.id is null THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(creationData.traceId,
        Map.of("loanProposal", LocalizedMessage(key = "id.must.not.be.null"))))
    // message: "The given id must not be null!"
  END IF

  // ── Field population ─────────────────────────────────────────────────────
  id                        = creationData.id  // client-supplied distributed snowflake ID
  loanProposalId            = creationData.loanProposalId
  proposalNumber            = generateProposalNumber(creationData.businessDate, creationData.sequence)
  branchId                  = creationData.branchId
  branchCode                = creationData.branchCode
  projectId                 = creationData.projectId
  projectCode               = creationData.projectCode
  villageOrganisationId     = creationData.villageOrganisationId
  villageOrganisationCode   = creationData.villageOrganisationCode
  memberId                  = creationData.memberId
  memberClassificationId    = creationData.memberClassificationId
  loanProductId             = creationData.loanProductId
  loanProductDetailsId      = creationData.loanProductDetailsId
  loanProductPolicyId       = creationData.loanProductPolicyId
  schemeId                  = creationData.schemeId
  sectorId                  = creationData.sectorId
  subSectorId               = creationData.subSectorId
  frequencyId               = creationData.frequencyId
  proposedLoanAmount        = creationData.proposedLoanAmount
  approvedLoanAmount        = creationData.proposedLoanAmount  // initialised to proposed
  proposedGrantAmount       = creationData.proposedGrantAmount
  approvedGrantAmount       = creationData.approvedGrantAmount
  preProposedLoanAmount     = creationData.preProposedLoanAmount
  interestRate              = creationData.interestRate
  numberOfInstallments      = creationData.numberOfInstallments
  approvedNumberOfInstallments = creationData.numberOfInstallments
  installmentAmount         = creationData.installmentAmount
  approvedInstallmentAmount = creationData.recalculatedInstallmentAmount
  proposalDurationInMonths  = creationData.proposalDurationInMonths
  approvedDurationInMonths  = creationData.proposalDurationInMonths
  loanProposalStatus        = LoanProposalStatus.PENDING
  loanProposalType          = creationData.loanProposalType ?? LoanProposalType.NORMAL_LOAN
  dataSource                = ApiDataSource.OTC
  microInsurance            = creationData.microInsurance
  policyTypeId              = creationData.policyTypeId
  insuranceProductId        = creationData.insuranceProductId
  premiumAmount             = creationData.premiumAmount
  secondInsurer             = creationData.secondInsurer
  wantsFireInsurance        = creationData.wantsFireInsurance
  fireInsuranceProductId    = creationData.fireInsuranceProductId
  fireInsuranceDetails      = defaultFireInsuranceDetails(creationData)
    // default insured amount → proposedLoanAmount if absent
    // default duration → max(proposalDurationInMonths, 12) if absent
  modeOfPayment             = creationData.modeOfPayment
  autoDebitCollection       = creationData.autoDebitCollection
  isDigitalDisbursement     = derivedDigitalDisbursementFlag(creationData.modeOfPayment)
  transactionDescription    = deriveCustomerReference(creationData.modeOfPayment, branchCode, voCode, memberId)
  nominees                  = assignNomineeIds(creationData.nominees)  // assign IDs + share percentages
  guardian                  = linkGuardianToFirstNominee(creationData.guardian, nominees)
  coBorrower                = assignCoBorrowerId(creationData.coBorrower)
  guarantors                = creationData.guarantors  // attached from member record
  specialSavingsAccountIds  = creationData.specialSavingsAccountIds
  specialSavingsAccountNumbers = creationData.specialSavingsAccountNumbers
  countryId                 = creationData.countryId
  loanApproverId            = creationData.loanApproverId
  totalPovertyScore         = creationData.totalPovertyScore
  fieldOfficerId            = creationData.fieldOfficerId
  loanSecurityAmount        = creationData.loanSecurityAmount ?? 0
  loanSecurityBalance       = creationData.loanSecurityBalance ?? 0
  // … populate all remaining fields from creationData

  // ── Validation context (all 12 SOURCE_DATA entities loaded for CREATE) ───
  context = new LoanProposalValidationContext(
    member               = creationData.member,             // specs 1, 2, 4, 8, 10, 11, 12, 13, 15, 18, 19
    loanProduct          = creationData.loanProduct,        // specs 3, 4, 5, 6, 7, 8, 10, 11, 12, 15, 16, 18, 19, 20
    loanProductDetails   = creationData.loanProductDetails, // specs 3, 5, 13
    loanProductPolicy    = creationData.loanProductPolicy,  // specs 3, 5, 6
    scheme               = creationData.scheme,             // specs 3, 20
    project              = creationData.project,            // specs 2, 3, 11, 12
    projectPolicy        = creationData.projectPolicy,      // specs 2, 6, 11
    branch               = creationData.branch,             // specs 2, 15, 17
    villageOrganisation  = creationData.villageOrganisation,// specs 2
    insuranceProduct     = creationData.insuranceProduct,   // specs 8, 17
    country              = creationData.country,            // spec 16
    bank                 = creationData.bank,               // specs 15, 21
    aggregate            = this)

  // ── Full 21-specification AND chain (CREATE) ─────────────────────────────
  compositeSpec =
    new MemberEligibilitySpecification<>()                    // 1: member exists, active, not blacklisted, has identity, no pending loan
        .and(new BranchProjectVoConsistencySpecification<>()) // 2: project found, member branch/project/VO match, active mapping
        .and(new LoanProductPolicySpecification<>())          // 3: product/details/policy found and active on business date, mapped to project/office/category/frequency/member-classification
        .and(new RepaymentFrequencyModeOfPaymentSpecification<>()) // 4: frequency valid, mode-of-payment sub-type valid, Rocket wallet present
        .and(new LoanAmountGrantInstallmentSpecification<>()) // 5: amount in policy range, grant matches, interest defined and valid, installment/duration/rate match product details
        .and(new LoanExposureLimitSpecification<>())          // 6: proposed amount within office-and-project exposure limit
        .and(new CoBorrowerSpecification<>())                 // 7: co-borrower supplied only if product requires it
        .and(new InsurancePolicyTypeSecondInsurerSpecification<>()) // 8: second-insurer rules, policy type validity, insurance product mapping
        .and(new NomineeSpecification<>())                    // 9: nominee count within DCS limit
        .and(new SpecialSavingsLienSpecification<>())         // 10: special-savings account belongs to member, lien parallel limit, approver role for lien
        .and(new ProjectSpecificRulesSpecification<>())       // 11: Progoti/Goti/Shondhi/General parallel rules, approver role, top-up window
        .and(new ParallelCoExistingLoanSpecification<>())     // 12: Remittance/General parallel, product parallel-loan flag, UPG first-loan rule
        .and(new InstallmentConfigurationSpecification<>())   // 13: installment config found, loan product details present, spouse-insurer loan engagement
        .and(new ModeOfPaymentRocketWalletSpecification<>())  // 14: Rocket wallet number matches member for disbursement and collection
        .and(new DigitalDisbursementSpecification<>())        // 15: money-plant + central disbursement exclusion, mode of payment present, auto-debit mapping, premium collection, bank account ownership
        .and(new MigrationCountrySpecification<>())           // 16: country selected and configured for migration loans
        .and(new FireInsuranceSpecification<>())              // 17: fire-insurance product, premium, duration, insured amount, sector, branch/project mapping, phone, nominee limits
        .and(new AgeLimitSpecification<>())                   // 18: member age eligibility, micro-insurance age rules, second-insurer age rules
        .and(new MoneyPlantSpecification<>())                 // 19: money-plant age, savings account validity, amount match
        .and(new SchemeSectorMappingSpecification<>())        // 20: scheme and sector mapped to loan product
        .and(new BankModeOfPaymentSpecification<>())          // 21: bank account, balance, BEFTN/fund-transfer rules, cheque/online reference

  errors = compositeSpec.validate(context)
  IF errors is not empty THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(creationData.traceId, errors))
  END IF

  this.status = DomainStatus.CREATED
  addEvent(LoanProposalEventMapper.toCreatedEvent(this))
  RETURN this
```

**Behaviour — update(updateData):** [PSEUDOCODE]

> Source rule: "When an operator updates a loan proposal, the Loan Proposal system shall run the full domain validation chain including migration-country and fire-insurance validation before persisting." (§ "Proposal Update")
> → ALL 21 category specifications run (same chain as create). Status guard runs BEFORE the spec chain.

```pseudocode
update(updateData):
  // ── Status guard (runs BEFORE spec chain) ───────────────────────────────
  IF this.loanProposalStatus != LoanProposalStatus.PENDING THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(updateData.traceId,
        Map.of("loanProposal", LocalizedMessage(key = "approved.modification.validation"))))
    // message: "Approved loan proposal cannot be modified"
  END IF

  // ── Field merge (apply non-null fields from updateData) ──────────────────
  loanProductId             = updateData.loanProductId ?? this.loanProductId
  loanProductDetailsId      = updateData.loanProductDetailsId ?? this.loanProductDetailsId
  schemeId                  = updateData.schemeId ?? this.schemeId
  sectorId                  = updateData.sectorId ?? this.sectorId
  subSectorId               = updateData.subSectorId ?? this.subSectorId
  policyTypeId              = updateData.policyTypeId ?? this.policyTypeId
  insuranceProductId        = updateData.insuranceProductId ?? this.insuranceProductId
  frequencyId               = updateData.frequencyId ?? this.frequencyId
  installmentAmount         = updateData.installmentAmount
  approvedInstallmentAmount = updateData.recalculatedInstallmentAmount
  interestRate              = updateData.interestRate ?? this.interestRate
  proposalDurationInMonths  = updateData.proposalDurationInMonths ?? this.proposalDurationInMonths
  proposedLoanAmount        = updateData.proposedLoanAmount ?? this.proposedLoanAmount
  approvedLoanAmount        = updateData.proposedLoanAmount ?? this.approvedLoanAmount
  proposedGrantAmount       = updateData.proposedGrantAmount ?? this.proposedGrantAmount
  approvedGrantAmount       = updateData.approvedGrantAmount ?? this.approvedGrantAmount
  nominees                  = updateData.nominees  // equal-split share recalculation
  guardian                  = updateData.guardian ?? this.guardian
  coBorrower                = updateData.coBorrower ?? this.coBorrower
  secondInsurer             = updateData.secondInsurer ?? this.secondInsurer
  modeOfPayment             = updateData.modeOfPayment ?? this.modeOfPayment
  autoDebitCollection       = updateData.autoDebitCollection ?? this.autoDebitCollection
  countryId                 = updateData.countryId ?? this.countryId
  fireInsuranceDetails      = refreshFireInsuranceDetails(updateData, this)
  isDigitalDisbursement     = derivedDigitalDisbursementFlag(modeOfPayment)
  transactionDescription    = deriveCustomerReference(modeOfPayment, branchCode, voCode, memberId)
  guarantors                = updateData.guarantors  // refreshed from member record
  // … merge all other supplied fields

  // ── Record "before" snapshot in change log (OTC general-edit source) ─────
  // [INFERRED — delegated to persistence layer via domain event metadata]

  // ── Validation context (conditional SOURCE_DATA on UPDATE) ──────────────
  // member is ALWAYS re-fetched; other entities fetched only when their IDs are non-null in updateData
  context = new LoanProposalValidationContext(
    member               = updateData.member,             // always present; specs 1, 2, 4, 8, 10, 11, 12, 13, 15, 18, 19
    loanProduct          = updateData.loanProduct,        // present when loanProductId non-null; specs 3, 4, 5, 6, 7, 8, 10, 11, 12, 15, 16, 18, 19, 20
    loanProductDetails   = updateData.loanProductDetails, // present when loanProductDetailsId non-null; specs 3, 5, 13
    loanProductPolicy    = updateData.loanProductPolicy,  // present when loanProductPolicyId non-null; specs 3, 5, 6
    scheme               = updateData.scheme,             // present when schemeId non-null; specs 3, 20
    project              = updateData.project,            // present when projectId non-null; specs 2, 3, 11, 12
    projectPolicy        = updateData.projectPolicy,      // present when projectId non-null; specs 2, 6, 11
    branch               = updateData.branch,             // always present; specs 2, 15, 17
    villageOrganisation  = updateData.villageOrganisation,// present when voId non-null; spec 2
    insuranceProduct     = updateData.insuranceProduct,   // present when insuranceProductId non-null; specs 8, 17
    country              = updateData.country,            // present when countryId non-null; spec 16
    bank                 = updateData.bank,               // present when bankId non-null; specs 15, 21
    aggregate            = this)

  // ── Full 21-specification AND chain (UPDATE — identical to CREATE) ────────
  compositeSpec =
    new MemberEligibilitySpecification<>()                    // 1
        .and(new BranchProjectVoConsistencySpecification<>()) // 2
        .and(new LoanProductPolicySpecification<>())          // 3
        .and(new RepaymentFrequencyModeOfPaymentSpecification<>()) // 4
        .and(new LoanAmountGrantInstallmentSpecification<>()) // 5
        .and(new LoanExposureLimitSpecification<>())          // 6
        .and(new CoBorrowerSpecification<>())                 // 7
        .and(new InsurancePolicyTypeSecondInsurerSpecification<>()) // 8
        .and(new NomineeSpecification<>())                    // 9
        .and(new SpecialSavingsLienSpecification<>())         // 10
        .and(new ProjectSpecificRulesSpecification<>())       // 11
        .and(new ParallelCoExistingLoanSpecification<>())     // 12
        .and(new InstallmentConfigurationSpecification<>())   // 13
        .and(new ModeOfPaymentRocketWalletSpecification<>())  // 14
        .and(new DigitalDisbursementSpecification<>())        // 15
        .and(new MigrationCountrySpecification<>())           // 16
        .and(new FireInsuranceSpecification<>())              // 17
        .and(new AgeLimitSpecification<>())                   // 18
        .and(new MoneyPlantSpecification<>())                 // 19
        .and(new SchemeSectorMappingSpecification<>())        // 20
        .and(new BankModeOfPaymentSpecification<>())          // 21

  errors = compositeSpec.validate(context)
  IF errors is not empty THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(updateData.traceId, errors))
  END IF

  this.status = DomainStatus.UPDATED
  addEvent(LoanProposalEventMapper.toUpdatedEvent(this))
```

**Behaviour — delete(deletionData):** [PSEUDOCODE] (soft delete)

> Source rule: "While a loan proposal is in Pending status, the Loan Proposal system shall, on a delete request, soft-delete the proposal." and "If a delete request targets a proposal that is not in Pending status, the Loan Proposal system shall reject the request." (§ "Proposal Deletion")
> → `delete` has **no domain specification chain**. The only check is the built-in status guard. No `ValidationContext` is constructed, no source data is fetched, and no specification is invoked.

```pseudocode
delete(deletionData):
  // ── Status guard — the ONLY validation for delete ────────────────────────
  // No domain specifications run. No source data fetched. No context constructed.
  IF this.loanProposalStatus != LoanProposalStatus.PENDING THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(deletionData.traceId,
        Map.of("loanProposal", LocalizedMessage(
          key   = MessageKey.DELETE_FAILED.getKey(),
          args  = [this.loanProposalStatus.name()]))))
    // message: "Only pending loan proposal can be deleted. Loan proposal Status: {status}"
  END IF

  this.deleted          = true
  this.deletionAudit    = new DeletionAudit(deletionData.deletedBy, now())
  this.domainStatus     = DomainStatus.INACTIVE
  this.loanProposalStatus = LoanProposalStatus.PENDING  // remains PENDING, domain status set INACTIVE
  addEvent(LoanProposalEventMapper.toDeletedEvent(this))
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation", "Proposal Update", "Proposal Deletion"

---
### DDD-REQ-003 — Value Object: FireInsuranceDetails

The Loan Proposal system shall model `FireInsuranceDetails` as an immutable Java `record` in
`domain/value/`. It captures all fire-insurance fields embedded within the proposal.

| Field | Type | Description |
|-------|------|-------------|
| `businessName` | `String` | Insured business name. Required for DCS fire insurance. |
| `businessAddress` | `String` | Insured business address. Required for DCS fire insurance. |
| `businessPhone` | `String` | Business phone number. Required; exactly 11 digits. |
| `businessEmail` | `String` | Business email. Optional. |
| `divisionId` | `Long` | Division of the insured business. Required for DCS fire insurance. |
| `districtId` | `Long` | District of the insured business. Required for DCS fire insurance. |
| `thanaId` | `Long` | Thana of the insured business. Required for DCS fire insurance. |
| `businessTypeId` | `Long` | Business type / risk classification. Required for DCS fire insurance. |
| `constructionOfPremisesId` | `Long` | Construction-of-premises type. Required for DCS fire insurance. |
| `fireInsurancePremiumAmount` | `BigDecimal` | Fire-insurance premium. Must match calculated premium. |
| `fireInsuranceInsuredAmount` | `BigDecimal` | Insured value. Defaults to proposed loan amount; must match it. |
| `durationOfFireInsurance` | `Integer` | Coverage months. Defaults to `max(proposalDurationInMonths, 12)`; must match calculated. |
| `fireInsuranceProductName` | `String` | Product name. Enriched on retrieval (read-side). |
| `bracCommissionAmount` | `BigDecimal` | BRAC commission amount. Default zero. |
| `memberCommissionAmount` | `BigDecimal` | Member commission amount. Default zero. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Fire Insurance Details", "Fire Insurance Validation"

---

### DDD-REQ-004 — Value Object: OtcModeOfPayment

The Loan Proposal system shall model `OtcModeOfPayment` as an immutable Java `record` in
`domain/value/`. It captures the disbursement and collection method for the proposal.

| Field | Type | Description |
|-------|------|-------------|
| `modeOfPaymentId` | `Long` | Mode of payment identifier. Must be valid. |
| `subType` | `ModeOfPaymentSubType` | Payment sub-type: Cheque/TT/BEFTN/Rocket/Bkash/Online/CashDeposit/RTGS/FundTransfer/SCB/HSBC. |
| `bankAccountNumber` | `String` | Bank account number. Required for non-digital bank payments. |
| `bankRoutingNumber` | `String` | Bank routing number. |
| `bankId` | `Long` | Bank identifier. ≥1. |
| `bankBranchId` | `Long` | Bank branch identifier. ≥1. |
| `paymentSubTypeNumber` | `String` | Cheque/TT/transaction reference number. Required for cheque/online/cash-deposit. |
| `paymentSubTypeDate` | `LocalDate` | Cheque/TT/transaction date. Required for cheque/online/cash-deposit. |
| `bkashWalletNumber` | `String` | Bkash wallet number. |
| `rocketWalletNumber` | `String` | Rocket wallet number. Must match member's Rocket wallet number. |
| `premiumModeOfPaymentId` | `Long` | Premium collection method identifier. |
| `digitalDisbursementModeId` | `Long` | Digital-disbursement type identifier. DCS context. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "OTC Mode of Payment", "Repayment Frequency and Mode of Payment", "Bank Mode-of-Payment Validation"

---

### DDD-REQ-005 — Value Object: AutoDebitCollection

The Loan Proposal system shall model `AutoDebitCollection` as an immutable Java `record` in
`domain/value/`. It captures the automated collection arrangement for the loan.

| Field | Type | Description |
|-------|------|-------------|
| `subType` | `AutoDebitCollectionSubType` | Collection sub-type: Rocket / DDI / etc. Must be mapped in branch and project. |
| `memberBankManagementLinkId` | `Long` | Auto-debit bank setup link. Must be mapped in branch and project. |
| `chequeNumbers` | `List<String>` | Direct-debit cheque instrument numbers. |
| `micrNumbers` | `List<String>` | MICR numbers. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Auto-Debit Collection"

---

### DDD-REQ-006 — Value Object: ProgotiDocumentChecklist

The Loan Proposal system shall model `ProgotiDocumentChecklist` as an immutable Java `record`
in `domain/value/`. It captures the property-loan (Progoti) document verification checklist.
All boolean fields are required for Progoti/ADP proposals on the DCS channel.

| Field | Type | Description |
|-------|------|-------------|
| `commitmentLetter` | `Boolean` | Commitment letter verified. |
| `collateralBond` | `Boolean` | Collateral bond verified. |
| `bankStatement` | `Boolean` | Bank statement verified. |
| `securityCheck` | `Boolean` | Security check verified. |
| `originalDeed` | `Boolean` | Original deed verified. |
| `bayaDeed` | `Boolean` | Baya deed verified. |
| `pittDeed` | `Boolean` | Pitt deed verified. |
| `positionDeed` | `Boolean` | Position deed verified. |
| `duplicateDocumentWithWithdrawalReceipt` | `Boolean` | Duplicate document with withdrawal receipt verified. |
| `dcr` | `Boolean` | DCR verified. |
| `dismissalForm` | `Boolean` | Dismissal form verified. |
| `saOriginalPapers` | `Boolean` | SA original papers verified. |
| `rsOriginalPapers` | `Boolean` | RS original papers verified. |
| `taxReceipt` | `Boolean` | Tax receipt verified. |
| `heirCertificate` | `Boolean` | Heir certificate verified. |
| `stopRentOrAdvanceAgreement` | `Boolean` | Stop-rent or advance agreement verified. |
| `seizedPropertyInvestigativeReport` | `Boolean` | Seized-property investigative report verified. |
| `other` | `String` | Other documents description. Optional. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Progoti Document Checklist", "Project-Specific Rules (Progoti / Goti, Branch Recommender)"

---

### DDD-REQ-007 — Status Lifecycle: LoanProposal

The Loan Proposal system shall track the proposal lifecycle through the following states using
`DomainStatus` from `com.bits.ddd.shared.domain.enums` for technical bits.ddd transitions, and
a local `LoanProposalStatus` enum in `domain/enums/` for business states.

**DomainStatus transitions (bits.ddd technical layer):**

| State | Trigger | Previous States |
|-------|---------|-----------------|
| `CREATED` | `LoanProposal.create(...)` | — (initial) |
| `UPDATED` | `LoanProposal.update(...)` | CREATED, UPDATED |
| `INACTIVE` | `LoanProposal.delete(...)` | CREATED, UPDATED |

**LoanProposalStatus (business lifecycle — local enum):**

| Status | Business Meaning |
|--------|-----------------|
| `PENDING` | Newly captured proposal awaiting decision. Set on create; retained on update. |
| `APPROVED` | Proposal approved (managed by approval service, not OTC channel). |
| `REJECTED` | Proposal rejected. |
| `DISBURSED` | Loan disbursed. |
| `DRAFT` | Draft proposal (excluded from OTC standard list). |
| `BM_APPROVAL_PENDING` | Awaiting branch-manager approval (Smart-PO; excluded from OTC list). |
| `BM_REJECT` | Rejected by branch manager (excluded from OTC list). |
| `AWAITING_DISBURSE` | Approved and queued for disbursement. |
| `FAILED_TO_DISBURSE` | Disbursement attempt failed. |
| `CLOSED` | Proposal closed (e.g. ERP undo-disbursement from `DISBURSED`). |
| `PENDING_DIGITAL_DISBURSEMENT` | Awaiting digital disbursement. |
| `QUEUED_FOR_DIGITAL_DISBURSEMENT` | Queued for digital disbursement. |
| `FAILED_TO_DIGITAL_DISBURSEMENT` | Digital disbursement failed. |
| `DCS_SENT_BACK` | Sent back on the DCS channel. |
| `OTC_SENT_BACK` | Sent back on the OTC channel. |
| `ASSET_PURCHASED` | Asset purchased for the loan. |
| `BM_SENT_BACK` | Sent back by branch manager. |
| `VALIDATION_FAILED` | Validation failed during processing. |
| `SERVER_ERROR` | Server error during processing. |
| `FAILED_TO_QUEUE` | Failed to enqueue for synchronisation. |

**OTC channel state transitions:**

| From State | To State | Triggered by |
|------------|----------|-------------|
| (new) | `PENDING` + `DomainStatus.CREATED` | `LoanProposal.create(...)` |
| `PENDING` | `PENDING` + `DomainStatus.UPDATED` | `LoanProposal.update(...)` (only while PENDING) |
| `PENDING` | soft-deleted + `DomainStatus.INACTIVE` | `LoanProposal.delete(...)` (only while PENDING) |
| `DISBURSED` | `CLOSED` | ERP undo-disbursement (managed externally; CLOSED state used by read-side enrichment) |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Domain Concepts and States — Loan Proposal"

---

### DDD-REQ-008 — Validation Context: LoanProposalValidationContext

The Loan Proposal system shall define `LoanProposalValidationContext` as a Java `record` in
`domain/specification/context/`. It shall implement the following context interfaces,
each declaring the subset of source-data fields its consuming specifications require:

- `MemberContext` — requires `member: Member`
- `LoanProductContext` — requires `loanProduct: LoanProduct`, `loanProductDetails: LoanProductDetails`, `loanProductPolicy: LoanProductPolicy`
- `BranchProjectContext` — requires `branch: Branch`, `project: Project`, `projectPolicy: ProjectPolicy`, `villageOrganisation: VillageOrganisation`
- `InsuranceContext` — requires `insuranceProduct: InsuranceProduct`
- `CountryContext` — requires `country: Country`
- `BankContext` — requires `bank: Bank`
- `SchemeContext` — requires `scheme: Scheme`

The context shall be instantiated inside the aggregate before running domain specifications on
every `create(...)` and `update(...)` call.

| Context field | Type | Specifications that consume it |
|--------------|------|-------------------------------|
| `member` | `Member` | 1, 2, 4, 8, 10, 11, 12, 13, 15, 18, 19 |
| `loanProduct` | `LoanProduct` | 3, 4, 5, 6, 7, 8, 10, 11, 12, 15, 16, 18, 19, 20 |
| `loanProductDetails` | `LoanProductDetails` | 3, 5, 13 |
| `loanProductPolicy` | `LoanProductPolicy` | 3, 5, 6 |
| `scheme` | `Scheme` | 3, 20 |
| `project` | `Project` | 2, 3, 11, 12 |
| `projectPolicy` | `ProjectPolicy` | 2, 6, 11 |
| `branch` | `Branch` | 2, 15, 17 |
| `villageOrganisation` | `VillageOrganisation` | 2 |
| `insuranceProduct` | `InsuranceProduct` | 8, 17 |
| `country` | `Country` | 16 |
| `bank` | `Bank` | 15, 21 |
| `aggregate` | `LoanProposal` (this) | all |

> 📎 [INFERRED] — required by bits.ddd pattern

---
### DDD-REQ-009 — Specification: MemberEligibilitySpecification

The Loan Proposal system shall implement `MemberEligibilitySpecification` in
`domain/specification/rules/`, implementing `Specification<ValidationContext>`. It bundles
the following sub-rules from the source spec's "Member Eligibility, Identity, and Status" section:

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Member not found in system | "Member Not Found" | `LoanProposalOTC-EARS-review-2-resolved.md § "Member Eligibility, Identity, and Status"` |
| Member is screened (blacklisted) | "Member is screened, Cannot Process Loan." | same |
| Member's classification record not found | "Member Classification not found." | same |
| Member's status is not active | (localized: cannot propose for member's current status) | same |
| Member has no identity information (NID/Smart ID/Birth Certificate/Passport) | "Member does not have any identity information [National ID/Smart ID/Birth Certificate/Passport]. Please update member profile and then try again." | same |
| Member already has a pending or approved loan proposal | "The member already has a loan proposal in pending/approved status." | same |
| Guarantor supplied without identity-card information | (localized: guarantor identity card cannot be null) | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (MemberContext) context
  IF ctx.member() is null THEN
    errors.put("member", LocalizedMessage(key = "MEMBER_NOT_FOUND"))
    RETURN errors  // cannot proceed without member
  END IF
  IF ctx.member().isScreened() THEN
    errors.put("member", LocalizedMessage(key = "MEMBER_SCREENED"))
  END IF
  IF ctx.member().classification() is null THEN
    errors.put("memberClassification", LocalizedMessage(key = "MEMBER_CLASSIFICATION_NOT_FOUND"))
  END IF
  IF ctx.member().status() != MemberStatus.ACTIVE THEN
    errors.put("member", LocalizedMessage(key = "MEMBER_STATUS_INVALID", args = [ctx.member().status().name()]))
  END IF
  IF ctx.member().hasNoIdentityDocuments() THEN
    errors.put("member", LocalizedMessage(key = "MEMBER_NO_IDENTITY"))
  END IF
  IF ctx.member().hasPendingOrApprovedLoanProposal() THEN
    errors.put("member", LocalizedMessage(key = "MEMBER_DUPLICATE_PROPOSAL"))
  END IF
  FOR EACH guarantor IN ctx.aggregate().getGuarantors() DO
    IF guarantor.identityCards() is null or empty THEN
      errors.put("guarantor", LocalizedMessage(key = "GUARANTOR_IDENTITY_NULL"))
    END IF
  END FOR
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Member Eligibility, Identity, and Status"

---

### DDD-REQ-010 — Specification: BranchProjectVoConsistencySpecification

The Loan Proposal system shall implement `BranchProjectVoConsistencySpecification` in
`domain/specification/rules/`. It bundles the following sub-rules from the source spec's
"Branch, Project, and Village Organisation Consistency" section:

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Project not found for supplied project code | "Project not found with code: {code}" | `§ "Branch, Project, and Village Organisation Consistency"` |
| Project policy not found | "Project Policy not found with id" | same |
| Member's branch differs from proposal branch | (localized: member branch and given branch do not match) | same |
| Member's project differs from proposal project | (localized: member project and given project do not match) | same |
| Group-based project but no VO code on proposal | "Vo Code not found for member." | same |
| Village organisation not found or has no code | "Vo Code not found for member." | same |
| Proposal VO code does not match member's VO code | (localized: member VO does not match loan proposal VO) | same |
| No active mapping between branch and project | "No active mapping found for branch and project." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (BranchProjectContext) context
  IF ctx.project() is null THEN
    errors.put("project", LocalizedMessage(key = "PROJECT_NOT_FOUND", args = [ctx.aggregate().getProjectCode()]))
    RETURN errors
  END IF
  IF ctx.projectPolicy() is null THEN
    errors.put("projectPolicy", LocalizedMessage(key = "PROJECT_POLICY_NOT_FOUND"))
  END IF
  IF ctx.member().branchId() != ctx.aggregate().getBranchId() THEN
    errors.put("branch", LocalizedMessage(key = "MEMBER_BRANCH_MISMATCH"))
  END IF
  IF ctx.member().projectId() != ctx.aggregate().getProjectId() THEN
    errors.put("project", LocalizedMessage(key = "MEMBER_PROJECT_MISMATCH"))
  END IF
  IF ctx.projectPolicy() != null AND ctx.projectPolicy().associationType() == GROUP
     AND ctx.aggregate().getVillageOrganisationCode() is null THEN
    errors.put("voCode", LocalizedMessage(key = "VO_CODE_NOT_FOUND"))
  END IF
  IF ctx.villageOrganisation() is null or ctx.villageOrganisation().code() is null THEN
    errors.put("voCode", LocalizedMessage(key = "VO_CODE_NOT_FOUND"))
  ELSE IF ctx.villageOrganisation().code() != ctx.aggregate().getVillageOrganisationCode() THEN
    errors.put("voCode", LocalizedMessage(key = "MEMBER_VO_MISMATCH"))
  END IF
  IF NOT activeMappingExistsBetweenBranchAndProject(ctx.branch(), ctx.project()) THEN
    errors.put("branchProject", LocalizedMessage(key = "NO_ACTIVE_BRANCH_PROJECT_MAPPING"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Branch, Project, and Village Organisation Consistency"

---

### DDD-REQ-011 — Specification: LoanProductPolicySpecification

The Loan Proposal system shall implement `LoanProductPolicySpecification` in
`domain/specification/rules/`. It bundles the sub-rules from the source spec's
"Loan Product, Loan Product Details, and Loan Product Policy" section:

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Loan product not found | "Loan Product Number is not found." | `§ "Loan Product, Loan Product Details, and Loan Product Policy"` |
| Loan product details not found | "Loan Product Details not found with product, frequency, proposal duration." | same |
| Loan product policy not found | "Loan Product Policy Number is not found." | same |
| Loan product not active on business date | "Loan product may be expired or inactivated" | same |
| Loan product details not active on business date | "Loan product details may be expired or removed" | same |
| Loan product policy not active on business date | "Loan product policy may be expired or removed" | same |
| Loan product not mapped with project, office, category, and frequency | "Loan Product is not mapped with project, office, category and frequency." | same |
| Loan product not mapped with member's classification | "Loan product is not mapped with member category." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  businessDate = ctx.aggregate().getApplicationDate()
  IF ctx.loanProduct() is null THEN
    errors.put("loanProduct", LocalizedMessage(key = "LOAN_PRODUCT_NOT_FOUND"))
    RETURN errors
  END IF
  IF ctx.loanProductDetails() is null THEN
    errors.put("loanProductDetails", LocalizedMessage(key = "LOAN_PRODUCT_DETAILS_NOT_FOUND"))
  END IF
  IF ctx.loanProductPolicy() is null THEN
    errors.put("loanProductPolicy", LocalizedMessage(key = "LOAN_PRODUCT_POLICY_NOT_FOUND"))
  END IF
  IF NOT ctx.loanProduct().isActiveOn(businessDate) THEN
    errors.put("loanProduct", LocalizedMessage(key = "LOAN_PRODUCT_EXPIRED"))
  END IF
  IF ctx.loanProductDetails() != null AND NOT ctx.loanProductDetails().isActiveOn(businessDate) THEN
    errors.put("loanProductDetails", LocalizedMessage(key = "LOAN_PRODUCT_DETAILS_EXPIRED"))
  END IF
  IF ctx.loanProductPolicy() != null AND NOT ctx.loanProductPolicy().isActiveOn(businessDate) THEN
    errors.put("loanProductPolicy", LocalizedMessage(key = "LOAN_PRODUCT_POLICY_EXPIRED"))
  END IF
  IF NOT ctx.loanProduct().isMappedWith(ctx.project(), ctx.aggregate().getOfficeId(),
                                        ctx.member().classificationId(), ctx.aggregate().getFrequencyId()) THEN
    errors.put("loanProduct", LocalizedMessage(key = "LOAN_PRODUCT_MAPPING_INVALID"))
  END IF
  IF NOT ctx.loanProduct().isMappedWithMemberCategory(ctx.member().classificationId()) THEN
    errors.put("loanProduct", LocalizedMessage(key = "LOAN_PRODUCT_MEMBER_CATEGORY_MISMATCH"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Loan Product, Loan Product Details, and Loan Product Policy"

---

### DDD-REQ-012 — Specification: RepaymentFrequencyModeOfPaymentSpecification

The Loan Proposal system shall implement `RepaymentFrequencyModeOfPaymentSpecification` in
`domain/specification/rules/`. Sub-rules from "Repayment Frequency and Mode of Payment":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Frequency ID invalid (>10 or not a recognised frequency) | "Loan frequency id not found." | `§ "Repayment Frequency and Mode of Payment"` |
| Mode-of-payment sub-type invalid | "Invalid mode of payment subtype." | same |
| Rocket wallet payment but member has no Rocket wallet number | "Member has no Rocket Wallet No. Please update member information." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (MemberContext) context
  freq = ctx.aggregate().getFrequencyId()
  IF freq > 10 OR NOT isRecognisedFrequency(freq) THEN
    errors.put("frequency", LocalizedMessage(key = "LOAN_FREQUENCY_NOT_FOUND"))
  END IF
  mop = ctx.aggregate().getModeOfPayment()
  IF mop != null AND NOT isValidModeOfPaymentSubType(mop.subType()) THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "INVALID_MODE_OF_PAYMENT_SUBTYPE"))
  END IF
  IF mop != null AND mop.subType() == ROCKET AND ctx.member().rocketWalletNumber() is null THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "MEMBER_NO_ROCKET_WALLET"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Repayment Frequency and Mode of Payment"

---

### DDD-REQ-013 — Specification: LoanAmountGrantInstallmentSpecification

The Loan Proposal system shall implement `LoanAmountGrantInstallmentSpecification` in
`domain/specification/rules/`. Sub-rules from "Loan Amount, Grant, Installment, Duration, and Interest Rate":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Proposed amount outside policy range | "Cannot process the following proposal : {proposalNo} - {amount} amount exceeds the loan product policy range." | `§ "Loan Amount, Grant, Installment, Duration, and Interest Rate"` |
| Grant amount differs from computed grant (when computed > 0) | "Your provide grant amount is wrong" | same |
| No interest rate defined for proposed amount | "Interest Rate is not defined for new loan amount." | same |
| Variable installments selected but no variable-installment config found | "Number of installment or duration or interest rate does not match with selected loan product." | same |
| Installment count, duration, or interest rate does not match product details | "Number of installment or duration or interest rate does not match with selected loan product." | same |
| Interest rate not valid for proposed amount and business date | "Interest rate is not valid." | same |
| Installment amount does not match recalculated value | "Your provided installment amount is wrong" | `§ "Proposal Creation"` |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  policy = ctx.loanProductPolicy()
  details = ctx.loanProductDetails()
  agg = ctx.aggregate()
  IF policy != null THEN
    IF agg.getProposedLoanAmount() < policy.minAmount() OR agg.getProposedLoanAmount() > policy.maxAmount() THEN
      errors.put("proposedLoanAmount", LocalizedMessage(key = "LOAN_AMOUNT_OUT_OF_RANGE",
        args = [agg.getProposalNumber(), agg.getProposedLoanAmount().toString()]))
    END IF
  END IF
  computedGrant = computeGrantAmount(policy, ctx.scheme(), agg)
  IF computedGrant > 0 AND agg.getProposedGrantAmount() != computedGrant THEN
    errors.put("proposedGrantAmount", LocalizedMessage(key = "GRANT_AMOUNT_WRONG"))
  END IF
  IF agg.getInstallmentAmount().compareTo(agg.getApprovedInstallmentAmount()) != 0 THEN
    errors.put("installmentAmount", LocalizedMessage(key = "INSTALLMENT_AMOUNT_WRONG"))
  END IF
  IF details != null THEN
    IF ctx.loanProduct().usesVariableInstallments() AND NOT variableInstallmentConfigExists(ctx.loanProduct()) THEN
      errors.put("installment", LocalizedMessage(key = "INSTALLMENT_CONFIG_MISMATCH"))
    END IF
    IF agg.getNumberOfInstallments() != details.installmentCount()
       OR agg.getProposalDurationInMonths() != details.durationMonths()
       OR agg.getInterestRate().compareTo(details.interestRate()) != 0 THEN
      errors.put("installment", LocalizedMessage(key = "INSTALLMENT_CONFIG_MISMATCH"))
    END IF
  END IF
  IF NOT isValidInterestRate(agg.getProposedLoanAmount(), agg.getInterestRate(), agg.getApplicationDate()) THEN
    errors.put("interestRate", LocalizedMessage(key = "INTEREST_RATE_INVALID"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Loan Amount, Grant, Installment, Duration, and Interest Rate", "Proposal Creation"

---

### DDD-REQ-014 — Specification: LoanExposureLimitSpecification

The Loan Proposal system shall implement `LoanExposureLimitSpecification` in
`domain/specification/rules/`. Sub-rule from "Loan Exposure Limit":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Project policy enforces exposure limit and proposed amount exceeds office-and-project limit | "Proposed loan amount is not extended within loan exposure limit." | `§ "Loan Exposure Limit"` |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (BranchProjectContext) context
  policy = ctx.projectPolicy()
  IF policy != null AND policy.enforcesLoanExposureLimit() THEN
    IF NOT agg.getProposedLoanAmount().isWithinExposureLimit(policy.officeAndProjectExposureLimit()) THEN
      errors.put("proposedLoanAmount", LocalizedMessage(key = "LOAN_EXPOSURE_LIMIT_EXCEEDED"))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Loan Exposure Limit"

---

### DDD-REQ-015 — Specification: CoBorrowerSpecification

The Loan Proposal system shall implement `CoBorrowerSpecification` in
`domain/specification/rules/`. Sub-rule from "Co-Borrower":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Co-borrower supplied but loan product does not require one | "Co-borrower is not applicable for this loan product." | `§ "Co-Borrower"` |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  IF ctx.aggregate().getCoBorrower() != null AND NOT ctx.loanProduct().requiresCoBorrower() THEN
    errors.put("coBorrower", LocalizedMessage(key = "CO_BORROWER_NOT_APPLICABLE"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Co-Borrower"

---

### DDD-REQ-016 — Specification: InsurancePolicyTypeSecondInsurerSpecification

The Loan Proposal system shall implement `InsurancePolicyTypeSecondInsurerSpecification` in
`domain/specification/rules/`. It bundles 16 sub-rules from "Insurance, Policy Type, and Second Insurer":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Single policy but second insurer supplied | (localized: single policy cannot have second-insurer information) | `§ "Insurance, Policy Type, and Second Insurer"` |
| Double policy with micro-insurance but no second insurer | "Second Insurer is applicable." | same |
| Non-single policy with micro-insurance: second insurer gender missing | "Please provide the insurer gender id." | same |
| Non-single policy with micro-insurance: second insurer relationship missing | "Please provide the insurer relationship id." | same |
| Proposed amount not allowed for selected insurance product coverage | (localized: loan amount not allowed for insurance product coverage) | same |
| Double policy but no second insurer supplied | "Second Insurer is mandatory for double policy." | same |
| Second insurer (not spouse) engaged with other loans | (localized: insurer engaged with other loans) | same |
| Second insurer relationship invalid for member's marital status | (relationship-specific error) | same |
| Second insurer (not spouse) engaged with other insurance | (localized: insurer engaged with other insurance) | same |
| Second insurer has same national identity as member | (localized: second insurer identity duplicates member's) | same |
| Second insurer gender-and-relationship combination invalid for member's gender | "Please select a valid relationship as per gender from the list." | same |
| Second insurer supplied when micro-insurance is false | (localized: second-insurer not required when micro-insurance false) | same |
| Micro-insurance requested but policy type missing | "Policy type id cannot be null for micro insurance." | same |
| Micro-insurance requested but insurance product missing | "Insurance product id cannot be null for micro insurance." | same |
| Micro-insurance requested but policy type neither single nor double | "Allowed Policy type is: 1 and 2." | same |
| Micro-insurance requested but insurance product not found | "Insurance Product Id not found." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (InsuranceContext) context
  agg = ctx.aggregate()
  IF agg.getPolicyTypeId() == SINGLE AND agg.getSecondInsurer() != null THEN
    errors.put("secondInsurer", LocalizedMessage(key = "SINGLE_POLICY_NO_SECOND_INSURER"))
  END IF
  IF agg.isMicroInsurance() THEN
    IF agg.getPolicyTypeId() is null THEN
      errors.put("policyTypeId", LocalizedMessage(key = "POLICY_TYPE_NULL_FOR_MICRO_INSURANCE"))
    END IF
    IF agg.getInsuranceProductId() is null THEN
      errors.put("insuranceProductId", LocalizedMessage(key = "INSURANCE_PRODUCT_NULL_FOR_MICRO_INSURANCE"))
    END IF
    IF agg.getPolicyTypeId() != null AND agg.getPolicyTypeId() NOT IN [SINGLE, DOUBLE] THEN
      errors.put("policyTypeId", LocalizedMessage(key = "POLICY_TYPE_INVALID"))
    END IF
    IF ctx.insuranceProduct() is null THEN
      errors.put("insuranceProductId", LocalizedMessage(key = "INSURANCE_PRODUCT_NOT_FOUND"))
    END IF
    IF agg.getPolicyTypeId() == DOUBLE THEN
      IF agg.getSecondInsurer() is null THEN
        errors.put("secondInsurer", LocalizedMessage(key = "SECOND_INSURER_MANDATORY_DOUBLE"))
      ELSE
        IF agg.getSecondInsurer().gender() is null THEN
          errors.put("insurerGender", LocalizedMessage(key = "INSURER_GENDER_REQUIRED"))
        END IF
        IF agg.getSecondInsurer().relationship() is null THEN
          errors.put("insurerRelationship", LocalizedMessage(key = "INSURER_RELATIONSHIP_REQUIRED"))
        END IF
        IF NOT isValidRelationshipForMaritalStatus(agg.getSecondInsurer(), ctx.member()) THEN
          errors.put("insurerRelationship", LocalizedMessage(key = "INSURER_RELATIONSHIP_INVALID"))
        END IF
        IF NOT isValidGenderRelationshipCombo(agg.getSecondInsurer(), ctx.member()) THEN
          errors.put("insurerRelationship", LocalizedMessage(key = "INSURER_GENDER_RELATIONSHIP_INVALID"))
        END IF
        IF agg.getSecondInsurer().sameIdentityAs(ctx.member()) THEN
          errors.put("secondInsurer", LocalizedMessage(key = "INSURER_IDENTITY_DUPLICATE"))
        END IF
        IF NOT agg.getSecondInsurer().isSpouseOf(ctx.member()) THEN
          IF agg.getSecondInsurer().isEngagedWithOtherLoans() THEN
            errors.put("secondInsurer", LocalizedMessage(key = "INSURER_ENGAGED_LOANS"))
          END IF
          IF agg.getSecondInsurer().isEngagedWithOtherInsurance() THEN
            errors.put("secondInsurer", LocalizedMessage(key = "INSURER_ENGAGED_INSURANCE"))
          END IF
        END IF
      END IF
    END IF
  ELSE
    IF agg.getSecondInsurer() != null THEN
      errors.put("secondInsurer", LocalizedMessage(key = "SECOND_INSURER_NOT_REQUIRED"))
    END IF
  END IF
  IF ctx.insuranceProduct() != null AND NOT ctx.insuranceProduct().coversAmount(agg.getProposedLoanAmount()) THEN
    errors.put("proposedLoanAmount", LocalizedMessage(key = "LOAN_AMOUNT_NOT_ALLOWED_FOR_INSURANCE"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Insurance, Policy Type, and Second Insurer"

---

### DDD-REQ-017 — Specification: NomineeSpecification

The Loan Proposal system shall implement `NomineeSpecification` in `domain/specification/rules/`.
Sub-rule from "Nominee":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Update would raise nominee count beyond DCS maximum | (localized: adding a nominee is not allowed) | `§ "Nominee"` |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (MemberContext) context
  IF ctx.aggregate().getNominees().size() > DCS_MAX_NOMINEES THEN
    errors.put("nominees", LocalizedMessage(key = "NOMINEE_LIMIT_EXCEEDED"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Nominee"

---

### DDD-REQ-018 — Specification: SpecialSavingsLienSpecification

The Loan Proposal system shall implement `SpecialSavingsLienSpecification` in
`domain/specification/rules/`. Sub-rules from "Special Savings, Lien (Prottasha), and Money Plant":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Member already has loan tied to same special-savings account | "Member has already taken loan with Special Savings Account {account}." | `§ "Special Savings, Lien (Prottasha), and Money Plant"` |
| Special-savings accounts supplied but product type is not lien or money-plant | "Special Savings is not applicable for the loan." | same |
| Supplied special-savings account number does not match member's accounts | "Special Savings Account {account} does not match with member's Special Savings Account." | same |
| Lien (Prottasha): total disbursed + proposed > policy maximum | "Member can avail multiple Prottasha loans in parallel with maximum limit together of {amount} taka." | same |
| Lien on individual-landing project: approver is not AAM or AM | "Only AAM or AM can approve Prottasha loan." | same |
| Lien on group-landing project: approver is not ABM or BM | "Only ABM or BM can approve Prottasha loan." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  agg = ctx.aggregate()
  member = ctx.member()
  IF agg.getSpecialSavingsAccountIds() is not empty THEN
    IF NOT isLienOrMoneyPlantProduct(ctx.loanProduct()) THEN
      errors.put("specialSavings", LocalizedMessage(key = "SPECIAL_SAVINGS_NOT_APPLICABLE"))
    END IF
    FOR EACH accountNumber IN agg.getSpecialSavingsAccountNumbers() DO
      IF member.existingLoanForAccount(accountNumber) THEN
        errors.put("specialSavings", LocalizedMessage(key = "MEMBER_HAS_LOAN_WITH_ACCOUNT", args = [accountNumber]))
      END IF
      IF NOT member.ownsSpecialSavingsAccount(accountNumber) THEN
        errors.put("specialSavings", LocalizedMessage(key = "SPECIAL_SAVINGS_ACCOUNT_MISMATCH", args = [accountNumber]))
      END IF
    END FOR
  END IF
  IF isLienProduct(ctx.loanProduct()) THEN
    totalParallel = member.totalDisbursedProttashaAmount() + agg.getProposedLoanAmount()
    IF totalParallel > policyMaximum(ctx.projectPolicy()) THEN
      errors.put("proposedLoanAmount", LocalizedMessage(key = "PROTTASHA_PARALLEL_LIMIT_EXCEEDED",
        args = [policyMaximum(ctx.projectPolicy()).toString()]))
    END IF
    IF isIndividualLandingProject(ctx.project()) THEN
      IF agg.getLoanApproverId() NOT IN [AAM, AM] THEN
        errors.put("loanApprover", LocalizedMessage(key = "PROTTASHA_APPROVER_INVALID_INDIVIDUAL"))
      END IF
    ELSE
      IF agg.getLoanApproverId() NOT IN [ABM, BM] THEN
        errors.put("loanApprover", LocalizedMessage(key = "PROTTASHA_APPROVER_INVALID_GROUP"))
      END IF
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Special Savings, Lien (Prottasha), and Money Plant"

---

### DDD-REQ-019 — Specification: ProjectSpecificRulesSpecification

The Loan Proposal system shall implement `ProjectSpecificRulesSpecification` in
`domain/specification/rules/`. It bundles 10 sub-rules from "Project-Specific Rules (Progoti / Goti, Branch Recommender)":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Progoti document checklist supplied for non-Progoti/ADP project | "Progoti document check list not applicable." | `§ "Project-Specific Rules (Progoti / Goti, Branch Recommender)"` |
| DCS-sourced Progoti/ADP proposal with missing checklist items | "Provide all the information of Progoti Check List." | same |
| DCS-sourced proposal: branch recommender not ABM and missing PIN/role/name/date | "Provide all the information of Branch Recommender." | same |
| Goti loan for Progoti/ADP: member has active Remittance/Migration/General loan | "Parallel Loan is not allowed." | same |
| Goti loan for Progoti/ADP: member has General loan not current/closed or with overdue | "Member can not avail the loan." | same |
| First Goti loan for Progoti/ADP: approver not AM/RM/AAM/ARM | "Approver is not authorized to approve this loan." | same |
| Goti top-up within 30 days of previous: approver not SM | "Only SM is authorized to approve this loan before 30 days." | same |
| Goti top-up after 30 days of previous: approver not DM | "Only DM is authorized to approve this loan." | same |
| Goti loan for Remittance/Migration member: has active non-lien loan but no active Remittance loan with zero overdue | "Member can not avail the loan." | same |
| Shondhi loan for Progoti/ADP: agent member has active Goti loan | "Member can not avail parallel loan with Goti Loan." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (BranchProjectContext) context
  agg = ctx.aggregate()
  member = ctx.member()
  IF agg.getProgotiDocumentChecklist() != null AND NOT isProgotiOrAdpProject(ctx.project()) THEN
    errors.put("progotiChecklist", LocalizedMessage(key = "PROGOTI_CHECKLIST_NOT_APPLICABLE"))
  END IF
  // DCS-channel-specific rules (OTC proposals skip DCS recommender checks)
  // Goti / Progoti loan-type specific parallel and approver rules
  IF isGotiProductForProgotiOrAdp(agg, ctx.project()) THEN
    IF member.hasActiveRemittanceOrMigrationOrGeneralLoan() THEN
      errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
    END IF
    IF member.hasGeneralLoanNotCurrentOrClosedOrWithOverdue() THEN
      errors.put("memberEligibility", LocalizedMessage(key = "MEMBER_CANNOT_AVAIL_LOAN"))
    END IF
    IF member.isFirstGotiLoan() AND agg.getLoanApproverId() NOT IN [AM, RM, AAM, ARM] THEN
      errors.put("loanApprover", LocalizedMessage(key = "APPROVER_NOT_AUTHORIZED"))
    END IF
    IF member.isGotiTopUp() THEN
      IF member.daysSincePreviousGotiLoan() <= 30 AND agg.getLoanApproverId() != SM THEN
        errors.put("loanApprover", LocalizedMessage(key = "ONLY_SM_AUTHORIZED_BEFORE_30_DAYS"))
      END IF
      IF member.daysSincePreviousGotiLoan() > 30 AND agg.getLoanApproverId() != DM THEN
        errors.put("loanApprover", LocalizedMessage(key = "ONLY_DM_AUTHORIZED"))
      END IF
    END IF
  END IF
  // Shondhi + General loan parallel rules
  IF isShondhiLoanForProgotiOrAdp(agg, ctx.project()) AND member.hasActiveGotiLoan() THEN
    errors.put("parallelLoan", LocalizedMessage(key = "CANNOT_AVAIL_PARALLEL_WITH_GOTI"))
  END IF
  IF isGeneralLoanForProgotiOrAdp(agg, ctx.project()) AND member.hasActiveGotiLoan() THEN
    errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Project-Specific Rules (Progoti / Goti, Branch Recommender)"

---

### DDD-REQ-020 — Specification: ParallelCoExistingLoanSpecification

The Loan Proposal system shall implement `ParallelCoExistingLoanSpecification` in
`domain/specification/rules/`. Sub-rules from "Parallel and Co-Existing Loan Rules":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Product disallows parallel loans and member has at least one existing loan | "Parallel Loan is not allowed." | `§ "Parallel and Co-Existing Loan Rules"` |
| Remittance loan proposed while member has active General loan | "Parallel Loan is not allowed." | same |
| General loan proposed while member has active Remittance loan | "Parallel Loan is not allowed." | same |
| UPG project: member has no prior TUP loan | "Member must take a General Loan as the first loan." | same |
| UPG project: member has active non-closed loan of same product | "Parallel Loan is not allowed." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  agg = ctx.aggregate()
  member = ctx.member()
  IF NOT ctx.loanProduct().allowsParallelLoans() AND member.hasExistingLoan() THEN
    errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
  END IF
  IF isRemittanceLoan(agg) AND member.hasActiveGeneralLoan() THEN
    errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
  END IF
  IF isGeneralLoan(agg) AND member.hasActiveRemittanceLoan() THEN
    errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
  END IF
  IF isUPGProject(ctx.project()) THEN
    IF NOT member.hasPriorTUPLoan() THEN
      errors.put("parallelLoan", LocalizedMessage(key = "UPG_MUST_TAKE_GENERAL_LOAN_FIRST"))
    END IF
    IF member.hasActiveNonClosedLoanOfSameProduct(agg.getLoanProductId()) THEN
      errors.put("parallelLoan", LocalizedMessage(key = "PARALLEL_LOAN_NOT_ALLOWED"))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Parallel and Co-Existing Loan Rules"

---

### DDD-REQ-021 — Specification: InstallmentConfigurationSpecification

The Loan Proposal system shall implement `InstallmentConfigurationSpecification` in
`domain/specification/rules/`. Sub-rules from "Installment Configuration (Standard Validator)":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Installment amount cannot be calculated: project installment config not found | "Unable to calculate installment amount: Project installment configuration not found." | `§ "Installment Configuration (Standard Validator)"` |
| Installment amount cannot be calculated: loan product details missing or invalid | "Installment calculation failed: Loan product details are missing or invalid." | same |
| Spouse is second insurer and spouse is engaged with one or more other loan accounts | (localized: second insurer engaged with other loan accounts) | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  IF NOT projectInstallmentConfigExists(ctx.project(), ctx.loanProduct()) THEN
    errors.put("installmentConfig", LocalizedMessage(key = "INSTALLMENT_CONFIG_NOT_FOUND"))
  END IF
  IF ctx.loanProductDetails() is null THEN
    errors.put("loanProductDetails", LocalizedMessage(key = "INSTALLMENT_CALC_DETAILS_MISSING"))
  END IF
  insurer = ctx.aggregate().getSecondInsurer()
  IF insurer != null AND insurer.isSpouseOf(ctx.member()) AND insurer.hasOtherLoanAccounts() THEN
    errors.put("secondInsurer", LocalizedMessage(key = "INSURER_SPOUSE_ENGAGED_LOAN_ACCOUNTS"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Installment Configuration (Standard Validator)"

---

### DDD-REQ-022 — Specification: ModeOfPaymentRocketWalletSpecification

The Loan Proposal system shall implement `ModeOfPaymentRocketWalletSpecification` in
`domain/specification/rules/`. Sub-rules from "Mode-of-Payment Rocket Wallet Match (Shared Base)":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Disbursement mode is Rocket wallet but number does not match member's Rocket wallet | "Mode of payment's Rocket wallet number does not match with member's Rocket wallet number." | `§ "Mode-of-Payment Rocket Wallet Match (Shared Base)"` |
| Collection mode is Rocket wallet but number does not match member's Rocket wallet | "Mode of payment's Rocket wallet number does not match with member's Rocket wallet number." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (MemberContext) context
  mop = ctx.aggregate().getModeOfPayment()
  IF mop != null AND mop.subType() == ROCKET
     AND mop.rocketWalletNumber() != ctx.member().rocketWalletNumber() THEN
    errors.put("modeOfPaymentDisbursement", LocalizedMessage(key = "ROCKET_WALLET_MISMATCH"))
  END IF
  collection = ctx.aggregate().getAutoDebitCollection()
  IF collection != null AND collection.subType() == ROCKET
     AND collection.rocketWalletNumber() != ctx.member().rocketWalletNumber() THEN
    errors.put("modeOfPaymentCollection", LocalizedMessage(key = "ROCKET_WALLET_MISMATCH"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Mode-of-Payment Rocket Wallet Match (Shared Base)"

---

### DDD-REQ-023 — Specification: DigitalDisbursementSpecification

The Loan Proposal system shall implement `DigitalDisbursementSpecification` in
`domain/specification/rules/`. Sub-rules from "Digital Disbursement Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Money-plant loan with central digital-disbursement payment method | "Money Plant loan cannot take in central disbursement payment method." | `§ "Digital Disbursement Validation"` |
| Digital disbursement enabled but no mode of payment supplied | "Mode of payment is required for digital disbursement." | same |
| Auto-debit collection not mapped in branch and project | "{sub-type name} is not mapped in branch and project." | same |
| Payment channel info not mapped or sub-type mismatch | "{sub-type name} is not mapped in branch and project." | same |
| Insurance premium amount is negative | "Premium amount cannot be negative." | same |
| Premium collected from Amar Hishab but no premium amount supplied | "Premium amount can not be null for premium collection." | same |
| Premium collected from Amar Hishab but no Amar Hishab Sadharon Sonchoy account for member | "Amar Hishab Sadharon Sonchoy is not found for this member." | same |
| Amar Hishab savings balance insufficient for insurance premium | "Amar Hishab Sadharon Sonchoy doesn't have enough balance." | same |
| Selected member bank account does not belong to proposed member | "Selected bank does not map with proposed member." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (BankContext) context
  agg = ctx.aggregate()
  IF isMoneyPlantLoan(agg) AND isCentralDisbursementMode(agg.getModeOfPayment()) THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "MONEY_PLANT_CENTRAL_DISBURSEMENT_INVALID"))
  END IF
  IF agg.isDigitalDisbursement() AND agg.getModeOfPayment() is null THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "MODE_OF_PAYMENT_REQUIRED_FOR_DIGITAL"))
  END IF
  autoDebit = agg.getAutoDebitCollection()
  IF autoDebit != null AND NOT isAutoDebitMappedInBranchAndProject(autoDebit, agg.getBranchId(), agg.getProjectId()) THEN
    errors.put("autoDebitCollection", LocalizedMessage(key = "AUTO_DEBIT_NOT_MAPPED",
      args = [autoDebit.subType().name()]))
  END IF
  IF agg.getModeOfPayment() != null AND NOT isPaymentChannelMapped(agg.getModeOfPayment(), agg.getBranchId(), agg.getProjectId()) THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "PAYMENT_CHANNEL_NOT_MAPPED",
      args = [agg.getModeOfPayment().subType().name()]))
  END IF
  IF agg.getPremiumAmount() != null AND agg.getPremiumAmount() < 0 THEN
    errors.put("premiumAmount", LocalizedMessage(key = "PREMIUM_AMOUNT_NEGATIVE"))
  END IF
  IF isAmarHishabhPremiumCollection(agg) THEN
    IF agg.getPremiumAmount() is null THEN
      errors.put("premiumAmount", LocalizedMessage(key = "PREMIUM_AMOUNT_NULL"))
    END IF
    memberAccount = ctx.member().amarHishabAccount()
    IF memberAccount is null THEN
      errors.put("amarHishab", LocalizedMessage(key = "AMAR_HISHAB_NOT_FOUND"))
    ELSE IF memberAccount.balance() < agg.getPremiumAmount() THEN
      errors.put("amarHishab", LocalizedMessage(key = "AMAR_HISHAB_INSUFFICIENT_BALANCE"))
    END IF
  END IF
  IF agg.getModeOfPayment() != null AND hasBankAccount(agg.getModeOfPayment()) THEN
    IF ctx.bank() != null AND NOT ctx.bank().accountBelongsToMember(agg.getMemberId()) THEN
      errors.put("bankAccount", LocalizedMessage(key = "BANK_DOES_NOT_MATCH_MEMBER"))
    END IF
    IF ctx.bank() is null THEN
      errors.put("bankAccount", LocalizedMessage(key = "MEMBER_BANK_ACCOUNT_NOT_FOUND"))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Digital Disbursement Validation"

---

### DDD-REQ-024 — Specification: MigrationCountrySpecification

The Loan Proposal system shall implement `MigrationCountrySpecification` in
`domain/specification/rules/`. Sub-rules from "Migration Country Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Migration loan but no country selected | "Country selection is mandatory for migration loan." | `§ "Migration Country Validation"` |
| Selected migration country not configured in system | "Migration Country is not configured." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (CountryContext) context
  agg = ctx.aggregate()
  IF isMigrationLoan(agg) THEN
    IF agg.getCountryId() is null THEN
      errors.put("country", LocalizedMessage(key = "MIGRATION_COUNTRY_MANDATORY"))
    ELSE IF ctx.country() is null OR NOT ctx.country().isConfiguredForMigration() THEN
      errors.put("country", LocalizedMessage(key = "MIGRATION_COUNTRY_NOT_CONFIGURED"))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Migration Country Validation"

---

### DDD-REQ-025 — Specification: FireInsuranceSpecification

The Loan Proposal system shall implement `FireInsuranceSpecification` in
`domain/specification/rules/`. It bundles 15 sub-rules from "Fire Insurance Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Fire insurance requested but no fire-insurance premium available | "You cannot take fire insurance as there is currently no fire insurance product available." | `§ "Fire Insurance Validation"` |
| Supplied fire-insurance premium does not match calculated premium | "Provided fire insurance premium amount is wrong." | same |
| Supplied fire-insurance duration does not match calculated duration | "Provided fire insurance duration is wrong." | same |
| Fire-insurance insured amount does not match proposed loan amount | "Insured amount is mismatch with loan amount." | same |
| Member's sector is not the trading sector | "Fire Insurance is not applicable with this sector." | same |
| Fire insurance requested but no fire-insurance product selected | "Fire Insurance ID not found." | same |
| Required fire-insurance field missing | "Fire Insurance required field must be provided." | same |
| Fire-insurance business phone number not exactly 11 digits | "Please enter a valid phone number with exactly 11 digits." | same |
| Nominee marked for fire insurance but proposal does not request fire insurance | "Fire Insurance is not applicable for nominee." | same |
| Number of CSI nominees exceeds three | "CSI nominees should not exceed the limit of three." | same |
| Number of fire-insurance nominees exceeds three | "Fire Insurance nominees should not exceed the limit of three." | same |
| DCS-sourced fire-insurance proposal missing required business details | "{field} is not found." | same |
| Fire-insurance product not mapped with branch | "Fire Insurance Product is not mapped with Branch {branch}." | same |
| Fire-insurance product not mapped with project | "Fire Insurance Product is not mapped with Project {project}." | same |
| OTC-sourced proposal: no micro-insurance, no fire insurance, but policy type present | "Policy Type Id is not applicable." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (InsuranceContext) context
  agg = ctx.aggregate()
  IF agg.isWantsFireInsurance() THEN
    IF ctx.fireInsuranceProduct() is null OR ctx.fireInsuranceProduct().premiumAmount() is null THEN
      errors.put("fireInsurance", LocalizedMessage(key = "FIRE_INSURANCE_NO_PRODUCT_AVAILABLE"))
    END IF
    IF agg.getFireInsuranceProductId() is null THEN
      errors.put("fireInsuranceProductId", LocalizedMessage(key = "FIRE_INSURANCE_ID_NOT_FOUND"))
    END IF
    details = agg.getFireInsuranceDetails()
    IF details is null THEN
      errors.put("fireInsuranceDetails", LocalizedMessage(key = "FIRE_INSURANCE_REQUIRED_FIELD"))
    ELSE
      IF details.businessPhone() is null OR details.businessPhone().length() != 11 THEN
        errors.put("businessPhone", LocalizedMessage(key = "FIRE_INSURANCE_PHONE_INVALID"))
      END IF
      calculatedPremium = calculateFireInsurancePremium(agg, ctx.fireInsuranceProduct())
      IF details.fireInsurancePremiumAmount() != calculatedPremium THEN
        errors.put("fireInsurancePremium", LocalizedMessage(key = "FIRE_INSURANCE_PREMIUM_WRONG"))
      END IF
      calculatedDuration = calculateFireInsuranceDuration(agg)
      IF details.durationOfFireInsurance() != calculatedDuration THEN
        errors.put("fireInsuranceDuration", LocalizedMessage(key = "FIRE_INSURANCE_DURATION_WRONG"))
      END IF
      IF details.fireInsuranceInsuredAmount() != agg.getProposedLoanAmount() THEN
        errors.put("fireInsuranceInsuredAmount", LocalizedMessage(key = "FIRE_INSURANCE_INSURED_AMOUNT_MISMATCH"))
      END IF
    END IF
    IF NOT isTradingSector(agg.getSectorId()) THEN
      errors.put("sector", LocalizedMessage(key = "FIRE_INSURANCE_SECTOR_INVALID"))
    END IF
    IF ctx.fireInsuranceProduct() != null THEN
      IF NOT ctx.fireInsuranceProduct().isMappedWithBranch(agg.getBranchId()) THEN
        errors.put("fireInsuranceProduct", LocalizedMessage(key = "FIRE_INSURANCE_PRODUCT_NOT_MAPPED_BRANCH",
          args = [agg.getBranchCode()]))
      END IF
      IF NOT ctx.fireInsuranceProduct().isMappedWithProject(agg.getProjectId()) THEN
        errors.put("fireInsuranceProduct", LocalizedMessage(key = "FIRE_INSURANCE_PRODUCT_NOT_MAPPED_PROJECT",
          args = [agg.getProjectCode()]))
      END IF
    END IF
    csiNominees = countCsiNominees(agg.getNominees())
    IF csiNominees > 3 THEN
      errors.put("nominees", LocalizedMessage(key = "CSI_NOMINEES_LIMIT_EXCEEDED"))
    END IF
    fireNominees = countFireInsuranceNominees(agg.getNominees())
    IF fireNominees > 3 THEN
      errors.put("nominees", LocalizedMessage(key = "FIRE_INSURANCE_NOMINEES_LIMIT_EXCEEDED"))
    END IF
  ELSE
    FOR EACH nominee IN agg.getNominees() DO
      IF nominee.insuranceTypes() contains FIRE THEN
        errors.put("nominee", LocalizedMessage(key = "FIRE_INSURANCE_NOT_APPLICABLE_FOR_NOMINEE"))
      END IF
    END FOR
    IF agg.getPolicyTypeId() != null AND isOtcSource(agg) THEN
      errors.put("policyTypeId", LocalizedMessage(key = "POLICY_TYPE_NOT_APPLICABLE"))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Fire Insurance Validation"

---

### DDD-REQ-026 — Specification: AgeLimitSpecification

The Loan Proposal system shall implement `AgeLimitSpecification` in `domain/specification/rules/`.
Sub-rules from "Age-Limit Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Member is 70+ years old (except lien/money-plant for golden member) | "Member is not eligible for loan due to age. Member age is {y} years {m} months {d} days" | `§ "Age-Limit Validation"` |
| Member in 70–80 age group on individual-landing project | "Member age is not eligible for loan for the project {project}. Member age is {y} years {m} months {d} days" | same |
| Member in 80+ age group on individual-landing project | (localized: member age not eligible for project) | same |
| Micro-insurance mandatory for loan product: member in 65+ age group on non-individual project without micro-insurance | "Micro-insurance is mandatory for this loan product" | same |
| Golden member's age in 65+ or 80+ group making it invalid | "Member age is not within valid range." | same |
| Member restricted at exactly 70 years at loan end date with micro-insurance requested | "Member is not eligible for micro-insurance due to age. Member age is {y} years {m} months {d} days." | same |
| Second insurer is 70+ years old with micro-insurance and double policy | "Second insurer is not eligible for micro-insurance due to age. Second insurer age is {y} years {m} months {d} days." | same |
| Second insurer in 70–80 age group with micro-insurance/double-policy on individual-landing project | (localized: second insurer age not eligible for project) | same |
| Second insurer restricted at exactly 70 years at loan end date with micro-insurance | "Second insurer is not eligible for micro-insurance due to age. Second insurer age is {y} years {m} months {d} days." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (MemberContext) context
  agg = ctx.aggregate()
  member = ctx.member()
  memberAge = calculateAge(member.dateOfBirth(), agg.getApplicationDate())
  loanEndDate = calculateLoanEndDate(agg)

  IF memberAge.years() >= 70 THEN
    IF NOT (isLienOrMoneyPlantProduct(agg) AND member.isGolden()) THEN
      errors.put("memberAge", LocalizedMessage(key = "MEMBER_AGE_INELIGIBLE",
        args = [memberAge.years(), memberAge.months(), memberAge.days()]))
    END IF
  ELSE IF isIndividualLandingProject(ctx.project()) THEN
    IF memberAge.years() >= 70 AND memberAge.years() < 80 THEN
      errors.put("memberAge", LocalizedMessage(key = "MEMBER_AGE_INELIGIBLE_FOR_PROJECT",
        args = [ctx.project().name(), memberAge.years(), memberAge.months(), memberAge.days()]))
    END IF
    IF memberAge.years() >= 80 THEN
      errors.put("memberAge", LocalizedMessage(key = "MEMBER_AGE_INELIGIBLE_FOR_PROJECT",
        args = [ctx.project().name(), memberAge.years(), memberAge.months(), memberAge.days()]))
    END IF
  END IF
  IF ctx.loanProduct().isMicroInsuranceMandatory() AND memberAge.years() >= 65
     AND NOT isIndividualLandingProject(ctx.project()) AND NOT agg.isMicroInsurance() THEN
    errors.put("microInsurance", LocalizedMessage(key = "MICRO_INSURANCE_MANDATORY"))
  END IF
  IF member.isGolden() AND isAgeInvalidForGoldenMember(memberAge) THEN
    errors.put("memberAge", LocalizedMessage(key = "MEMBER_AGE_OUT_OF_VALID_RANGE"))
  END IF
  ageAtLoanEnd = calculateAge(member.dateOfBirth(), loanEndDate)
  IF ageAtLoanEnd.totalMonths() == 70 * 12 AND agg.isMicroInsurance() THEN
    errors.put("microInsuranceAge", LocalizedMessage(key = "MEMBER_MICRO_INSURANCE_AGE_INELIGIBLE",
      args = [ageAtLoanEnd.years(), ageAtLoanEnd.months(), ageAtLoanEnd.days()]))
  END IF
  insurer = agg.getSecondInsurer()
  IF insurer != null AND agg.isMicroInsurance() AND agg.getPolicyTypeId() == DOUBLE THEN
    insurerAge = calculateAge(insurer.dateOfBirth(), agg.getApplicationDate())
    IF insurerAge.years() >= 70 THEN
      errors.put("secondInsurerAge", LocalizedMessage(key = "SECOND_INSURER_AGE_INELIGIBLE",
        args = [insurerAge.years(), insurerAge.months(), insurerAge.days()]))
    END IF
    IF isIndividualLandingProject(ctx.project()) AND insurerAge.years() >= 70 AND insurerAge.years() < 80 THEN
      errors.put("secondInsurerAge", LocalizedMessage(key = "SECOND_INSURER_AGE_INELIGIBLE_FOR_PROJECT",
        args = [insurerAge.years(), insurerAge.months(), insurerAge.days()]))
    END IF
    insurerAgeAtEnd = calculateAge(insurer.dateOfBirth(), loanEndDate)
    IF insurerAgeAtEnd.totalMonths() == 70 * 12 THEN
      errors.put("secondInsurerMicroInsuranceAge", LocalizedMessage(key = "SECOND_INSURER_MICRO_INSURANCE_AGE_INELIGIBLE",
        args = [insurerAgeAtEnd.years(), insurerAgeAtEnd.months(), insurerAgeAtEnd.days()]))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Age-Limit Validation"

---

### DDD-REQ-027 — Specification: MoneyPlantSpecification

The Loan Proposal system shall implement `MoneyPlantSpecification` in `domain/specification/rules/`.
Sub-rules from "Money Plant Loan Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Money-plant loan but loan duration not provided | (localized: loan duration is null) | `§ "Money Plant Loan Validation"` |
| Money-plant member age in ineligible group (under 18, 80+, or 70–80 non-golden including double-policy variant) | "Member is not eligible for loan due to age. Member age is {y} years {m} months {d} days" | same |
| Money-plant special-savings account number invalid or member has no money-plant accounts | "Invalid special savings account number" | same |
| Money-plant special-savings account not provided | "Special savings account not found for this member" | same |
| Money-plant savings account does not belong to the member | (localized: savings account does not belong to member) | same |
| Proposed loan amount not provided for money-plant loan | (localized: proposed amount is null) | same |
| Proposed loan amount does not match loan portion of money-plant savings account | "Proposed amount mismatch with special savings account balance" | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (LoanProductContext) context
  agg = ctx.aggregate()
  IF NOT isMoneyPlantLoan(agg, ctx.loanProduct()) THEN RETURN errors END IF
  IF agg.getProposalDurationInMonths() is null THEN
    errors.put("duration", LocalizedMessage(key = "MONEY_PLANT_DURATION_NULL"))
  END IF
  IF agg.getProposedLoanAmount() is null THEN
    errors.put("proposedLoanAmount", LocalizedMessage(key = "MONEY_PLANT_AMOUNT_NULL"))
  END IF
  memberAge = calculateAge(ctx.member().dateOfBirth(), agg.getApplicationDate())
  IF memberAge.years() < 18 OR isAgeIneligibleForMoneyPlant(memberAge, ctx.member()) THEN
    errors.put("memberAge", LocalizedMessage(key = "MEMBER_AGE_INELIGIBLE",
      args = [memberAge.years(), memberAge.months(), memberAge.days()]))
  END IF
  specialAccounts = agg.getSpecialSavingsAccountNumbers()
  IF specialAccounts is null or empty THEN
    errors.put("specialSavings", LocalizedMessage(key = "SPECIAL_SAVINGS_NOT_FOUND"))
  ELSE
    FOR EACH accountNumber IN specialAccounts DO
      IF NOT ctx.member().hasMoneyPlantAccount(accountNumber) THEN
        errors.put("specialSavings", LocalizedMessage(key = "MONEY_PLANT_ACCOUNT_INVALID"))
      END IF
      IF NOT ctx.member().ownsAccount(accountNumber) THEN
        errors.put("specialSavings", LocalizedMessage(key = "SAVINGS_ACCOUNT_NOT_BELONG_TO_MEMBER"))
      END IF
      loanPortion = ctx.member().moneyPlantLoanPortion(accountNumber)
      IF agg.getProposedLoanAmount() != loanPortion THEN
        errors.put("proposedLoanAmount", LocalizedMessage(key = "MONEY_PLANT_AMOUNT_MISMATCH"))
      END IF
    END FOR
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Money Plant Loan Validation"

---

### DDD-REQ-028 — Specification: SchemeSectorMappingSpecification

The Loan Proposal system shall implement `SchemeSectorMappingSpecification` in
`domain/specification/rules/`. Sub-rules from "Scheme and Sector Mapping Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| No scheme mapped to loan product and sector | "Scheme list empty." | `§ "Scheme and Sector Mapping Validation"` |
| Supplied scheme not mapped to loan product | "Scheme not mapped to loan product." | same |
| Supplied sector not mapped to loan product or scheme | "Sector not mapped to loan product." | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (SchemeContext) context
  agg = ctx.aggregate()
  IF NOT schemeExistsForLoanProductAndSector(ctx.loanProduct(), agg.getSectorId()) THEN
    errors.put("scheme", LocalizedMessage(key = "SCHEME_LIST_EMPTY"))
  END IF
  IF ctx.scheme() != null AND NOT ctx.scheme().isMappedToLoanProduct(ctx.loanProduct()) THEN
    errors.put("scheme", LocalizedMessage(key = "SCHEME_NOT_MAPPED_TO_LOAN_PRODUCT"))
  END IF
  IF agg.getSectorId() != null AND NOT isSectorMappedToLoanProductOrScheme(ctx.loanProduct(), ctx.scheme(), agg.getSectorId()) THEN
    errors.put("sector", LocalizedMessage(key = "SECTOR_NOT_MAPPED_TO_LOAN_PRODUCT"))
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme and Sector Mapping Validation"

---

### DDD-REQ-029 — Specification: BankModeOfPaymentSpecification

The Loan Proposal system shall implement `BankModeOfPaymentSpecification` in
`domain/specification/rules/`. Sub-rules from "Bank Mode-of-Payment Validation":

| Sub-rule | Rejection message | Source |
|----------|-------------------|--------|
| Member bank information (account + routing) required but missing | "Member Bank Information is not found. Please add bank information first." | `§ "Bank Mode-of-Payment Validation"` |
| Non-digital-disbursement bank payment without bank account number | "Bank Account Number not found." | same |
| Insufficient balance in non-overdraft bank account | "Insufficient balance for bank account to disburse this loan" | same |
| No bank mode-of-payment sub-type supplied | "No bank mode of payment subtype found." | same |
| BEFTN payment selected for same bank as branch | "BEFTN is not allowed for same bank" | same |
| Fund-transfer payment selected for different bank from branch | "Fund Transfer is not allowed for different bank" | same |
| Cheque/online/cash-deposit missing or invalid document number | "Please input valid {document label}" | same |
| Cheque/online/cash-deposit missing document date | "Please input valid {date label}" | same |
| Supplied bank payment sub-type not a recognised method | "Unsupported bank payment sub-type" | same |

**Behaviour — validate(context):** [PSEUDOCODE]

```pseudocode
validate(context):
  errors = new HashMap
  ctx = (BankContext) context
  agg = ctx.aggregate()
  mop = agg.getModeOfPayment()
  IF mop is null OR NOT isBankMode(mop) THEN RETURN errors END IF
  IF mop.subType() is null THEN
    errors.put("modeOfPaymentSubType", LocalizedMessage(key = "NO_BANK_MOP_SUBTYPE"))
    RETURN errors
  END IF
  IF NOT isRecognisedBankSubType(mop.subType()) THEN
    errors.put("modeOfPaymentSubType", LocalizedMessage(key = "UNSUPPORTED_BANK_PAYMENT_SUBTYPE"))
  END IF
  IF ctx.bank() is null THEN
    errors.put("memberBank", LocalizedMessage(key = "MEMBER_BANK_INFO_NOT_FOUND"))
  ELSE
    IF NOT agg.isDigitalDisbursement() AND mop.bankAccountNumber() is null THEN
      errors.put("bankAccountNumber", LocalizedMessage(key = "BANK_ACCOUNT_NUMBER_NOT_FOUND"))
    END IF
    IF NOT ctx.bank().isOverdraftAccount() AND ctx.bank().balance() < agg.getProposedLoanAmount() THEN
      errors.put("bankBalance", LocalizedMessage(key = "BANK_INSUFFICIENT_BALANCE"))
    END IF
  END IF
  IF mop.subType() == BEFTN AND isSameBankAsBranch(ctx.bank(), agg.getBranchId()) THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "BEFTN_NOT_ALLOWED_SAME_BANK"))
  END IF
  IF mop.subType() == FUND_TRANSFER AND NOT isSameBankAsBranch(ctx.bank(), agg.getBranchId()) THEN
    errors.put("modeOfPayment", LocalizedMessage(key = "FUND_TRANSFER_NOT_ALLOWED_DIFFERENT_BANK"))
  END IF
  IF mop.subType() IN [CHEQUE, ONLINE, CASH_DEPOSIT] THEN
    IF mop.paymentSubTypeNumber() is null OR NOT isValidDocumentNumber(mop.paymentSubTypeNumber()) THEN
      errors.put("paymentSubTypeNumber", LocalizedMessage(key = "BANK_PAYMENT_DOCUMENT_NUMBER_INVALID",
        args = [documentLabelFor(mop.subType())]))
    END IF
    IF mop.paymentSubTypeDate() is null THEN
      errors.put("paymentSubTypeDate", LocalizedMessage(key = "BANK_PAYMENT_DOCUMENT_DATE_INVALID",
        args = [dateLabelFor(mop.subType())]))
    END IF
  END IF
  RETURN errors
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Bank Mode-of-Payment Validation"

---
### DDD-REQ-030 — Specification-to-Behavior Mapping and Composition

#### DDD-REQ-030a — Behavior Applicability Matrix

| # | Specification | `create` | `update` | `delete` | Source EARS basis |
|---|--------------|:--------:|:--------:|:--------:|-------------------|
| 1 | `MemberEligibilitySpecification` | ✅ | ✅ | ❌ | "run the full … domain validation chain" § "Proposal Creation"; "run the full domain validation chain" § "Proposal Update" |
| 2 | `BranchProjectVoConsistencySpecification` | ✅ | ✅ | ❌ | same |
| 3 | `LoanProductPolicySpecification` | ✅ | ✅ | ❌ | same |
| 4 | `RepaymentFrequencyModeOfPaymentSpecification` | ✅ | ✅ | ❌ | same |
| 5 | `LoanAmountGrantInstallmentSpecification` | ✅ | ✅ | ❌ | same |
| 6 | `LoanExposureLimitSpecification` | ✅ | ✅ | ❌ | same |
| 7 | `CoBorrowerSpecification` | ✅ | ✅ | ❌ | same |
| 8 | `InsurancePolicyTypeSecondInsurerSpecification` | ✅ | ✅ | ❌ | same |
| 9 | `NomineeSpecification` | ✅ | ✅ | ❌ | same |
| 10 | `SpecialSavingsLienSpecification` | ✅ | ✅ | ❌ | same |
| 11 | `ProjectSpecificRulesSpecification` | ✅ | ✅ | ❌ | same |
| 12 | `ParallelCoExistingLoanSpecification` | ✅ | ✅ | ❌ | same |
| 13 | `InstallmentConfigurationSpecification` | ✅ | ✅ | ❌ | same |
| 14 | `ModeOfPaymentRocketWalletSpecification` | ✅ | ✅ | ❌ | same |
| 15 | `DigitalDisbursementSpecification` | ✅ | ✅ | ❌ | "run the full structural, digital-disbursement, and domain validation chain" § "Proposal Creation" |
| 16 | `MigrationCountrySpecification` | ✅ | ✅ | ❌ | "including migration-country … validation" § "Proposal Update" |
| 17 | `FireInsuranceSpecification` | ✅ | ✅ | ❌ | "including … fire-insurance validation" § "Proposal Update" |
| 18 | `AgeLimitSpecification` | ✅ | ✅ | ❌ | "run the full domain validation chain" § "Proposal Creation", "Proposal Update" |
| 19 | `MoneyPlantSpecification` | ✅ | ✅ | ❌ | same |
| 20 | `SchemeSectorMappingSpecification` | ✅ | ✅ | ❌ | same |
| 21 | `BankModeOfPaymentSpecification` | ✅ | ✅ | ❌ | same |

`delete` — no specification chain: only HANDLER_GUARD; no ValidationContext constructed, no source data fetched.
Source: "While a loan proposal is in Pending status… soft-delete the proposal" / "only pending loan proposal can be deleted" § "Proposal Deletion"

#### DDD-REQ-030b — Context Initialisation per Behavior

| Context data | Required by specs | `create` | `update` | `delete` |
|-------------|-------------------|:--------:|:--------:|:--------:|
| `member` | 1, 2, 4, 8, 10, 11, 12, 13, 15, 18, 19 | ✅ always | ✅ always re-fetched | ❌ |
| `loanProduct` | 3, 4, 5, 6, 7, 8, 10, 11, 12, 15, 16, 18, 19, 20 | ✅ always | ✅ when `loanProductId` non-null | ❌ |
| `loanProductDetails` | 3, 5, 13 | ✅ always | ✅ when `loanProductDetailsId` non-null | ❌ |
| `loanProductPolicy` | 3, 5, 6 | ✅ always | ✅ when `loanProductPolicyId` non-null | ❌ |
| `scheme` | 3, 20 | ✅ always | ✅ when `schemeId` non-null | ❌ |
| `project` | 2, 3, 11, 12 | ✅ always | ✅ when `projectId` non-null | ❌ |
| `projectPolicy` | 2, 6, 11 | ✅ always | ✅ when `projectId` non-null | ❌ |
| `branch` | 2, 15, 17 | ✅ always | ✅ always re-fetched | ❌ |
| `villageOrganisation` | 2 | ✅ always | ✅ when `villageOrganisationId` non-null | ❌ |
| `insuranceProduct` | 8, 17 | ✅ always | ✅ when `insuranceProductId` non-null | ❌ |
| `country` | 16 | ✅ always | ✅ when `countryId` non-null | ❌ |
| `bank` | 15, 21 | ✅ always | ✅ when `bankId` non-null | ❌ |
| `aggregate (this)` | all | ✅ | ✅ | ❌ |

For `update`, null source-data entries mean the consuming spec null-guards and skips sub-rules for unchanged fields.

#### DDD-REQ-030c — AND Chain (canonical form, applies to both `create` and `update`)

```pseudocode
compositeSpec =
  new MemberEligibilitySpecification<>()                    // 1
      .and(new BranchProjectVoConsistencySpecification<>()) // 2
      .and(new LoanProductPolicySpecification<>())          // 3
      .and(new RepaymentFrequencyModeOfPaymentSpecification<>()) // 4
      .and(new LoanAmountGrantInstallmentSpecification<>()) // 5
      .and(new LoanExposureLimitSpecification<>())          // 6
      .and(new CoBorrowerSpecification<>())                 // 7
      .and(new InsurancePolicyTypeSecondInsurerSpecification<>()) // 8
      .and(new NomineeSpecification<>())                    // 9
      .and(new SpecialSavingsLienSpecification<>())         // 10
      .and(new ProjectSpecificRulesSpecification<>())       // 11
      .and(new ParallelCoExistingLoanSpecification<>())     // 12
      .and(new InstallmentConfigurationSpecification<>())   // 13
      .and(new ModeOfPaymentRocketWalletSpecification<>())  // 14
      .and(new DigitalDisbursementSpecification<>())        // 15
      .and(new MigrationCountrySpecification<>())           // 16
      .and(new FireInsuranceSpecification<>())              // 17
      .and(new AgeLimitSpecification<>())                   // 18
      .and(new MoneyPlantSpecification<>())                 // 19
      .and(new SchemeSectorMappingSpecification<>())        // 20
      .and(new BankModeOfPaymentSpecification<>())          // 21

errors = compositeSpec.validate(context)
IF errors is not empty THEN
  THROW LoanProposalValidationException(
    LoanProposalFailedEvent.validationError(traceId, errors))
END IF
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation", "Proposal Update"
> [INFERRED for composition order]

---

### DDD-REQ-031 — Domain Events: LoanProposal

> **Event Origination Rule (MANDATORY):** Every event listed here MUST be originated by calling
> `addEvent(LoanProposalEventMapper.to{Op}Event(this))` as the final statement inside the
> corresponding AR behavior method. Command Handlers MUST NEVER call `addEvent(...)`.
> Handlers only call `messagingProcessor.publish(aggregate.getEvents())` after persistence.

The `LoanProposal` aggregate shall emit the following domain events via `addEvent(...)` **inside the
respective AR behavior method** at the end of each successful state transition. All event classes are
sourced from `com.bits.ddd.contracts.loanproposal.event.*` and shall NOT be defined locally.
Event mapping shall be delegated to `LoanProposalEventMapper` in `domain/mapper/`.

| State Transition | Domain Event Class |
|-----------------|-------------------|
| create → CREATED | `LoanProposalCreatedEvent` |
| update → UPDATED | `LoanProposalUpdatedEvent` |
| delete → (soft-deleted, INACTIVE) | `LoanProposalDeletedEvent` |

**LoanProposalCreatedEvent — field schema:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Aggregate identifier (snowflake) |
| `loanProposalId` | `Long` | Distributed numeric proposal ID |
| `proposalNumber` | `String` | Generated proposal number |
| `branchId` | `Long` | Owning branch ID |
| `branchCode` | `String` | Branch code |
| `projectId` | `Long` | Project ID |
| `memberId` | `Long` | Member ID |
| `loanProductId` | `Long` | Loan product ID |
| `proposedLoanAmount` | `BigDecimal` | Proposed loan amount |
| `approvedLoanAmount` | `BigDecimal` | Approved loan amount |
| `loanProposalStatus` | `LoanProposalStatus` | Status at creation (PENDING) |
| `dataSource` | `ApiDataSource` | OTC |
| `domainStatus` | `DomainStatus` | CREATED |
| `isDigitalDisbursement` | `Boolean` | Digital-disbursement flag |
| `nominees` | `List<Nominee>` | Nominee snapshot |
| `fireInsuranceDetails` | `FireInsuranceDetails` | Fire-insurance snapshot |
| `modeOfPayment` | `OtcModeOfPayment` | Mode-of-payment snapshot |
| `applicationDate` | `LocalDate` | Application date |
| `traceId` | `String` | Correlation trace ID |
| `createdBy` | `String` | Creating actor |
| `createdAt` | `LocalDateTime` | Creation timestamp |

**LoanProposalUpdatedEvent — field schema:**
(Same fields as `LoanProposalCreatedEvent` plus all updated mutable fields from `update(updateData)`; `domainStatus` = UPDATED)

**LoanProposalDeletedEvent — field schema:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Aggregate identifier |
| `branchId` | `Long` | Owning branch ID |
| `memberId` | `Long` | Member ID |
| `proposalNumber` | `String` | Proposal number |
| `loanProposalStatus` | `LoanProposalStatus` | PENDING (at time of deletion) |
| `domainStatus` | `DomainStatus` | INACTIVE |
| `deletedBy` | `String` | Deleting actor |
| `deletedAt` | `LocalDateTime` | Deletion timestamp |
| `traceId` | `String` | Correlation trace ID |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-032 — Failure Event: LoanProposalFailedEvent

When any domain invariant or state-guard check fails, the `LoanProposal` aggregate shall
construct a `LoanProposalFailedEvent` from `com.bits.ddd.contracts.loanproposal.event.*`
and throw it wrapped inside `LoanProposalValidationException`.

> **Note:** `LoanProposalValidationException` is a **domain-specific exception** created
> within the domain application itself — not sourced from any external library. The
> GlobalExceptionHandler (provided by the implementation layer) will map it to the
> appropriate HTTP response.

Two failure factory methods on `LoanProposalFailedEvent`:
- `LoanProposalFailedEvent.validationError(traceId, errors)` — for domain invariant failures
- `LoanProposalFailedEvent.sourceDataError(traceId, errorCode, errors)` — for source-data fetch failures

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-033 — i18n Message Keys: LoanProposalMessageKey

The Loan Proposal system shall define a local `LoanProposalMessageKey` enum in the top-level
`enums/` package. Each constant shall carry a `getKey()` method returning the i18n key string.

Required keys:
- `ENTITY` — entity identifier key (`"loanProposal"`)
- `NOT_FOUND` — aggregate not found by ID ("Buffer Loan Proposal not found")
- `UPDATE_FAILED` — status guard failure on update ("Approved loan proposal cannot be modified")
- `DELETE_FAILED` — status guard failure on delete ("Only pending loan proposal can be deleted. Loan proposal Status: {status}")
- `ID_MUST_NOT_BE_NULL` — missing ID on create ("The given id must not be null!")
- `ALREADY_EXISTS` — duplicate ID on create ("Buffer Loan Proposal already exists with given id.")
- `SECOND_INSURER_REJECTED` — Member cannot be admitted as 2nd insurer ("Member can not be admitted as 2nd Insurer.")
- Additional keys derived from all specification sub-rule rejection messages in DDD-REQ-009 to DDD-REQ-029.

> 📎 [INFERRED] — required by bits.ddd pattern

---
## Application Layer

### DDD-REQ-034 — Parameter Object: LoanProposalCreationData

The Loan Proposal system shall define `LoanProposalCreationData` in `domain/param/` carrying
all data needed to construct the aggregate. It shall include all command fields plus all
source-data snapshots and a `traceId`.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Client-supplied snowflake proposal identifier |
| `traceId` | `String` | Correlation trace ID from MDC |
| `loanProposalId` | `Long` | Numeric proposal ID |
| `businessDate` | `LocalDate` | Branch accounting business date |
| `sequence` | `Long` | Sequence for proposal number generation |
| `branchId` | `Long` | Owning branch ID |
| `branchCode` | `String` | Branch code |
| `projectId` | `Long` | Project ID |
| `projectCode` | `String` | Project code |
| `villageOrganisationId` | `Long` | Village-organisation ID |
| `villageOrganisationCode` | `String` | VO code |
| `memberId` | `Long` | Member ID |
| `memberClassificationId` | `Long` | Member classification ID |
| `loanProductId` | `Long` | Loan product ID |
| `loanProductDetailsId` | `Long` | Loan product details ID |
| `loanProductPolicyId` | `Long` | Loan product policy ID |
| `schemeId` | `Long` | Scheme ID |
| `sectorId` | `Long` | Sector ID |
| `subSectorId` | `Long` | Sub-sector ID |
| `frequencyId` | `Long` | Frequency ID |
| `proposedLoanAmount` | `BigDecimal` | Proposed loan amount |
| `proposedGrantAmount` | `BigDecimal` | Proposed grant amount |
| `approvedGrantAmount` | `BigDecimal` | Approved grant amount |
| `preProposedLoanAmount` | `BigDecimal` | Pre-proposed loan amount |
| `interestRate` | `BigDecimal` | Interest rate |
| `numberOfInstallments` | `Integer` | Number of installments |
| `installmentAmount` | `BigDecimal` | Supplied installment amount |
| `recalculatedInstallmentAmount` | `BigDecimal` | Server-recalculated installment amount |
| `proposalDurationInMonths` | `Integer` | Proposal duration in months |
| `loanProposalType` | `LoanProposalType` | Proposal type |
| `microInsurance` | `Boolean` | Micro-insurance flag |
| `policyTypeId` | `Long` | Policy type ID |
| `insuranceProductId` | `Long` | Insurance product ID |
| `premiumAmount` | `BigDecimal` | Insurance premium |
| `secondInsurer` | `SecondInsurer` | Second insurer entity |
| `wantsFireInsurance` | `Boolean` | Fire-insurance flag |
| `fireInsuranceProductId` | `Long` | Fire-insurance product ID |
| `fireInsuranceDetails` | `FireInsuranceDetails` | Fire-insurance details |
| `modeOfPayment` | `OtcModeOfPayment` | Mode of payment |
| `autoDebitCollection` | `AutoDebitCollection` | Auto-debit collection |
| `nominees` | `List<Nominee>` | Nominees |
| `guardian` | `Guardian` | Guardian |
| `coBorrower` | `CoBorrower` | Co-borrower |
| `guarantors` | `List<Guarantor>` | Guarantors from member record |
| `specialSavingsAccountIds` | `List<String>` | Special-savings account IDs |
| `specialSavingsAccountNumbers` | `List<String>` | Special-savings account numbers |
| `countryId` | `Long` | Migration country ID |
| `loanApproverId` | `Long` | Loan approver ID |
| `totalPovertyScore` | `BigDecimal` | Total poverty score |
| `fieldOfficerId` | `Long` | Field officer (PO) ID |
| `loanSecurityAmount` | `BigDecimal` | Loan security amount |
| `loanSecurityBalance` | `BigDecimal` | Loan security balance |
| `member` | `Member` | Member source-data snapshot |
| `loanProduct` | `LoanProduct` | Loan product source-data snapshot |
| `loanProductDetails` | `LoanProductDetails` | Loan product details snapshot |
| `loanProductPolicy` | `LoanProductPolicy` | Loan product policy snapshot |
| `scheme` | `Scheme` | Scheme snapshot |
| `project` | `Project` | Project snapshot |
| `projectPolicy` | `ProjectPolicy` | Project policy snapshot |
| `branch` | `Branch` | Branch snapshot |
| `villageOrganisation` | `VillageOrganisation` | Village-organisation snapshot |
| `insuranceProduct` | `InsuranceProduct` | Insurance product snapshot |
| `country` | `Country` | Country snapshot |
| `bank` | `Bank` | Bank snapshot |
| (additional demographic fields) | various | spousePrimaryIncomeSource, spouseSecondaryIncomeSource, firstChildName, secondChildName, largeGroupLeaderName, largeGroupLeaderImage, reasonForLoan, numberOfChildGoToSchool, noOfPreviousLoanFromBrac, rcaEnabled, memberMobileNumber, address, contactNo, voLeaderId, voLeaderName, spouseContactNumber, earner, ownIncome, loanUser, ageType, isNewInsurer, loanRecommenderId, longitude, latitude, signConsent, consentUrl, progotiDocumentChecklist, applicationDate, approvalCode |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-035 — Parameter Object: LoanProposalUpdateData

The Loan Proposal system shall define `LoanProposalUpdateData` in `domain/param/`. Fields mirror
`LoanProposalCreationData` except:
- `id` is the existing aggregate ID (required, not updatable)
- All business fields are optional (null means "do not update this field")
- `member` and `branch` source-data snapshots are always present (re-fetched on every update)
- Other source-data snapshots are nullable (fetched only when the corresponding ID field is non-null)
- No `businessDate` / `sequence` fields (proposal number not regenerated on update)

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-036 — Parameter Object: LoanProposalDeletionData

The Loan Proposal system shall define `LoanProposalDeletionData` in `domain/param/`.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Aggregate identifier |
| `branchId` | `Long` | Owning branch ID |
| `traceId` | `String` | Correlation trace ID |
| `deletedBy` | `String` | Authenticated actor performing the deletion |
| `deletedAt` | `LocalDateTime` | Deletion timestamp |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-037 — Source Data DTO: LoanProposalSourceData

The Loan Proposal system shall define `LoanProposalSourceData` in `application/dto/` holding
optional snapshots for each of the 12 SOURCE_DATA entities. Fields shall be nullable; the
source-data service populates only the entities included in the request map.

| Field | Type | Populated when EntityType key requested |
|-------|------|-----------------------------------------|
| `member` | `Member` | `EntityType.MEMBER` |
| `loanProduct` | `LoanProduct` | `EntityType.LOAN_PRODUCT` |
| `loanProductDetails` | `LoanProductDetails` | `EntityType.LOAN_PRODUCT_DETAILS` |
| `loanProductPolicy` | `LoanProductPolicy` | `EntityType.LOAN_PRODUCT_POLICY` |
| `scheme` | `Scheme` | `EntityType.SCHEME` |
| `project` | `Project` | `EntityType.PROJECT` |
| `projectPolicy` | `ProjectPolicy` | `EntityType.PROJECT_POLICY` |
| `branch` | `Branch` | `EntityType.BRANCH` |
| `villageOrganisation` | `VillageOrganisation` | `EntityType.VILLAGE_ORGANISATION` |
| `insuranceProduct` | `InsuranceProduct` | `EntityType.INSURANCE_PRODUCT` |
| `country` | `Country` | `EntityType.COUNTRY` |
| `bank` | `Bank` | `EntityType.BANK` |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-038 — Source Data Request Map: LoanProposalSourceDataRequest

The Loan Proposal system shall define `LoanProposalSourceDataRequest` in `application/service/`
with static factory methods:

**Create command map** — always includes all 12 entities:

| EntityType key | Fetch ID source |
|---------------|-----------------|
| `EntityType.MEMBER` | `command.getMemberId()` |
| `EntityType.LOAN_PRODUCT` | `command.getLoanProductId()` |
| `EntityType.LOAN_PRODUCT_DETAILS` | `command.getLoanProductDetailsId()` |
| `EntityType.LOAN_PRODUCT_POLICY` | `command.getLoanProductPolicyId()` |
| `EntityType.SCHEME` | `command.getSchemeId()` |
| `EntityType.PROJECT` | `command.getProjectId()` |
| `EntityType.PROJECT_POLICY` | `command.getProjectId()` (derived) |
| `EntityType.BRANCH` | `command.getBranchId()` |
| `EntityType.VILLAGE_ORGANISATION` | `command.getVillageOrganisationId()` |
| `EntityType.INSURANCE_PRODUCT` | `command.getInsuranceProductId()` |
| `EntityType.COUNTRY` | `command.getCountryId()` |
| `EntityType.BANK` | `command.getBankId()` |

**Update command map** — conditionally includes (non-null fields only):

| EntityType key | Fetch ID source | Condition |
|---------------|-----------------|-----------|
| `EntityType.MEMBER` | `command.getMemberId()` | always present (re-fetched) |
| `EntityType.LOAN_PRODUCT` | `command.getLoanProductId()` | only when non-null |
| `EntityType.LOAN_PRODUCT_DETAILS` | `command.getLoanProductDetailsId()` | only when non-null |
| `EntityType.LOAN_PRODUCT_POLICY` | `command.getLoanProductPolicyId()` | only when non-null |
| `EntityType.SCHEME` | `command.getSchemeId()` | only when non-null |
| `EntityType.PROJECT` | `command.getProjectId()` | only when non-null |
| `EntityType.PROJECT_POLICY` | `command.getProjectId()` | only when non-null |
| `EntityType.BRANCH` | `command.getBranchId()` | always present (re-fetched) |
| `EntityType.VILLAGE_ORGANISATION` | `command.getVillageOrganisationId()` | only when non-null |
| `EntityType.INSURANCE_PRODUCT` | `command.getInsuranceProductId()` | only when non-null |
| `EntityType.COUNTRY` | `command.getCountryId()` | only when non-null |
| `EntityType.BANK` | `command.getBankId()` | only when non-null |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-039 — Source Data Service: LoanProposalSourceDataService

The Loan Proposal system shall implement `LoanProposalSourceDataService` implementing
`SourceDataService<LoanProposalSourceData>` in `application/service/`. The service shall:

1. Read `trace_id` from `MDC.get("trace_id")`, falling back to `"no-trace-id"`.
2. Fan out fetch calls concurrently using `CompletableFuture` per entity in the request map.
3. Delegate each fetch to `LoanProposalSourceDataFactory`.
4. On any fetch error, collect errors and throw `LoanProposalFailedEvent.sourceDataError(...)` wrapped in `LoanProposalValidationException`.
5. Be annotated with `@Observed` for Micrometer tracing.

**Behaviour — getSourceData(entityTypeIdMap):** [PSEUDOCODE]

```pseudocode
getSourceData(entityTypeIdMap):
  traceId = MDC.get("trace_id") ?? "no-trace-id"
  futures = []
  FOR EACH (entityType, id) IN entityTypeIdMap DO
    future = CompletableFuture.supplyAsync(() -> factory.fetch(entityType, id))
    futures.add(future)
  END FOR
  CompletableFuture.allOf(futures).join()
  errors = []
  FOR EACH future IN futures DO
    IF future failed THEN errors.add(future.cause.message) END IF
  END FOR
  IF errors is not empty THEN
    failureEvent = LoanProposalFailedEvent.sourceDataError(traceId, SOURCE_DATA_ERROR, errors)
    THROW LoanProposalValidationException(failureEvent)
  END IF
  RETURN assembleSourceData(futures.results)
```

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-040 — Source Data Factory: LoanProposalSourceDataFactory

The Loan Proposal system shall implement `LoanProposalSourceDataFactory` in `application/service/`.
In a `@PostConstruct` method it shall register a snapshot creator per `EntityType` key, each
delegating to its corresponding infrastructure repository. Each snapshot creator converts the
repository result using `toModel()` + `JsonUtil.convert(...)`.

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-041 — Source Data Event Listeners (All 12 SOURCE_DATA Entities)

The Loan Proposal system shall implement a `@RabbitListener`-based source-data snapshot
pipeline for all 12 SOURCE_DATA entities. For each entity the system shall implement:

1. A queue listener class annotated `@Service` with `@RabbitListener` for that entity's
   external creation, update, and deletion events.
2. A dedicated Spring Data MongoDB repository for persisting, updating, or soft-deleting
   the source-data snapshot locally.

**Per-entity listener specifications:**

#### 41.1 — Member Snapshot Listener

- **Listener class:** `MemberSnapshotListener` in `application/listener/sourcedata/`
- **Queues:** `RabbitMQConstants.MEMBER_CREATED_QUEUE`, `MEMBER_UPDATED_QUEUE`, `MEMBER_DELETED_QUEUE`
- **Local collection:** `member_snapshot`
- **Repository:** `MemberSnapshotRepository` extending `MongoRepository<MemberDocument, String>`
- **Behaviour:**
  - `onMemberCreated(event)` → upsert `MemberDocument` with all member fields; set `isActive = true`
  - `onMemberUpdated(event)` → load existing or create new; merge all updated fields; save
  - `onMemberDeleted(event)` → load; set `isActive = false`; save minimal record

#### 41.2 — LoanProduct Snapshot Listener

- **Listener class:** `LoanProductSnapshotListener`
- **Queues:** `LOAN_PRODUCT_CREATED_QUEUE`, `LOAN_PRODUCT_UPDATED_QUEUE`, `LOAN_PRODUCT_DELETED_QUEUE`
- **Local collection:** `loan_product_snapshot`
- **Repository:** `LoanProductSnapshotRepository`
- **Behaviour:** same create/update/soft-delete pattern; persist `LoanProductDocument` with product type, active dates, parallel-loan flag, micro-insurance-mandatory flag, co-borrower flag, and variable-installment flag

#### 41.3 — LoanProductDetails Snapshot Listener

- **Listener class:** `LoanProductDetailsSnapshotListener`
- **Queues:** `LOAN_PRODUCT_DETAILS_CREATED_QUEUE`, `LOAN_PRODUCT_DETAILS_UPDATED_QUEUE`, `LOAN_PRODUCT_DETAILS_DELETED_QUEUE`
- **Local collection:** `loan_product_details_snapshot`
- **Repository:** `LoanProductDetailsSnapshotRepository`
- **Behaviour:** same pattern; persist `LoanProductDetailsDocument` with frequency, duration, installment count, interest rate, active dates

#### 41.4 — LoanProductPolicy Snapshot Listener

- **Listener class:** `LoanProductPolicySnapshotListener`
- **Queues:** `LOAN_PRODUCT_POLICY_CREATED_QUEUE`, `LOAN_PRODUCT_POLICY_UPDATED_QUEUE`, `LOAN_PRODUCT_POLICY_DELETED_QUEUE`
- **Local collection:** `loan_product_policy_snapshot`
- **Repository:** `LoanProductPolicySnapshotRepository`
- **Behaviour:** same pattern; persist `LoanProductPolicyDocument` with min/max amount, grant percentage, exposure-limit flag, active dates

#### 41.5 — Scheme Snapshot Listener

- **Listener class:** `SchemeSnapshotListener`
- **Queues:** `SCHEME_CREATED_QUEUE`, `SCHEME_UPDATED_QUEUE`, `SCHEME_DELETED_QUEUE`
- **Local collection:** `scheme_snapshot`
- **Repository:** `SchemeSnapshotRepository`
- **Behaviour:** same pattern; persist `SchemeDocument` with scheme name, loan-product mapping, sector mapping, asset-grant percentage by VO category

#### 41.6 — Project Snapshot Listener

- **Listener class:** `ProjectSnapshotListener`
- **Queues:** `PROJECT_CREATED_QUEUE`, `PROJECT_UPDATED_QUEUE`, `PROJECT_DELETED_QUEUE`
- **Local collection:** `project_snapshot`
- **Repository:** `ProjectSnapshotRepository`
- **Behaviour:** same pattern; persist `ProjectDocument` with project code, name, association type (GROUP/MEMBER), landing type (individual/group)

#### 41.7 — ProjectPolicy Snapshot Listener

- **Listener class:** `ProjectPolicySnapshotListener`
- **Queues:** `PROJECT_POLICY_CREATED_QUEUE`, `PROJECT_POLICY_UPDATED_QUEUE`, `PROJECT_POLICY_DELETED_QUEUE`
- **Local collection:** `project_policy_snapshot`
- **Repository:** `ProjectPolicySnapshotRepository`
- **Behaviour:** same pattern; persist `ProjectPolicyDocument` with association type, exposure-limit flag, max parallel Prottasha amount

#### 41.8 — Branch Snapshot Listener

- **Listener class:** `BranchSnapshotListener`
- **Queues:** `BRANCH_CREATED_QUEUE`, `BRANCH_UPDATED_QUEUE`, `BRANCH_DELETED_QUEUE`
- **Local collection:** `branch_snapshot`
- **Repository:** `BranchSnapshotRepository`
- **Behaviour:** same pattern; persist `BranchDocument` with branch code, name, bank ID, last accounting business date

#### 41.9 — VillageOrganisation Snapshot Listener

- **Listener class:** `VillageOrganisationSnapshotListener`
- **Queues:** `VO_CREATED_QUEUE`, `VO_UPDATED_QUEUE`, `VO_DELETED_QUEUE`
- **Local collection:** `village_organisation_snapshot`
- **Repository:** `VillageOrganisationSnapshotRepository`
- **Behaviour:** same pattern; persist `VillageOrganisationDocument` with VO code, name, category

#### 41.10 — InsuranceProduct Snapshot Listener

- **Listener class:** `InsuranceProductSnapshotListener`
- **Queues:** `INSURANCE_PRODUCT_CREATED_QUEUE`, `INSURANCE_PRODUCT_UPDATED_QUEUE`, `INSURANCE_PRODUCT_DELETED_QUEUE`
- **Local collection:** `insurance_product_snapshot`
- **Repository:** `InsuranceProductSnapshotRepository`
- **Behaviour:** same pattern; persist `InsuranceProductDocument` with product name, coverage min/max amount, branch mapping, project mapping, premium amount

#### 41.11 — Country Snapshot Listener

- **Listener class:** `CountrySnapshotListener`
- **Queues:** `COUNTRY_CREATED_QUEUE`, `COUNTRY_UPDATED_QUEUE`, `COUNTRY_DELETED_QUEUE`
- **Local collection:** `country_snapshot`
- **Repository:** `CountrySnapshotRepository`
- **Behaviour:** same pattern; persist `CountryDocument` with country code, name, isMigrationConfigured flag

#### 41.12 — Bank Snapshot Listener

- **Listener class:** `BankSnapshotListener`
- **Queues:** `BANK_CREATED_QUEUE`, `BANK_UPDATED_QUEUE`, `BANK_DELETED_QUEUE`
- **Local collection:** `bank_snapshot`
- **Repository:** `BankSnapshotRepository`
- **Behaviour:** same pattern; persist `BankDocument` with bank code, name, branch ID of corresponding branch, account number, balance, isOverdraftAccount flag, memberId ownership mapping

> 📎 [INFERRED] — EVENT sourcing mechanism confirmed at Gate 2b/2c

---

### DDD-REQ-042 — Command Handler: CreateLoanProposalCommandHandler

The Loan Proposal system shall implement `CreateLoanProposalCommandHandler` in
`application/commandhandler/`, annotated `@RegisterCommandHandler @Service`,
implementing `CommandHandler<CreateLoanProposalCommand>`.

**Responsibilities (in order):**
1. Verify proposal ID uniqueness: reject with "Buffer Loan Proposal already exists with given id." if the ID already exists in the domain store.
2. Fetch source data: `sourceDataService.getSourceData(LoanProposalSourceDataRequest.getCreateCollectionIdMap(command))`
3. Map to param object: `LoanProposalDataMapper.toCreationData(command, sourceData)`
4. Create aggregate: `LoanProposal.create(creationData)` — domain decision + `addEvent()` happens HERE (inside AR)
5. Persist: `persistenceService.persist(loanProposal)`
6. Publish events: `messagingProcessor.publish(loanProposal.getEvents())`

**Injected dependencies:**
- `@PersistDomain DomainPersistenceService<LoanProposal, String>`
- `SourceDataService<LoanProposalSourceData>`
- `MessageProcessor`

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation"

---

### DDD-REQ-043 — Command Handler: UpdateLoanProposalCommandHandler

The Loan Proposal system shall implement `UpdateLoanProposalCommandHandler` in
`application/commandhandler/`, annotated `@RegisterCommandHandler @Service`,
implementing `CommandHandler<UpdateLoanProposalCommand>`.

**Responsibilities (in order):**
0. Load aggregate: `queryService.fetchByIdOrHandleFailure(command.getId(), traceId)` — rejects with "Buffer Loan Proposal not found" if not found
1. Fetch source data conditionally: `sourceDataService.getSourceData(LoanProposalSourceDataRequest.getUpdateCollectionIdMap(command))`
2. Map to param object: `LoanProposalDataMapper.toUpdateData(command, sourceData, existingAggregate)`
3. Update aggregate: `loanProposal.update(updateData)` — status guard + spec chain + `addEvent()` happens HERE
4. Persist: `persistenceService.persist(loanProposal)`
5. Publish events: `messagingProcessor.publish(loanProposal.getEvents())`

**Injected dependencies:**
- `@PersistDomain DomainPersistenceService<LoanProposal, String>`
- `SourceDataService<LoanProposalSourceData>`
- `MessageProcessor`
- `LoanProposalQueryService`

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Update"

---

### DDD-REQ-044 — Command Handler: DeleteLoanProposalCommandHandler

The Loan Proposal system shall implement `DeleteLoanProposalCommandHandler` in
`application/commandhandler/`, annotated `@RegisterCommandHandler @Service`,
implementing `CommandHandler<DeleteLoanProposalCommand>`.

**Responsibilities (in order):**
0. Load aggregate: `queryService.fetchByIdOrHandleFailure(command.getId(), traceId)` — rejects with "Buffer Loan Proposal not found" if not found
1. Map to param object: `LoanProposalDataMapper.toDeletionData(command)`
2. Soft-delete aggregate: `loanProposal.delete(deletionData)` — status guard inside AR; **no source data fetched**
3. Persist: `persistenceService.persist(loanProposal)`
4. Publish events: `messagingProcessor.publish(loanProposal.getEvents())`

**Injected dependencies:**
- `@PersistDomain DomainPersistenceService<LoanProposal, String>`
- `MessageProcessor`
- `LoanProposalQueryService`

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Deletion"

---

### DDD-REQ-045 — Query Service: LoanProposalQueryService

The Loan Proposal system shall implement `LoanProposalQueryService` in `application/service/`.
It shall inject `DomainRepository<LoanProposal, String>` via `@MongoDomainRepo`.

The `fetchByIdOrHandleFailure(String id, String traceId)` method (annotated `@Observed`) shall
return the aggregate or throw `LoanProposalValidationException` with a `LoanProposalFailedEvent`
payload containing the localized not-found message.

**Behaviour — fetchByIdOrHandleFailure(id, traceId):** [PSEUDOCODE]

```pseudocode
fetchByIdOrHandleFailure(id, traceId):
  result = domainRepository.findById(id)  // Optional<LoanProposal>
  IF result is empty THEN
    errors = Map.of(MessageKey.ENTITY.getKey(),
                    LocalizedMessage(key  = MessageKey.NOT_FOUND.getKey(),
                                     args = [id]))
    failureEvent = LoanProposalFailedEvent.validationError(traceId, errors)
    THROW LoanProposalValidationException(failureEvent)
  END IF
  RETURN result.value
```

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-046 — Data Mapper: LoanProposalDataMapper

The Loan Proposal system shall implement `LoanProposalDataMapper` in `application/mapper/` as a
static utility class. It shall provide:
- `toCreationData(CreateLoanProposalCommand, LoanProposalSourceData)` → `LoanProposalCreationData`
- `toUpdateData(UpdateLoanProposalCommand, LoanProposalSourceData, LoanProposal)` → `LoanProposalUpdateData`
- `toDeletionData(DeleteLoanProposalCommand)` → `LoanProposalDeletionData`

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-047 — Command Mapper: LoanProposalCommandMapper

The Loan Proposal system shall implement `LoanProposalCommandMapper` in `application/mapper/` as a
static utility class. It shall map each HTTP request DTO or event payload to its corresponding
command object:
- `toCreateCommand(traceId, CreateLoanProposalRequest)` → `CreateLoanProposalCommand`
- `toUpdateCommand(traceId, UpdateLoanProposalRequest)` → `UpdateLoanProposalCommand`
- `toDeleteCommand(traceId, id, branchId)` → `DeleteLoanProposalCommand`

> 📎 [INFERRED] — required by bits.ddd pattern

---
## Presentation Layer

### DDD-REQ-048 — Command: CreateLoanProposalCommand

The Loan Proposal system shall define `CreateLoanProposalCommand` as an immutable Java `record`
in `application/command/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `id` | `String` | `@NotBlank` | Client-supplied snowflake proposal ID (idempotency key) |
| `branchId` | `Long` | `@NotNull`, ≥1 | Owning branch ID |
| `projectId` | `Long` | `@NotNull` | Project ID |
| `memberId` | `Long` | `@NotNull` | Member ID |
| `memberClassificationId` | `Long` | `@NotNull`, ≥1 | Member classification ID |
| `loanProductId` | `Long` | `@NotNull`, ≥1 | Loan product ID |
| `loanProductDetailsId` | `Long` | `@NotNull`, ≥1 | Loan product details ID |
| `loanProductPolicyId` | `Long` | `@NotNull` | Loan product policy ID |
| `schemeId` | `Long` | `@NotNull`, ≥1 | Scheme ID |
| `frequencyId` | `Long` | `@NotNull`, ≥1 | Repayment frequency ID |
| `proposedLoanAmount` | `BigDecimal` | `@NotNull`, ≥0 | Proposed loan amount |
| `proposedGrantAmount` | `BigDecimal` | `@NotNull`, ≥0 | Proposed grant amount |
| `approvedGrantAmount` | `BigDecimal` | ≥0 | Approved grant amount |
| `interestRate` | `BigDecimal` | `@NotNull`, ≥0 | Interest rate |
| `installmentAmount` | `BigDecimal` | `@NotNull`, >0 | Installment amount |
| `proposalDurationInMonths` | `Integer` | `@NotNull`, ≥1 | Duration in months |
| `numberOfInstallments` | `Integer` | ≥0 | Number of installments |
| `sectorId` | `Long` | ≥1 | Sector ID |
| `subSectorId` | `Long` | ≥0 | Sub-sector ID |
| `villageOrganisationId` | `Long` | ≥0 | VO ID |
| `insuranceProductId` | `Long` | ≥1 | Insurance product ID |
| `microInsurance` | `Boolean` | | Micro-insurance flag |
| `policyTypeId` | `Long` | | Policy type ID |
| `wantsFireInsurance` | `Boolean` | | Fire-insurance flag |
| `fireInsuranceProductId` | `Long` | | Fire-insurance product ID |
| `fireInsuranceDetails` | `FireInsuranceDetails` | | Fire-insurance details |
| `modeOfPayment` | `OtcModeOfPayment` | | Mode of payment |
| `autoDebitCollection` | `AutoDebitCollection` | | Auto-debit collection |
| `nominees` | `List<Nominee>` | | Nominees |
| `guardian` | `Guardian` | | Guardian |
| `coBorrower` | `CoBorrower` | | Co-borrower |
| `guarantors` | `List<Guarantor>` | | Guarantors |
| `specialSavingsAccountIds` | `List<String>` | | Special-savings account IDs |
| `specialSavingsAccountNumbers` | `List<String>` | | Special-savings account numbers |
| `countryId` | `Long` | | Migration country ID |
| `loanApproverId` | `Long` | | Approver ID |
| `totalPovertyScore` | `BigDecimal` | ≥0 | Poverty score |
| `fieldOfficerId` | `Long` | ≥0 | Field officer ID |
| `loanSecurityAmount` | `BigDecimal` | ≥0 | Loan security amount |
| `loanSecurityBalance` | `BigDecimal` | ≥0 | Loan security balance |
| `bankId` | `Long` | ≥1 | Bank ID |
| `bankBranchId` | `Long` | ≥1 | Bank branch ID |
| `preProposedLoanAmount` | `BigDecimal` | ≥0 | Pre-proposed loan amount |
| (demographic fields) | various | max 100 chars each | spousePrimaryIncomeSource, spouseSecondaryIncomeSource, firstChildName, secondChildName, largeGroupLeaderName, largeGroupLeaderImage |
| `loanAccountId` | `Long` | ≥1 | Loan account ID |
| `progotiDocumentChecklist` | `ProgotiDocumentChecklist` | | Progoti checklist |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation", "Field-level validation — OTC Loan Proposal Create request"

---

### DDD-REQ-049 — Command: UpdateLoanProposalCommand

The Loan Proposal system shall define `UpdateLoanProposalCommand` as an immutable Java `record`
in `application/command/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `id` | `String` | `@NotBlank` | Existing aggregate identifier |
| `branchId` | `Long` | `@NotNull` | Branch ID |
| `loanProductId` | `Long` | `@NotNull` | Loan product ID |
| `schemeId` | `Long` | `@NotNull` | Scheme ID |
| `frequencyId` | `Long` | `@NotNull` | Frequency ID |
| `interestRate` | `BigDecimal` | `@NotNull` | Interest rate |
| `proposedLoanAmount` | `BigDecimal` | `@NotNull`, ≥0 | Proposed loan amount |
| `installmentAmount` | `BigDecimal` | `@NotNull`, >0 | Installment amount |
| `subSectorId` | `Long` | ≥0 | Sub-sector ID |
| `nominees` | `List<Nominee>` | | Updated nominees |
| `coBorrower` | `CoBorrower` | | Updated co-borrower |
| `secondInsurer` | `SecondInsurer` | | Updated second insurer |
| `modeOfPayment` | `OtcModeOfPayment` | | Updated mode of payment |
| `autoDebitCollection` | `AutoDebitCollection` | | Updated auto-debit |
| `fireInsuranceDetails` | `FireInsuranceDetails` | | Updated fire-insurance details |
| (all other updatable fields from CreateLoanProposalCommand as nullable) | various | | |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Update", "Field-level validation — OTC Update request"

---

### DDD-REQ-050 — Command: DeleteLoanProposalCommand

The Loan Proposal system shall define `DeleteLoanProposalCommand` as an immutable Java `record`
in `application/command/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `id` | `String` | `@NotBlank` | Aggregate identifier |
| `branchId` | `Long` | `@NotNull` | Branch ID |
| `deletedBy` | `String` | `@NotBlank` | Authenticated actor |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Deletion"

---

### DDD-REQ-051 — Request DTO: CreateLoanProposalRequest

The Loan Proposal system shall define `CreateLoanProposalRequest` as an immutable Java `record`
in `presentation/controller/dto/`. Fields carry `@Valid`, `@NotNull`, `@NotBlank`, `@Min`, and
`@Size` constraints derived from Phase 3 PRESENTATION_VALID rules.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `id` | `String` | `@NotNull` | Proposal identifier. "Buffer loan proposal id cannot be null." |
| `memberId` | `Long` | `@NotNull` | Member ID. "Member Information could not be null." |
| `projectId` | `Long` | `@NotNull` | Project ID |
| `memberClassificationId` | `Long` | `@NotNull`, `@Min(1)` | Classification. "Member Classification not found." |
| `loanProductId` | `Long` | `@NotNull`, `@Min(1)` | Loan product ID |
| `loanProductDetailsId` | `Long` | `@NotNull`, `@Min(1)` | Details ID. "Loan product detail id is invalid." |
| `schemeId` | `Long` | `@NotNull`, `@Min(1)` | Scheme ID. "Scheme id is invalid." |
| `frequencyId` | `Long` | `@NotNull`, `@Min(1)` | Frequency ID. "Frequency id is invalid." |
| `interestRate` | `BigDecimal` | `@NotNull`, `@Min(0)` | Interest rate. "Interest rate is invalid." |
| `installmentAmount` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` | Installment. "Installment amount must be greater than zero." |
| `proposalDurationInMonths` | `Integer` | `@Min(1)` | Duration. "Proposal duration in months is invalid." |
| `proposedLoanAmount` | `BigDecimal` | `@NotNull`, `@Min(0)` | Amount. "Proposed loan amount is invalid." |
| `proposedGrantAmount` | `BigDecimal` | `@Min(0)` | Grant amount. "Proposed grant amount is invalid." |
| `preProposedLoanAmount` | `BigDecimal` | `@Min(0)` | Pre-proposed amount. "Pre-proposed loan amount is invalid." |
| `sectorId` | `Long` | `@Min(1)` | Sector. "Sector id is invalid." |
| `subSectorId` | `Long` | `@Min(0)` | Sub-sector. "Sub Sector id is invalid." |
| `totalPovertyScore` | `BigDecimal` | `@Min(0)` | Poverty score. "Total poverty score is invalid." |
| `branchId` | `Long` | `@Min(1)` | Branch. "Branch id is invalid." |
| `villageOrganisationId` | `Long` | `@Min(0)` | VO ID. "VO id is invalid." |
| `fieldOfficerId` | `Long` | `@Min(0)` | Field officer. "PO id is invalid." |
| `approvedGrantAmount` | `BigDecimal` | `@Min(0)` | Approved grant. "Approved grant amount is invalid." |
| `loanSecurityAmount` | `BigDecimal` | `@Min(0)` | Security amount. "Loan security amount is invalid." |
| `loanSecurityBalance` | `BigDecimal` | `@Min(0)` | Security balance. "Loan security balance is invalid." |
| `numberOfInstallments` | `Integer` | `@Min(0)` | Installment count. "Number of installment is invalid." |
| `loanAccountId` | `Long` | `@Min(1)` | Loan account. "Loan account id is invalid." |
| `insuranceProductId` | `Long` | `@Min(1)` | Insurance product. "Insurance product id is invalid." |
| `bankId` | `Long` | `@Min(1)` | Bank. "Bank id is invalid." |
| `bankBranchId` | `Long` | `@Min(1)` | Bank branch. "Bank branch id is invalid." |
| `spousePrimaryIncomeSource` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `spouseSecondaryIncomeSource` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `firstChildName` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `secondChildName` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `largeGroupLeaderName` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `largeGroupLeaderImage` | `String` | `@Size(max=100)` | "Can have max 100 characters" |
| `nominees` | `List<@Valid Nominee>` | `@Valid` | Nested validation |
| `guardian` | `@Valid Guardian` | `@Valid` | Nested validation |
| `coBorrower` | `@Valid CoBorrower` | `@Valid` | Nested validation |
| `secondInsurer` | `@Valid SecondInsurer` | `@Valid` | Nested validation |
| `modeOfPayment` | `@Valid OtcModeOfPayment` | `@Valid` | Nested validation |
| `fireInsuranceDetails` | `@Valid FireInsuranceDetails` | `@Valid` | Nested validation |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Field-level validation — OTC Loan Proposal Create request"

---

### DDD-REQ-052 — Request DTO: UpdateLoanProposalRequest

The Loan Proposal system shall define `UpdateLoanProposalRequest` as an immutable Java `record`
in `presentation/controller/dto/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `id` | `String` | `@NotNull` | Aggregate identifier |
| `loanProductId` | `Long` | `@NotNull` | Loan product ID |
| `schemeId` | `Long` | `@NotNull` | Scheme ID |
| `frequencyId` | `Long` | `@NotNull` | Frequency ID |
| `interestRate` | `BigDecimal` | `@NotNull` | Interest rate |
| `proposedLoanAmount` | `BigDecimal` | `@NotNull`, `@Min(0)` | "Proposed loan amount is invalid." |
| `installmentAmount` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` | "Installment amount must be greater than zero." |
| `subSectorId` | `Long` | `@Min(0)` | Sub-sector. "Sub Sector id is invalid." |
| `nominees` | `List<@Valid Nominee>` | `@Valid` | Updated nominees |
| `coBorrower` | `@Valid CoBorrower` | `@Valid` | Updated co-borrower |
| `secondInsurer` | `@Valid SecondInsurer` | `@Valid` | Updated second insurer |
| `modeOfPayment` | `@Valid OtcModeOfPayment` | `@Valid` | Updated mode of payment |
| `autoDebitCollection` | `@Valid AutoDebitCollection` | `@Valid` | Updated auto-debit |
| `fireInsuranceDetails` | `@Valid FireInsuranceDetails` | `@Valid` | Updated fire-insurance |
| (other updatable fields) | various | nullable | All other fields from create request that can be updated |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Field-level validation — OTC Update request"

---

### DDD-REQ-053 — Request DTO: DeleteLoanProposalRequest

The Loan Proposal system shall define `DeleteLoanProposalRequest` as an immutable Java `record`
in `presentation/controller/dto/`. Since deletion is triggered by path parameters, this DTO is
minimal (used for event-listener path only).

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `id` | `String` | `@NotBlank` | Aggregate identifier |
| `branchId` | `Long` | `@NotNull` | Branch ID |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-054 — REST Controller: LoanProposalCommandController

The Loan Proposal system shall implement `LoanProposalCommandController` in
`presentation/controller/`, extending `BaseApiController`. It shall inject only `CommandBus`.

For each command endpoint the controller shall:
1. Read `trace_id` from `@RequestAttribute(MdcConstants.TRACE_ID)`.
2. Call `MDC.put("trace_id", trace_id)`.
3. Map the request to a command via `LoanProposalCommandMapper`.
4. Dispatch via `commandBus.handle(command)`.
5. Return `ResponseEntity<ApiResponse>` with `HttpStatus.ACCEPTED` (202).

**Endpoints:**

| Method | Path | Command | Note |
|--------|------|---------|------|
| `POST` | `/api/loan-proposals` | `CreateLoanProposalCommand` | Business-day check applied via `@PrePersist`/filter |
| `DELETE` | `/api/loan-proposals/{branchId}/{id}` | `DeleteLoanProposalCommand` | Soft delete |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation", "Proposal Deletion"

---

### DDD-REQ-055 — Event Listener: LoanProposalCommandListener

The Loan Proposal system shall implement `LoanProposalCommandListener` in
`presentation/listener/`. It handles Update and Delete commands arriving via RabbitMQ.

For each queue-driven command the listener shall:
1. Annotate the handler method with `@RabbitListener(queues = RabbitMQConstants.{QUEUE})`.
2. Deserialize the message body: `JsonUtil.deserialize(message.getBody(), {Verb}LoanProposalCommand.class)`.
3. Dispatch: `commandBus.handle(command)`.

Exceptions shall NOT be caught here; RabbitMQ retry handles failures.

**Queue bindings:**

| Queue constant | Command deserialized | Handler method |
|---------------|---------------------|----------------|
| `RabbitMQConstants.LOAN_PROPOSAL_UPDATE_COMMAND_QUEUE` | `UpdateLoanProposalCommand` | `onUpdateCommand(message)` |
| `RabbitMQConstants.LOAN_PROPOSAL_DELETE_COMMAND_QUEUE` | `DeleteLoanProposalCommand` | `onDeleteCommand(message)` |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Update", "Proposal Deletion" [Gate 3a: Update and Delete have @RabbitListener entry points]

---
## Infrastructure Layer

### DDD-REQ-056 to DDD-REQ-067 — MongoDB Documents: 12 SOURCE_DATA Entities

For each of the 12 SOURCE_DATA entities the Loan Proposal system shall define:
- A `@Document`-annotated MongoDB document class in `infrastructure/persistence/document/sourcedata/`
- A `toModel()` method converting the document to the domain model type

#### DDD-REQ-056 — MemberDocument

- **Collection:** `member_snapshot`
- **Key fields:** `memberId`, `memberClassificationId`, `branchId`, `projectId`, `voId`, `voCode`, `status`, `isScreened`, `dateOfBirth`, `rocketWalletNumber`, `isGolden`, `identityCards`, `amarHishabAccounts`, `activeLoans`, `specialSavingsAccounts`
- **Index candidates:** `memberId` (unique sparse), `branchId + status`
- **toModel():** Converts to `Member` domain model, populating all eligibility fields.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-057 — LoanProductDocument

- **Collection:** `loan_product_snapshot`
- **Key fields:** `loanProductId`, `productCode`, `name`, `activePeriodStart`, `activePeriodEnd`, `allowsParallelLoans`, `requiresCoBorrower`, `isMicroInsuranceMandatory`, `usesVariableInstallments`, `projectMappings`, `officeMappings`, `categoryMappings`, `frequencyMappings`, `memberCategoryMappings`, `loanProductType`
- **Index candidates:** `loanProductId` (unique sparse)
- **toModel():** Converts to `LoanProduct` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-058 — LoanProductDetailsDocument

- **Collection:** `loan_product_details_snapshot`
- **Key fields:** `loanProductDetailsId`, `loanProductId`, `frequencyId`, `durationMonths`, `installmentCount`, `interestRate`, `activePeriodStart`, `activePeriodEnd`, `variableInstallmentConfigs`
- **Index candidates:** `loanProductDetailsId` (unique sparse), `loanProductId`
- **toModel():** Converts to `LoanProductDetails` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-059 — LoanProductPolicyDocument

- **Collection:** `loan_product_policy_snapshot`
- **Key fields:** `loanProductPolicyId`, `loanProductId`, `minAmount`, `maxAmount`, `grantPercentage`, `enforcesLoanExposureLimit`, `officeAndProjectExposureLimit`, `activePeriodStart`, `activePeriodEnd`
- **Index candidates:** `loanProductPolicyId` (unique sparse), `loanProductId`
- **toModel():** Converts to `LoanProductPolicy` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-060 — SchemeDocument

- **Collection:** `scheme_snapshot`
- **Key fields:** `schemeId`, `name`, `loanProductMappings`, `sectorMappings`, `assetGrantPercentageByVoCategory`
- **Index candidates:** `schemeId` (unique sparse)
- **toModel():** Converts to `Scheme` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-061 — ProjectDocument

- **Collection:** `project_snapshot`
- **Key fields:** `projectId`, `code`, `name`, `associationType` (GROUP/MEMBER), `landingType` (INDIVIDUAL/GROUP), `branchMappings`
- **Index candidates:** `projectId` (unique sparse), `code` (unique sparse)
- **toModel():** Converts to `Project` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-062 — ProjectPolicyDocument

- **Collection:** `project_policy_snapshot`
- **Key fields:** `projectPolicyId`, `projectId`, `associationType`, `enforcesLoanExposureLimit`, `maxProttashaParallelAmount`
- **Index candidates:** `projectPolicyId` (unique sparse), `projectId`
- **toModel():** Converts to `ProjectPolicy` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-063 — BranchDocument

- **Collection:** `branch_snapshot`
- **Key fields:** `branchId`, `code`, `name`, `bankId`, `lastAccountingBusinessDate`
- **Index candidates:** `branchId` (unique sparse), `code` (unique sparse)
- **toModel():** Converts to `Branch` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-064 — VillageOrganisationDocument

- **Collection:** `village_organisation_snapshot`
- **Key fields:** `voId`, `code`, `name`, `category`, `projectId`, `branchId`, `isActive`
- **Index candidates:** `voId` (unique sparse), `code`
- **toModel():** Converts to `VillageOrganisation` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-065 — InsuranceProductDocument

- **Collection:** `insurance_product_snapshot`
- **Key fields:** `insuranceProductId`, `name`, `coverageMinAmount`, `coverageMaxAmount`, `premiumAmount`, `branchMappings`, `projectMappings`, `type` (MICRO_INSURANCE/FIRE)
- **Index candidates:** `insuranceProductId` (unique sparse), `type`
- **toModel():** Converts to `InsuranceProduct` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-066 — CountryDocument

- **Collection:** `country_snapshot`
- **Key fields:** `countryId`, `code`, `name`, `isMigrationConfigured`
- **Index candidates:** `countryId` (unique sparse), `code` (unique sparse)
- **toModel():** Converts to `Country` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

#### DDD-REQ-067 — BankDocument

- **Collection:** `bank_snapshot`
- **Key fields:** `bankId`, `accountNumber`, `routingNumber`, `balance`, `isOverdraftAccount`, `memberId`, `branchBankId`
- **Index candidates:** `bankId` (unique sparse), `memberId`
- **toModel():** Converts to `Bank` domain model.

> 📎 [INFERRED] — EVENT sourcing from Gate 2b

---

### DDD-REQ-068 to DDD-REQ-079 — Repositories: 12 SOURCE_DATA Snapshot Repositories

For each of the 12 SOURCE_DATA entities the Loan Proposal system shall define a Spring Data
MongoDB repository interface in `infrastructure/persistence/repository/sourcedata/`:

| DDD-REQ | Repository | Extends | Primary finder methods |
|---------|-----------|---------|------------------------|
| DDD-REQ-068 | `MemberSnapshotRepository` | `MongoRepository<MemberDocument, String>` | `findByMemberId(Long)`, `findByBranchIdAndStatus(Long, MemberStatus)` |
| DDD-REQ-069 | `LoanProductSnapshotRepository` | `MongoRepository<LoanProductDocument, String>` | `findByLoanProductId(Long)`, `findByProductCode(String)` |
| DDD-REQ-070 | `LoanProductDetailsSnapshotRepository` | `MongoRepository<LoanProductDetailsDocument, String>` | `findByLoanProductDetailsId(Long)`, `findByLoanProductIdAndFrequencyId(Long, Long)` |
| DDD-REQ-071 | `LoanProductPolicySnapshotRepository` | `MongoRepository<LoanProductPolicyDocument, String>` | `findByLoanProductPolicyId(Long)`, `findByLoanProductId(Long)` |
| DDD-REQ-072 | `SchemeSnapshotRepository` | `MongoRepository<SchemeDocument, String>` | `findBySchemeId(Long)`, `findByName(String)` |
| DDD-REQ-073 | `ProjectSnapshotRepository` | `MongoRepository<ProjectDocument, String>` | `findByProjectId(Long)`, `findByCode(String)` |
| DDD-REQ-074 | `ProjectPolicySnapshotRepository` | `MongoRepository<ProjectPolicyDocument, String>` | `findByProjectPolicyId(Long)`, `findByProjectId(Long)` |
| DDD-REQ-075 | `BranchSnapshotRepository` | `MongoRepository<BranchDocument, String>` | `findByBranchId(Long)`, `findByCode(String)` |
| DDD-REQ-076 | `VillageOrganisationSnapshotRepository` | `MongoRepository<VillageOrganisationDocument, String>` | `findByVoId(Long)`, `findByCode(String)`, `findByProjectIdAndIsActive(Long, Boolean)` |
| DDD-REQ-077 | `InsuranceProductSnapshotRepository` | `MongoRepository<InsuranceProductDocument, String>` | `findByInsuranceProductId(Long)`, `findByType(String)` |
| DDD-REQ-078 | `CountrySnapshotRepository` | `MongoRepository<CountryDocument, String>` | `findByCountryId(Long)`, `findByCode(String)` |
| DDD-REQ-079 | `BankSnapshotRepository` | `MongoRepository<BankDocument, String>` | `findByBankId(Long)`, `findByMemberId(Long)` |

---

### DDD-REQ-080 — Persistence Configuration

The Loan Proposal system shall define `LoanProposalPersistenceConfiguration` in
`infrastructure/config/`. It shall declare the `LoanProposal` aggregate root document and
shall configure the following indexes for the `loan_proposal` collection:

| Index | Fields | Type |
|-------|--------|------|
| Default `_id` | `_id` | Auto |
| `idx_proposal_number_branch` | `proposalNumber ASC, branchId ASC` | Unique sparse |
| `idx_member_status` | `memberId ASC, loanProposalStatus ASC` | Compound |
| `idx_branch_data_source` | `branchId ASC, dataSource ASC` | Compound |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-081 — Queue Configuration: LoanProposalQueueConfig

The Loan Proposal system shall define `LoanProposalQueueConfig` as a `@Configuration` bean in
`infrastructure/config/`. It shall declare the following AMQP constructs:

**Exchanges:**

| Exchange name | Type |
|--------------|------|
| `loan-proposal.exchange` | Topic |
| `loan-proposal.dlx` | Topic (Dead Letter) |

**Queues and routing keys:**

| Queue | Routing key | DLQ | Purpose |
|-------|-------------|-----|---------|
| `loan-proposal.created.queue` | `loan-proposal.created` | `loan-proposal.created.dlq` | Publish created events |
| `loan-proposal.updated.queue` | `loan-proposal.updated` | `loan-proposal.updated.dlq` | Publish updated events |
| `loan-proposal.deleted.queue` | `loan-proposal.deleted` | `loan-proposal.deleted.dlq` | Publish deleted events |
| `loan-proposal.failed.queue` | `loan-proposal.failed` | `loan-proposal.failed.dlq` | Publish failure events |
| `loan-proposal.update-command.queue` | `loan-proposal.update-command` | `loan-proposal.update-command.dlq` | Receive update commands |
| `loan-proposal.delete-command.queue` | `loan-proposal.delete-command` | `loan-proposal.delete-command.dlq` | Receive delete commands |

Each source-data entity queue pair follows the same pattern (separate exchange per domain).

> 📎 [INFERRED] — required by bits.ddd pattern; [UNCHANGED] queue topology derived from EARS RabbitMQ conventions

---

### DDD-REQ-082 — Application Bootstrap

The Loan Proposal system shall define `LoanProposalCommandServiceApplication` as the Spring Boot
entry point in the root `application/` package.

Required annotations: `@SpringBootApplication`, `@EnableMongoRepositories`, `@EnableRabbit`

> 📎 [INFERRED] — required by bits.ddd pattern

---
## Cross-Cutting Requirements

The following requirements are copied from the source EARS and apply without modification
to the Loan Proposal command-side service.

### [UNCHANGED] Audit and Record Lifecycle

| # | EARS Requirement |
|---|-----------------|
| CCR-AUD-001 | Whenever the Loan Proposal system creates a loan proposal record, the Loan Proposal system shall persist a creation audit trail containing: timestamp of creation, identifier of the user who created the record (createdBy), and the system source. |
| CCR-AUD-002 | Whenever the Loan Proposal system modifies a loan proposal record, the Loan Proposal system shall persist a modification audit trail containing: timestamp of modification, identifier of the user who modified the record (lastModifiedBy), and the nature of the modification. |
| CCR-AUD-003 | Whenever the Loan Proposal system soft-deletes a loan proposal record, the Loan Proposal system shall persist a deletion audit trail containing: timestamp of deletion, identifier of the user who deleted the record (deletedBy), and the reason for deletion where available. |
| CCR-AUD-004 | The Loan Proposal system shall embed audit fields (createdAt, createdBy, lastModifiedAt, lastModifiedBy) in every persisted domain document. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Audit and Record Lifecycle"

---

### [UNCHANGED] Authentication and Authorisation

| # | EARS Requirement |
|---|-----------------|
| CCR-AUTH-001 | All Loan Proposal API endpoints shall require a valid OAuth2 Bearer token. Requests without a valid token shall be rejected with HTTP 401 Unauthorized. |
| CCR-AUTH-002 | The Loan Proposal system shall enforce role-based access control using database-driven path-and-role ACL. Each API path shall be mapped to the set of roles permitted to invoke it. Requests from authenticated users whose role is not in the permitted set for the requested path shall be rejected with HTTP 403 Forbidden. |
| CCR-AUTH-003 | The Loan Proposal system shall read the authenticated user's identity and role from the validated JWT claims and make them available to all command and query handlers for audit logging purposes. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Authentication and Authorisation"

---

### [UNCHANGED] Request Handling

| # | EARS Requirement |
|---|-----------------|
| CCR-REQ-001 | Every inbound HTTP request shall be assigned a globally unique `trace_id` (UUID v4). The `trace_id` shall be propagated through the MDC and included in all log messages and error responses for the duration of the request. |
| CCR-REQ-002 | The Loan Proposal system shall apply request-size and content-type validation at the gateway or filter level. Requests that exceed the configured maximum body size shall be rejected with HTTP 413 Payload Too Large. |
| CCR-REQ-003 | The Loan Proposal system shall respond within the configured timeout for all synchronous operations. Operations that exceed the timeout shall return HTTP 504 Gateway Timeout. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Request Handling"

---

### [UNCHANGED] Operational Cross-Cuts

| # | EARS Requirement |
|---|-----------------|
| CCR-OPS-001 | The Loan Proposal system shall expose health-check endpoints for liveness and readiness probes (MongoDB, RabbitMQ, and any external HTTP dependency). |
| CCR-OPS-002 | The Loan Proposal system shall instrument all command and query handlers with Micrometer `@Observed` annotations for distributed tracing and metrics collection. No Prometheus-specific annotations are permitted. |
| CCR-OPS-003 | The Loan Proposal system shall log all command dispatch, domain event publication, and exception events at the `INFO` level in structured JSON format, including `trace_id`, `command_type`, and `outcome`. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Operational Cross-Cuts"

---

### [UNCHANGED] Error Response Format

All error responses from the Loan Proposal command-side service shall conform to the following
standard API error body:

```json
{
  "timestamp": "ISO-8601 UTC timestamp",
  "status": <HTTP status code>,
  "error": "<HTTP reason phrase>",
  "message": "<localized user-facing message>",
  "path": "<request URI>",
  "traceId": "<trace_id from MDC>"
}
```

**HTTP status-code mapping table:**

| Condition | HTTP Status |
|-----------|------------|
| Domain validation failure (specification chain) | `422 Unprocessable Entity` |
| Aggregate not found | `404 Not Found` |
| Duplicate aggregate ID (idempotency conflict) | `409 Conflict` |
| Field-level constraint violation (`@Valid`) | `400 Bad Request` |
| Authentication failure (no / invalid token) | `401 Unauthorized` |
| Authorisation failure (insufficient role) | `403 Forbidden` |
| Source-data fetch failure | `503 Service Unavailable` |
| Unhandled / infrastructure exception | `500 Internal Server Error` |
| Request timeout | `504 Gateway Timeout` |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Error Response Format"

---

## Out of Scope

The following items were identified during the planning phase and excluded from this
DDD specification by explicit user decision (Gate 1b / Gate 5a).

| Item | Reason |
|------|--------|
| **Event Sourcing snapshotting** | Not currently required; standard bits.ddd persistence model sufficient. May be revisited in a future phase. |
| **External-System Backfill** | The `resync` / `resyncById` / loan-account back-fill operations from the EARS async-pipeline section are fully excluded. These are data-migration / operational concerns outside the DDD command boundary. |
| **Async pipeline operations: resyncById, resyncAll, bulkResync, replayFailed, dateRangeResync** | Gate 1b decision: all seven async/pipeline/maintenance operations are Out of Scope. |
| **Buffer member/proposal traversal (deprecated maintenance endpoint)** | Deprecated; no requirement to generate a DDD-REQ for this. |
| **Downstream Kafka pipeline** | Disabled by default per EARS `[DISABLED]` marker. To be enabled in a future phase when the downstream consumer service is onboarded. |
| **Speed Search indexing** | External-system concern (Elasticsearch / search service); not a responsibility of the Loan Proposal command service. |
| **Saga / Process Manager** | Gate 5a decision: no Saga needed. The service owns its full transaction boundary. |

---

## Open Questions

| # | Question | Source | Status |
|---|----------|--------|--------|
| OQ-001 | The EARS file contains an open question: "`childInformationDto` field — no class found in codebase. What does this field represent? Should it be a list of child objects with name, age, school attendance? Or is it a free-text string?" | `LoanProposalOTC-EARS-review-2-resolved.md § "Open Questions"` | Open — no class definition found in source codebase |
| OQ-002 | The EARS file notes that `guarntors` (guarantors) are "attached from member record" — does the OTC channel independently submit guarantor details, or is this always sourced from the member's profile? The command handler currently copies `guarantors` from `creationData.guarantors` (supplied by the calling service). If this field must be forced-loaded from member source data regardless of the caller, a change to the data mapper will be required. | Inferred from EARS guarantee section | Open |
| OQ-003 | The `loanProposalType` field is defaulted to `NORMAL_LOAN` when null. Confirm the expected default for loans that arrive with an absent type field via the `@RabbitListener` update path. | Inferred | Open |
| OQ-004 | The `disbursementDate` and `firstRepaymentDate` fields appear in the proposal body but are described as post-disbursement. Confirm whether OTC channel proposals can set these dates at creation time or only at disbursement. | `LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Creation"` | Open |

---

*End of LoanProposal-Command-DDD-EARS.md*
