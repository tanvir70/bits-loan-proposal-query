# Loan Proposal — DDD/CQRS/Event-Sourcing Business Requirements Specification (EARS) — Query Side

> **Format:** Easy Approach to Requirements Syntax (EARS) — DDD/CQRS/ES Edition — Query / Read-Model Side
> **Source:** Converted from layered-architecture EARS spec `LoanProposalOTC-EARS-review-2-resolved.md`
> **Architecture:** bits.ddd query-side read projection service
> **Read Model:** `LoanProposalReadDocument` — MongoDB collection `loan_proposal_read`
> **Companion:** `output/loan-proposal/LoanProposal-Command-DDD-EARS.md` (command side)
> **Output path:** `output/loan-proposal/LoanProposal-Query-DDD-EARS.md`
> **Traceability:** Every DDD-REQ cites the source EARS section it was derived from,
>   in the format: 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "{Section / Sub-section heading}"

---

## Planning Phase Gate Decisions

> Gate decisions below are copied from the Command file for completeness. They are binding here.
> All 12 gates confirmed. Refer to `LoanProposal-Command-DDD-EARS.md § Planning Phase Gate Decisions`
> for the full ledger. Only decisions that directly affect the query-side are restated below.

### Gate 1b — Queries Confirmed
**Decision:** ✅ Six query-side operations: `getById`, `list`, `searchV2`, `getSchemeDetails`, `getUPGTUPExistingLoans`, `monitoringFeed`. Seven async/pipeline/maintenance ops → Out of Scope.
**Architectural effect:** Six Query objects, six Query Handlers, one Projection Handler (three @RabbitListener methods), one Read Repository, one Query Controller with six GET endpoints.

### Gate 2a — Entity Roles
**Decision:** ✅ See Command file. Relevant to the query side: the read model document must denormalise all embedded entities (Nominee, Guardian, CoBorrower, SecondInsurer, Guarantor, FireInsuranceDetails, OtcModeOfPayment, AutoDebitCollection, ProgotiDocumentChecklist) into a single flat-ish document. Computed fields: `creditShieldExpiryDate`, `fireInsuranceExpiryDate`. Enriched reference field: `fireInsuranceProductName`. Status-conditional fields: loan-account financials when `loanProposalStatus = DISBURSED`.

### Gate 4 — Split Output Format
**Decision:** ✅ This file is the query-side companion. Full command-side specification is in `LoanProposal-Command-DDD-EARS.md`.

---

## Document Conventions

| Marker | Meaning |
|--------|---------|
| `[INFERRED]` | Required by bits.ddd pattern; no explicit requirement in source EARS |
| `[UNCHANGED]` | Copied verbatim from the original EARS (cross-cutting concern) |
| `[PSEUDOCODE]` | Java-adjacent pseudocode block — describes behaviour without full implementation syntax |
| `[READ-MODEL]` | CQRS query-side requirement — covered in this Query file |
| `[ASYNC]` | Asynchronous side effect — out of scope for this DDD specification |
| `[OUT-OF-SCOPE]` | Identified in source EARS but excluded from this DDD specification |

---

## Query-Side Inventory Matrix

| # | Item Type | Count | Items |
|---|-----------|-------|-------|
| 1 | Read Model Document | 1 | `LoanProposalReadDocument` |
| 2 | Projection Handler | 1 | `LoanProposalProjectionHandler` (3 @RabbitListener methods) |
| 3 | Query objects | 6 | `GetLoanProposalByIdQuery`, `ListLoanProposalsQuery`, `SearchLoanProposalsV2Query`, `GetSchemeDetailsQuery`, `GetUPGTUPExistingLoansQuery`, `GetMonitoringFeedQuery` |
| 4 | Query Handlers | 6 | `GetLoanProposalByIdQueryHandler`, `ListLoanProposalsQueryHandler`, `SearchLoanProposalsV2QueryHandler`, `GetSchemeDetailsQueryHandler`, `GetUPGTUPExistingLoansQueryHandler`, `GetMonitoringFeedQueryHandler` |
| 5 | Response DTOs | 2 | `LoanProposalResponse` (full detail), `LoanProposalListItem` (summary) |
| 6 | Specialized responses | 3 | `SchemeDetailsResponse`, `UPGTUPExistingLoansResponse`, `MonitoringFeedResponse` |
| 7 | Query Request DTOs | 4 | `ListLoanProposalsRequest`, `SearchLoanProposalsV2Request`, `GetSchemeDetailsRequest`, `GetMonitoringFeedRequest` |
| 8 | Read Repository | 1 | `LoanProposalReadRepository` |
| 9 | Read Mapper | 1 | `LoanProposalReadMapper` |
| 10 | Query Controller | 1 | `LoanProposalQueryController` |

---

## Read Model Domain Layer

### DDD-REQ-Q001 — Read Model Document: LoanProposalReadDocument

The Loan Proposal system shall define `LoanProposalReadDocument` as a `@Document(collection = "loan_proposal_read")`
MongoDB document in `infrastructure/readmodel/document/`. It shall mirror all aggregate fields,
plus computed and reference-enriched fields, to serve all six query operations from a single
denormalized collection.

**Field schema:**

All fields from `LoanProposal` aggregate (see DDD-REQ-002 in Command file) shall be present, plus:

| Additional Field | Type | Description | Population |
|-----------------|------|-------------|------------|
| `creditShieldExpiryDate` | `LocalDate` | Computed credit-shield expiry. Null if micro-insurance false. | Computed from applicationDate + proposalDurationInMonths |
| `fireInsuranceExpiryDate` | `LocalDate` | Computed fire-insurance expiry. Null if wantsFireInsurance false. | Computed from applicationDate + fireInsuranceDetails.durationOfFireInsurance |
| `fireInsuranceProductName` | `String` | Fire-insurance product name. | Enriched from insurance product snapshot on projection |
| `loanAccountBalance` | `BigDecimal` | Loan account outstanding balance. Null unless `loanProposalStatus = DISBURSED`. | Set on DISBURSED event callback |
| `loanAccountScheduledAmount` | `BigDecimal` | Loan account scheduled repayment amount. Null unless DISBURSED. | Set on DISBURSED event callback |
| `loanAccountOverdueAmount` | `BigDecimal` | Loan account overdue amount. Null unless DISBURSED. | Set on DISBURSED event callback |
| `loanAccountStatus` | `String` | Loan account status code. Null unless DISBURSED. | Set on DISBURSED event callback |
| `enrollmentStatusOverride` | `String` | Overridden enrollment status for mobile-app source proposals. | Applied during getById enrichment for mobile-app source |
| `isActive` | `Boolean` | False when proposal is soft-deleted (DomainStatus.INACTIVE). | Set to false on deletion projection |

**Index candidates:**

| Index name | Fields | Type | Supports |
|-----------|--------|------|---------|
| `_id` | `_id` | Default | getById |
| `idx_read_branch_datasource_status` | `branchId ASC, dataSource ASC, loanProposalStatus ASC` | Compound | list, searchV2, monitoringFeed |
| `idx_read_member_status` | `memberId ASC, loanProposalStatus ASC` | Compound | scheme details, UPG/TUP |
| `idx_read_created_at` | `createdAt DESC` | Single | date-range queries (list, monitoring) |
| `idx_read_proposal_number` | `proposalNumber ASC` | Unique sparse | business-key lookup |
| `idx_read_proposal_type` | `loanProposalType ASC` | Single | GOOD_LOAN exclusion filter |
| `idx_read_is_active` | `isActive ASC` | Single | exclude soft-deleted records |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval", "Monitoring Feed", "Scheme/Reference Lookups"

---

## Query Application Layer

### DDD-REQ-Q002 — Projection Handler: LoanProposalProjectionHandler

The Loan Proposal system shall implement `LoanProposalProjectionHandler` in
`application/projection/`. It shall consume domain events published to the
RabbitMQ event queues and maintain the `loan_proposal_read` MongoDB collection
in near-real-time.

**Queue bindings:**

| Queue constant | Event consumed | Handler method |
|---------------|---------------|----------------|
| `RabbitMQConstants.LOAN_PROPOSAL_CREATED_EVENT_QUEUE` | `LoanProposalCreatedEvent` | `onLoanProposalCreated(message)` |
| `RabbitMQConstants.LOAN_PROPOSAL_UPDATED_EVENT_QUEUE` | `LoanProposalUpdatedEvent` | `onLoanProposalUpdated(message)` |
| `RabbitMQConstants.LOAN_PROPOSAL_DELETED_EVENT_QUEUE` | `LoanProposalDeletedEvent` | `onLoanProposalDeleted(message)` |

**Behaviour — onLoanProposalCreated(message):** [PSEUDOCODE]

```pseudocode
onLoanProposalCreated(message):
  event = JsonUtil.deserialize(message.getBody(), LoanProposalCreatedEvent.class)
  // Compute derived fields
  creditShieldExpiry  = null
  fireInsuranceExpiry = null
  IF event.isMicroInsurance() AND event.getApplicationDate() != null THEN
    creditShieldExpiry = event.getApplicationDate().plusMonths(event.getProposalDurationInMonths())
  END IF
  IF event.isWantsFireInsurance() AND event.getFireInsuranceDetails() != null THEN
    fireInsuranceExpiry = event.getApplicationDate().plusMonths(event.getFireInsuranceDetails().durationOfFireInsurance())
  END IF
  // Enrich fire-insurance product name
  fireInsuranceProductName = null
  IF event.getFireInsuranceProductId() != null THEN
    insuranceProduct = insuranceProductSnapshotRepository.findByInsuranceProductId(event.getFireInsuranceProductId())
    fireInsuranceProductName = insuranceProduct?.name
  END IF
  // Build and upsert read document
  readDocument = LoanProposalReadMapper.toReadDocument(event,
    creditShieldExpiry, fireInsuranceExpiry, fireInsuranceProductName)
  readDocument.isActive = true
  readRepository.save(readDocument)
```

**Behaviour — onLoanProposalUpdated(message):** [PSEUDOCODE]

```pseudocode
onLoanProposalUpdated(message):
  event = JsonUtil.deserialize(message.getBody(), LoanProposalUpdatedEvent.class)
  existing = readRepository.findById(event.getId()).orElseGet(() -> new LoanProposalReadDocument())
  // Recompute derived fields using updated values
  creditShieldExpiry  = null
  fireInsuranceExpiry = null
  IF event.isMicroInsurance() AND event.getApplicationDate() != null THEN
    creditShieldExpiry = event.getApplicationDate().plusMonths(event.getProposalDurationInMonths())
  END IF
  IF event.isWantsFireInsurance() AND event.getFireInsuranceDetails() != null THEN
    fireInsuranceExpiry = event.getApplicationDate().plusMonths(event.getFireInsuranceDetails().durationOfFireInsurance())
  END IF
  fireInsuranceProductName = existing.getFireInsuranceProductName()
  IF event.getFireInsuranceProductId() != null THEN
    insuranceProduct = insuranceProductSnapshotRepository.findByInsuranceProductId(event.getFireInsuranceProductId())
    fireInsuranceProductName = insuranceProduct?.name
  END IF
  // Merge all changed fields into existing read document
  updatedDocument = LoanProposalReadMapper.mergeUpdatedFields(existing, event,
    creditShieldExpiry, fireInsuranceExpiry, fireInsuranceProductName)
  readRepository.save(updatedDocument)
```

**Behaviour — onLoanProposalDeleted(message):** [PSEUDOCODE]

```pseudocode
onLoanProposalDeleted(message):
  event = JsonUtil.deserialize(message.getBody(), LoanProposalDeletedEvent.class)
  existing = readRepository.findById(event.getId())
  IF existing is empty THEN RETURN END IF   // idempotent: skip if already gone
  doc = existing.value
  doc.isActive                = false
  doc.domainStatus            = DomainStatus.INACTIVE
  doc.lastModifiedAt          = event.getDeletedAt()
  doc.lastModifiedBy          = event.getDeletedBy()
  readRepository.save(doc)
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval"; [INFERRED] CQRS projection handler

---

### DDD-REQ-Q003 — Query: GetLoanProposalByIdQuery

The Loan Proposal system shall define `GetLoanProposalByIdQuery` as an immutable Java `record`
in `application/query/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `branchKey` | `String` | `@NotBlank` | Branch code (path variable) |
| `id` | `String` | `@NotBlank` | Aggregate identifier |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — Retrieve by ID"

---

### DDD-REQ-Q004 — Query Handler: GetLoanProposalByIdQueryHandler

The Loan Proposal system shall implement `GetLoanProposalByIdQueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  doc = readRepository.findByIdAndBranchCodeAndIsActive(query.getId(), query.getBranchKey(), true)
  IF doc is null THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(query.getTraceId(),
        Map.of("loanProposal", LocalizedMessage(key = MessageKey.NOT_FOUND.getKey(), args = [query.getId()]))))
  END IF

  // Enrichment: first repayment date
  firstRepaymentDate = computeFirstRepaymentDate(doc)

  // Enrichment: credit-shield expiry (from read document computed field)
  creditShieldExpiry = doc.getCreditShieldExpiryDate()

  // Enrichment: fire-insurance expiry (from read document computed field)
  fireInsuranceExpiry = doc.getFireInsuranceExpiryDate()

  // Enrichment: loan-account financials (only when DISBURSED)
  loanAccountInfo = null
  IF doc.getLoanProposalStatus() == DISBURSED AND doc.getLoanAccountId() != null THEN
    loanAccountInfo = LoanAccountInfo(
      balance         = doc.getLoanAccountBalance(),
      scheduledAmount = doc.getLoanAccountScheduledAmount(),
      overdueAmount   = doc.getLoanAccountOverdueAmount(),
      status          = doc.getLoanAccountStatus())
  END IF

  // Enrichment: enrollment status override (mobile-app source)
  enrollmentStatus = doc.getEnrollmentStatusOverride()
  IF doc.getDataSource() == MOBILE_APP THEN
    enrollmentStatus = overrideEnrollmentStatus(doc.getMemberId())
  END IF

  RETURN LoanProposalReadMapper.toDetailResponse(doc, firstRepaymentDate, creditShieldExpiry,
    fireInsuranceExpiry, loanAccountInfo, enrollmentStatus)
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — Retrieve by ID"

---

### DDD-REQ-Q005 — Query: ListLoanProposalsQuery

The Loan Proposal system shall define `ListLoanProposalsQuery` as an immutable Java `record`
in `application/query/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `branchKey` | `String` | `@NotBlank` | Branch code (path variable) |
| `voId` | `Long` | optional | Village-organisation filter |
| `memberId` | `Long` | optional | Member filter |
| `loanProductId` | `Long` | optional | Loan-product filter |
| `schemeId` | `Long` | optional | Scheme filter |
| `projectId` | `Long` | optional | Project filter |
| `fromDate` | `LocalDate` | optional | Application date range start |
| `toDate` | `LocalDate` | optional | Application date range end |
| `statuses` | `List<LoanProposalStatus>` | optional | Status filter list |
| `proposalType` | `LoanProposalType` | optional | Proposal type filter |
| `page` | `Integer` | `@Min(0)`, default 0 | Page number |
| `size` | `Integer` | `@Min(1)`, default 20 | Page size |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — List Proposals"

---

### DDD-REQ-Q006 — Query Handler: ListLoanProposalsQueryHandler

The Loan Proposal system shall implement `ListLoanProposalsQueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Fixed filters (always applied):**

| Filter | Value | Source |
|--------|-------|--------|
| `dataSource` | `ApiDataSource.OTC` | "OTC data source only" per EARS |
| `loanProposalType` NOT IN | `[GOOD_LOAN]` | "exclude GOOD_LOAN type" per EARS |
| `isActive` | `true` | exclude soft-deleted proposals |
| **default status exclusions** | `BM_APPROVAL_PENDING`, `DRAFT`, `BM_REJECT` | "excluded from standard OTC list view" per EARS |

**Optional filters (applied when non-null in query):**

| Filter | Field matched | Description |
|--------|--------------|-------------|
| `branchKey` | `branchCode` | Always applied — restrict to branch |
| `voId` | `villageOrganisationId` | VO scope |
| `memberId` | `memberId` | Member scope |
| `loanProductId` | `loanProductId` | Product scope |
| `schemeId` | `schemeId` | Scheme scope |
| `projectId` | `projectId` | Project scope |
| `fromDate` / `toDate` | `applicationDate` | Date range (inclusive both ends) |
| `statuses` | `loanProposalStatus IN (...)` | Status list filter (overrides default exclusions when explicitly supplied) |
| `proposalType` | `loanProposalType` | Type filter |

**Response:** `Page<LoanProposalListItem>` (trimmed summary DTO).

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  spec = Criteria.where("branchCode").is(query.getBranchKey())
                 .and("dataSource").is(OTC)
                 .and("loanProposalType").ne(GOOD_LOAN)
                 .and("isActive").is(true)
  IF query.getStatuses() is null or empty THEN
    spec = spec.and("loanProposalStatus").nin([BM_APPROVAL_PENDING, DRAFT, BM_REJECT])
  ELSE
    spec = spec.and("loanProposalStatus").in(query.getStatuses())
  END IF
  IF query.getVoId() != null THEN spec = spec.and("villageOrganisationId").is(query.getVoId()) END IF
  IF query.getMemberId() != null THEN spec = spec.and("memberId").is(query.getMemberId()) END IF
  IF query.getLoanProductId() != null THEN spec = spec.and("loanProductId").is(query.getLoanProductId()) END IF
  IF query.getSchemeId() != null THEN spec = spec.and("schemeId").is(query.getSchemeId()) END IF
  IF query.getProjectId() != null THEN spec = spec.and("projectId").is(query.getProjectId()) END IF
  IF query.getFromDate() != null THEN spec = spec.and("applicationDate").gte(query.getFromDate()) END IF
  IF query.getToDate() != null THEN spec = spec.and("applicationDate").lte(query.getToDate()) END IF
  IF query.getProposalType() != null THEN spec = spec.and("loanProposalType").is(query.getProposalType()) END IF

  pageable = PageRequest.of(query.getPage(), query.getSize(), Sort.by("createdAt").descending())
  page = readRepository.findAll(spec, pageable)
  RETURN page.map(doc -> LoanProposalReadMapper.toListItem(doc))
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — List Proposals"

---

### DDD-REQ-Q007 — Query: SearchLoanProposalsV2Query

The Loan Proposal system shall define `SearchLoanProposalsV2Query` as an immutable Java `record`
in `application/query/`.

| Field | Type | Description |
|-------|------|-------------|
| `traceId` | `String` | Correlation trace ID |
| `branchKey` | `String` | Branch code |
| `searchTerm` | `String` | Optional free-text search term (matches proposalNumber, memberName) |
| `voId` | `Long` | Optional VO filter |
| `memberId` | `Long` | Optional member filter |
| `loanProductId` | `Long` | Optional product filter |
| `statuses` | `List<LoanProposalStatus>` | Optional status list |
| `proposalType` | `LoanProposalType` | Optional type filter |
| `fromDate` | `LocalDate` | Optional date range start |
| `toDate` | `LocalDate` | Optional date range end |
| `page` | `Integer` | Page number (default 0) |
| `size` | `Integer` | Page size (default 20) |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — Search V2"

---

### DDD-REQ-Q008 — Query Handler: SearchLoanProposalsV2QueryHandler

The Loan Proposal system shall implement `SearchLoanProposalsV2QueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Fixed filters (same as ListLoanProposalsQueryHandler):** `dataSource = OTC`, `loanProposalType != GOOD_LOAN`, `isActive = true`.

**V2 additional logic:**
- When `searchTerm` is non-null and non-blank, add a regex `$or` filter across `proposalNumber` and `memberName` fields.
- Default status exclusions (`BM_APPROVAL_PENDING`, `DRAFT`, `BM_REJECT`) apply unless `statuses` explicitly supplied.

**Response:** `Page<LoanProposalListItem>`.

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  spec = Criteria.where("branchCode").is(query.getBranchKey())
                 .and("dataSource").is(OTC)
                 .and("loanProposalType").ne(GOOD_LOAN)
                 .and("isActive").is(true)
  IF query.getStatuses() is null or empty THEN
    spec = spec.and("loanProposalStatus").nin([BM_APPROVAL_PENDING, DRAFT, BM_REJECT])
  ELSE
    spec = spec.and("loanProposalStatus").in(query.getStatuses())
  END IF
  IF query.getSearchTerm() is not blank THEN
    regexPattern = Pattern.compile(query.getSearchTerm(), CASE_INSENSITIVE)
    spec = spec.andOperator(
      Criteria.where("proposalNumber").regex(regexPattern)
              .orOperator(Criteria.where("memberName").regex(regexPattern)))
  END IF
  // Apply remaining optional filters same as list handler
  pageable = PageRequest.of(query.getPage(), query.getSize(), Sort.by("createdAt").descending())
  page = readRepository.findAll(spec, pageable)
  RETURN page.map(doc -> LoanProposalReadMapper.toListItem(doc))
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — Search V2"

---

### DDD-REQ-Q009 — Query: GetSchemeDetailsQuery

The Loan Proposal system shall define `GetSchemeDetailsQuery` as an immutable Java `record`
in `application/query/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `memberId` | `Long` | `@NotNull` | Member identifier |
| `loanProductId` | `Long` | `@NotNull` | Loan product identifier |
| `schemeId` | `Long` | `@NotNull` | Scheme identifier |
| `branchId` | `Long` | `@NotNull` | Branch identifier |
| `voId` | `Long` | optional | Village-organisation identifier |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — Get Scheme Details"

---

### DDD-REQ-Q010 — Query Handler: GetSchemeDetailsQueryHandler

The Loan Proposal system shall implement `GetSchemeDetailsQueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  // Check member existence
  member = memberSnapshotRepository.findByMemberId(query.getMemberId())
  IF member is null THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(query.getTraceId(),
        Map.of("member", LocalizedMessage(key = "MEMBER_NOT_FOUND"))))
  END IF

  // Derive VO category (for asset-grant percentage lookup)
  voCategory = null
  IF query.getVoId() != null THEN
    vo = voSnapshotRepository.findByVoId(query.getVoId())
    voCategory = vo?.category
  END IF

  // Count and sum existing loans for scheme-level parallel limits
  existingLoanCount = readRepository.countByMemberIdAndSchemeIdAndActiveStatuses(
    query.getMemberId(), query.getSchemeId(), [PENDING, APPROVED, DISBURSED])
  existingLoanTotalAmount = readRepository.sumProposedLoanAmountByMemberIdAndSchemeIdAndActiveStatuses(
    query.getMemberId(), query.getSchemeId(), [PENDING, APPROVED, DISBURSED])

  // Fetch scheme details
  scheme = schemeSnapshotRepository.findBySchemeId(query.getSchemeId())
  assetGrantPercentage = scheme?.assetGrantPercentageForVoCategory(voCategory) ?? 0

  RETURN SchemeDetailsResponse(
    schemeId             = query.getSchemeId(),
    memberFound          = true,
    voCategory           = voCategory,
    existingLoanCount    = existingLoanCount,
    existingLoanTotal    = existingLoanTotalAmount,
    assetGrantPercentage = assetGrantPercentage)
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — Get Scheme Details"

---

### DDD-REQ-Q011 — Query: GetUPGTUPExistingLoansQuery

The Loan Proposal system shall define `GetUPGTUPExistingLoansQuery` as an immutable Java `record`
in `application/query/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `branchKey` | `String` | `@NotBlank` | Branch code |
| `loanProductId` | `Long` | `@NotNull` | UPG/TUP loan product identifier |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — UPG/TUP Existing Loans"

---

### DDD-REQ-Q012 — Query Handler: GetUPGTUPExistingLoansQueryHandler

The Loan Proposal system shall implement `GetUPGTUPExistingLoansQueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  existingLoanCount  = readRepository.countByBranchCodeAndLoanProductIdAndActiveStatuses(
    query.getBranchKey(), query.getLoanProductId(), [PENDING, APPROVED, DISBURSED])
  existingLoanAmount = readRepository.sumProposedLoanAmountByBranchCodeAndLoanProductIdAndActiveStatuses(
    query.getBranchKey(), query.getLoanProductId(), [PENDING, APPROVED, DISBURSED])

  RETURN UPGTUPExistingLoansResponse(
    branchKey          = query.getBranchKey(),
    loanProductId      = query.getLoanProductId(),
    existingLoanCount  = existingLoanCount,
    existingLoanAmount = existingLoanAmount)
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — UPG/TUP Existing Loans"

---

### DDD-REQ-Q013 — Query: GetMonitoringFeedQuery

The Loan Proposal system shall define `GetMonitoringFeedQuery` as an immutable Java `record`
in `application/query/`.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `traceId` | `String` | `@NotBlank` | Correlation trace ID |
| `fromDateTime` | `LocalDateTime` | `@NotNull` | Feed window start (UTC) |
| `toDateTime` | `LocalDateTime` | `@NotNull` | Feed window end (UTC). Must be ≤ fromDateTime + 24 hours. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Monitoring Feed"

---

### DDD-REQ-Q014 — Query Handler: GetMonitoringFeedQueryHandler

The Loan Proposal system shall implement `GetMonitoringFeedQueryHandler` in
`application/queryhandler/`, annotated `@RegisterQueryHandler @Service`.

**Behaviour — handle(query):** [PSEUDOCODE]

```pseudocode
handle(query):
  windowHours = Duration.between(query.getFromDateTime(), query.getToDateTime()).toHours()
  IF windowHours > 24 THEN
    THROW LoanProposalValidationException(
      LoanProposalFailedEvent.validationError(query.getTraceId(),
        Map.of("dateRange", LocalizedMessage(key = "MONITORING_FEED_WINDOW_EXCEEDED",
          args = ["24"]))))
  END IF

  proposals = readRepository.findByCreatedAtBetween(
    query.getFromDateTime(), query.getToDateTime())

  RETURN MonitoringFeedResponse(
    fromDateTime = query.getFromDateTime(),
    toDateTime   = query.getToDateTime(),
    totalCount   = proposals.size(),
    items        = proposals.stream().map(LoanProposalReadMapper::toMonitoringItem).toList())
```

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Monitoring Feed"

---
## Query Presentation Layer

### DDD-REQ-Q015 — Response DTO: LoanProposalResponse (Full Detail)

The Loan Proposal system shall define `LoanProposalResponse` as an immutable Java `record` in
`presentation/dto/` for the `getById` operation. It shall include all aggregate fields plus
computed and enriched fields.

| Field | Type | Source / Note |
|-------|------|---------------|
| `id` | `String` | Aggregate identifier |
| `loanProposalId` | `Long` | Numeric proposal ID |
| `proposalNumber` | `String` | Human-readable proposal number |
| `proposalReferenceNumber` | `String` | External reference |
| `branchId` | `Long` | Branch ID |
| `branchCode` | `String` | Branch code |
| `projectId` | `Long` | Project ID |
| `projectCode` | `String` | Project code |
| `villageOrganisationId` | `Long` | VO ID |
| `villageOrganisationCode` | `String` | VO code |
| `memberId` | `Long` | Member ID |
| `memberClassificationId` | `Long` | Classification ID |
| `loanProductId` | `Long` | Product ID |
| `loanProductDetailsId` | `Long` | Product details ID |
| `loanProductPolicyId` | `Long` | Policy ID |
| `schemeId` | `Long` | Scheme ID |
| `sectorId` | `Long` | Sector ID |
| `subSectorId` | `Long` | Sub-sector ID |
| `frequencyId` | `Long` | Frequency ID |
| `proposedLoanAmount` | `BigDecimal` | Proposed amount |
| `approvedLoanAmount` | `BigDecimal` | Approved amount |
| `proposedGrantAmount` | `BigDecimal` | Proposed grant |
| `approvedGrantAmount` | `BigDecimal` | Approved grant |
| `interestRate` | `BigDecimal` | Interest rate |
| `numberOfInstallments` | `Integer` | Installment count |
| `approvedNumberOfInstallments` | `Integer` | Approved installment count |
| `installmentAmount` | `BigDecimal` | Per-installment amount |
| `approvedInstallmentAmount` | `BigDecimal` | Approved installment |
| `proposalDurationInMonths` | `Integer` | Duration |
| `approvedDurationInMonths` | `Integer` | Approved duration |
| `loanProposalStatus` | `LoanProposalStatus` | Business lifecycle status |
| `loanProposalType` | `LoanProposalType` | Proposal type |
| `dataSource` | `ApiDataSource` | OTC |
| `microInsurance` | `Boolean` | Micro-insurance flag |
| `policyTypeId` | `Long` | Policy type |
| `insuranceProductId` | `Long` | Insurance product |
| `premiumAmount` | `BigDecimal` | Premium amount |
| `secondInsurer` | `SecondInsurerDto` | Second insurer |
| `wantsFireInsurance` | `Boolean` | Fire-insurance flag |
| `fireInsuranceProductId` | `Long` | Fire-insurance product ID |
| `fireInsuranceProductName` | `String` | Enriched product name |
| `fireInsuranceDetails` | `FireInsuranceDetailsDto` | Fire-insurance details |
| `modeOfPayment` | `OtcModeOfPaymentDto` | Mode of payment |
| `autoDebitCollection` | `AutoDebitCollectionDto` | Auto-debit |
| `isDigitalDisbursement` | `Boolean` | Digital-disbursement flag |
| `nominees` | `List<NomineeDto>` | Nominees |
| `guardian` | `GuardianDto` | Guardian |
| `coBorrower` | `CoBorrowerDto` | Co-borrower |
| `guarantors` | `List<GuarantorDto>` | Guarantors |
| `countryId` | `Long` | Migration country |
| `loanApproverId` | `Long` | Approver |
| `totalPovertyScore` | `BigDecimal` | Poverty score |
| `applicationDate` | `LocalDate` | Application date |
| `disbursementDate` | `LocalDate` | Disbursement date |
| `firstRepaymentDate` | `LocalDate` | First repayment date (enriched) |
| `creditShieldExpiryDate` | `LocalDate` | Credit-shield expiry (computed) |
| `fireInsuranceExpiryDate` | `LocalDate` | Fire-insurance expiry (computed) |
| `loanAccountId` | `Long` | Post-disbursement loan account ID |
| `loanAccountBalance` | `BigDecimal` | Loan account balance (DISBURSED only) |
| `loanAccountScheduledAmount` | `BigDecimal` | Scheduled repayment (DISBURSED only) |
| `loanAccountOverdueAmount` | `BigDecimal` | Overdue amount (DISBURSED only) |
| `loanAccountStatus` | `String` | Loan account status (DISBURSED only) |
| `enrollmentStatus` | `String` | Enrollment status (possibly overridden for mobile-app source) |
| `progotiDocumentChecklist` | `ProgotiDocumentChecklistDto` | Progoti checklist |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `createdBy` | `String` | Creator |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |
| `lastModifiedBy` | `String` | Last modifier |
| `disbursedAmount` | `BigDecimal` | Disbursed amount |
| `disbursedBy` | `String` | Disburser |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — Retrieve by ID"

---

### DDD-REQ-Q016 — Response DTO: LoanProposalListItem (Summary)

The Loan Proposal system shall define `LoanProposalListItem` as an immutable Java `record` in
`presentation/dto/` for list and search operations. It shall contain a trimmed summary subset
suitable for table/list views.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Aggregate identifier |
| `proposalNumber` | `String` | Human-readable proposal number |
| `memberId` | `Long` | Member ID |
| `memberName` | `String` | Member name (denormalised) |
| `memberMobileNumber` | `String` | Member mobile number |
| `branchId` | `Long` | Branch ID |
| `branchCode` | `String` | Branch code |
| `projectId` | `Long` | Project ID |
| `projectCode` | `String` | Project code |
| `villageOrganisationId` | `Long` | VO ID |
| `villageOrganisationCode` | `String` | VO code |
| `loanProductId` | `Long` | Product ID |
| `schemeId` | `Long` | Scheme ID |
| `proposedLoanAmount` | `BigDecimal` | Proposed amount |
| `approvedLoanAmount` | `BigDecimal` | Approved amount |
| `loanProposalStatus` | `LoanProposalStatus` | Status |
| `loanProposalType` | `LoanProposalType` | Type |
| `applicationDate` | `LocalDate` | Application date |
| `approvalFlowStatus` | `String` | Approval workflow state |
| `dataSource` | `ApiDataSource` | Data source |
| `isDigitalDisbursement` | `Boolean` | Digital flag |
| `createdAt` | `LocalDateTime` | Creation time |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval — List Proposals", "Proposal Retrieval — Search V2"

---

### DDD-REQ-Q017 — Response DTO: SchemeDetailsResponse

The Loan Proposal system shall define `SchemeDetailsResponse` as an immutable Java `record` in
`presentation/dto/`.

| Field | Type | Description |
|-------|------|-------------|
| `schemeId` | `Long` | Scheme identifier |
| `memberFound` | `Boolean` | Whether member exists |
| `voCategory` | `String` | Village-organisation category (for asset-grant percentage) |
| `existingLoanCount` | `Integer` | Count of active existing loans for this member + scheme |
| `existingLoanTotal` | `BigDecimal` | Sum of active loan amounts for this member + scheme |
| `assetGrantPercentage` | `BigDecimal` | Asset-grant percentage applicable for VO category |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — Get Scheme Details"

---

### DDD-REQ-Q018 — Response DTO: UPGTUPExistingLoansResponse

The Loan Proposal system shall define `UPGTUPExistingLoansResponse` as an immutable Java `record`
in `presentation/dto/`.

| Field | Type | Description |
|-------|------|-------------|
| `branchKey` | `String` | Branch code |
| `loanProductId` | `Long` | UPG/TUP loan product ID |
| `existingLoanCount` | `Integer` | Count of branch-wide existing UPG/TUP loans |
| `existingLoanAmount` | `BigDecimal` | Sum of branch-wide existing UPG/TUP loan amounts |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Scheme/Reference Lookups — UPG/TUP Existing Loans"

---

### DDD-REQ-Q019 — Response DTO: MonitoringFeedResponse

The Loan Proposal system shall define `MonitoringFeedResponse` as an immutable Java `record`
in `presentation/dto/`.

| Field | Type | Description |
|-------|------|-------------|
| `fromDateTime` | `LocalDateTime` | Feed window start |
| `toDateTime` | `LocalDateTime` | Feed window end |
| `totalCount` | `Integer` | Number of proposals in the window |
| `items` | `List<MonitoringFeedItem>` | Monitoring feed items |

`MonitoringFeedItem` fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Aggregate ID |
| `proposalNumber` | `String` | Proposal number |
| `memberId` | `Long` | Member ID |
| `memberName` | `String` | Member name |
| `branchCode` | `String` | Branch code |
| `projectCode` | `String` | Project code |
| `proposedLoanAmount` | `BigDecimal` | Proposed amount |
| `loanProposalStatus` | `LoanProposalStatus` | Status |
| `dataSource` | `ApiDataSource` | Data source |
| `createdAt` | `LocalDateTime` | Creation timestamp |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Monitoring Feed"

---

### DDD-REQ-Q020 — Read Mapper: LoanProposalReadMapper

The Loan Proposal system shall implement `LoanProposalReadMapper` as a static utility class
in `application/mapper/`. It shall provide:

- `toReadDocument(LoanProposalCreatedEvent, creditShieldExpiry, fireInsuranceExpiry, productName)` → `LoanProposalReadDocument`
- `mergeUpdatedFields(LoanProposalReadDocument, LoanProposalUpdatedEvent, creditShieldExpiry, fireInsuranceExpiry, productName)` → `LoanProposalReadDocument`
- `toDetailResponse(LoanProposalReadDocument, firstRepaymentDate, creditShieldExpiry, fireInsuranceExpiry, loanAccountInfo, enrollmentStatus)` → `LoanProposalResponse`
- `toListItem(LoanProposalReadDocument)` → `LoanProposalListItem`
- `toMonitoringItem(LoanProposalReadDocument)` → `MonitoringFeedItem`

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-Q021 — Query Controller: LoanProposalQueryController

The Loan Proposal system shall implement `LoanProposalQueryController` in
`presentation/controller/`, extending `BaseApiController`. It shall inject only `QueryBus`.

For each query endpoint the controller shall:
1. Read `trace_id` from `@RequestAttribute(MdcConstants.TRACE_ID)`.
2. Map request parameters to a Query object.
3. Dispatch via `queryBus.handle(query)`.
4. Return `ResponseEntity<ApiResponse>` with `HttpStatus.OK` (200).

**Endpoints:**

| Method | Path | Query | Response DTO |
|--------|------|-------|-------------|
| `GET` | `/api/loan-proposals/{branchKey}/{id}` | `GetLoanProposalByIdQuery` | `LoanProposalResponse` |
| `GET` | `/api/loan-proposals/{branchKey}` | `ListLoanProposalsQuery` | `Page<LoanProposalListItem>` |
| `GET` | `/api/loan-proposals/v2/{branchKey}` | `SearchLoanProposalsV2Query` | `Page<LoanProposalListItem>` |
| `GET` | `/api/loan-proposals/scheme-details` | `GetSchemeDetailsQuery` | `SchemeDetailsResponse` |
| `GET` | `/api/loan-proposals/upg-tup/{branchKey}` | `GetUPGTUPExistingLoansQuery` | `UPGTUPExistingLoansResponse` |
| `GET` | `/api/loan-proposals/monitor` | `GetMonitoringFeedQuery` | `MonitoringFeedResponse` |

**Path variable and request param mapping:**

- `getById`: `{branchKey}` → `query.branchKey`; `{id}` → `query.id`
- `list`: `{branchKey}` → `query.branchKey`; `@RequestParam` for voId, memberId, loanProductId, schemeId, projectId, fromDate, toDate, statuses, proposalType, page, size
- `searchV2`: `{branchKey}` → `query.branchKey`; `@RequestParam` for searchTerm, voId, memberId, loanProductId, statuses, proposalType, fromDate, toDate, page, size
- `schemeDetails`: `@RequestParam` for memberId, loanProductId, schemeId, branchId, voId
- `upgTup`: `{branchKey}` → `query.branchKey`; `@RequestParam` for loanProductId
- `monitor`: `@RequestParam` for fromDateTime, toDateTime

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Proposal Retrieval", "Scheme/Reference Lookups", "Monitoring Feed"

---
## Read Infrastructure Layer

### DDD-REQ-Q022 — Read Repository: LoanProposalReadRepository

The Loan Proposal system shall implement `LoanProposalReadRepository` in
`infrastructure/readmodel/repository/`. It shall extend `MongoRepository<LoanProposalReadDocument, String>`
with additional `MongoTemplate`-based dynamic query methods via a custom repository interface.

**Static finder methods (derived from Spring Data conventions):**

| Method signature | Used by |
|-----------------|---------|
| `findByIdAndBranchCodeAndIsActive(String id, String branchCode, Boolean isActive)` → `Optional<LoanProposalReadDocument>` | `GetLoanProposalByIdQueryHandler` |
| `findByProposalNumber(String proposalNumber)` → `Optional<LoanProposalReadDocument>` | Internal uniqueness check |
| `findByCreatedAtBetween(LocalDateTime from, LocalDateTime to)` → `List<LoanProposalReadDocument>` | `GetMonitoringFeedQueryHandler` |

**Dynamic query methods (via `MongoTemplate` + custom repo impl):**

| Method signature | Used by |
|-----------------|---------|
| `findAll(Criteria spec, Pageable pageable)` → `Page<LoanProposalReadDocument>` | `ListLoanProposalsQueryHandler`, `SearchLoanProposalsV2QueryHandler` |
| `countByMemberIdAndSchemeIdAndActiveStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses)` → `Integer` | `GetSchemeDetailsQueryHandler` |
| `sumProposedLoanAmountByMemberIdAndSchemeIdAndActiveStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses)` → `BigDecimal` | `GetSchemeDetailsQueryHandler` |
| `countByBranchCodeAndLoanProductIdAndActiveStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses)` → `Integer` | `GetUPGTUPExistingLoansQueryHandler` |
| `sumProposedLoanAmountByBranchCodeAndLoanProductIdAndActiveStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses)` → `BigDecimal` | `GetUPGTUPExistingLoansQueryHandler` |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-Q023 — Read Persistence Configuration

The Loan Proposal system shall declare the `loan_proposal_read` collection and its indexes
within `infrastructure/readmodel/config/LoanProposalReadPersistenceConfiguration.java`:

| Index | Fields | Type | Supports |
|-------|--------|------|---------|
| Default `_id` | `_id` | Auto | getById |
| `idx_read_branch_datasource_status` | `branchCode ASC, dataSource ASC, loanProposalStatus ASC` | Compound | list, searchV2 |
| `idx_read_member_status` | `memberId ASC, loanProposalStatus ASC` | Compound | scheme details, UPG/TUP |
| `idx_read_created_at` | `createdAt DESC` | Single | date-range queries |
| `idx_read_proposal_number` | `proposalNumber ASC` | Unique sparse | business-key uniqueness |
| `idx_read_proposal_type` | `loanProposalType ASC` | Single | GOOD_LOAN filter |
| `idx_read_is_active` | `isActive ASC` | Single | exclude soft-deleted |

> 📎 [INFERRED] — required by bits.ddd pattern

---

### DDD-REQ-Q024 — Application Bootstrap

The Loan Proposal system shall define `LoanProposalQueryServiceApplication` as the Spring Boot
entry point for the query-side service in the root `application/` package.

Required annotations: `@SpringBootApplication`, `@EnableMongoRepositories`, `@EnableRabbit`

> 📎 [INFERRED] — required by bits.ddd pattern

---

## Cross-Cutting Requirements

The following requirements are copied from the source EARS and apply without modification to the
Loan Proposal query-side service.

### [UNCHANGED] Authentication and Authorisation

| # | EARS Requirement |
|---|-----------------|
| CCR-AUTH-001 | All Loan Proposal query API endpoints shall require a valid OAuth2 Bearer token. Requests without a valid token shall be rejected with HTTP 401 Unauthorized. |
| CCR-AUTH-002 | The Loan Proposal system shall enforce role-based access control via database-driven path-and-role ACL. Each query path shall be mapped to the set of roles permitted to invoke it. |
| CCR-AUTH-003 | The authenticated user's identity and role shall be made available to all query handlers for audit logging. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Authentication and Authorisation"

---

### [UNCHANGED] Request Handling

| # | EARS Requirement |
|---|-----------------|
| CCR-REQ-001 | Every inbound HTTP request shall be assigned a globally unique `trace_id` (UUID v4), propagated through MDC. |
| CCR-REQ-003 | The Loan Proposal system shall respond within the configured timeout for all synchronous operations. Operations that exceed the timeout shall return HTTP 504 Gateway Timeout. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Request Handling"

---

### [UNCHANGED] Operational Cross-Cuts

| # | EARS Requirement |
|---|-----------------|
| CCR-OPS-001 | Health-check endpoints for liveness and readiness probes (MongoDB, RabbitMQ). |
| CCR-OPS-002 | Micrometer `@Observed` annotations on all query handlers and projection handler. No Prometheus-specific annotations. |
| CCR-OPS-003 | Structured JSON logging at `INFO` level for all query dispatch and projection events, including `trace_id`, `query_type`, and `outcome`. |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Operational Cross-Cuts"

---

### [UNCHANGED] Error Response Format

Same as command side. See `LoanProposal-Command-DDD-EARS.md § Error Response Format`.

**Query-side HTTP status-code mapping:**

| Condition | HTTP Status |
|-----------|------------|
| Aggregate / read document not found | `404 Not Found` |
| Query validation failure | `422 Unprocessable Entity` |
| Authentication failure | `401 Unauthorized` |
| Authorisation failure | `403 Forbidden` |
| Monitoring feed window exceeds 24 hours | `422 Unprocessable Entity` |
| Request timeout | `504 Gateway Timeout` |
| Unhandled / infrastructure exception | `500 Internal Server Error` |

> 📎 Source: LoanProposalOTC-EARS-review-2-resolved.md § "Error Response Format"

---

## Out of Scope

| Item | Reason |
|------|--------|
| **Kafka-based read-model projection** | Downstream Kafka pipeline is disabled by default per EARS `[DISABLED]` marker. |
| **Speed Search (Elasticsearch) indexing** | External search-service concern; not owned by the Loan Proposal query service. |
| **Async pipeline queries (resyncById, resyncAll, bulkResync, replayFailed, dateRangeResync)** | Gate 1b decision: excluded from DDD scope. |
| **Buffer member/proposal traversal (maintenance endpoint)** | Deprecated. |
| **ERP Loan Proposal synchronisation queries** | Managed by ERP integration service outside this domain boundary. |
| **Data Sync Status read model** | Data-sync monitoring is an infrastructure concern; not a business domain query. |

---

## Open Questions

| # | Question | Source | Status |
|---|----------|--------|--------|
| OQ-Q001 | The `monitoringFeed` query uses `createdAt` for the date range. Confirm whether the intent is `createdAt` (system ingestion time) or `applicationDate` (business date). If `applicationDate`, the query and index must be adjusted. | `§ "Monitoring Feed"` | Open |
| OQ-Q002 | The `getById` enrichment for `loanAccountBalance`, `loanAccountScheduledAmount`, and `loanAccountOverdueAmount` requires a loan-account event projection. Confirm the exact queue from which loan-account financial snapshots are published and whether the query service should maintain its own loan-account snapshot or delegate to a dedicated loan-account read service. | Inferred from EARS disbursement section | Open |
| OQ-Q003 | The `enrollmentStatusOverride` for mobile-app source proposals references an override mechanism. Confirm the source of truth for this override (member service, enrollment service, or inline logic) and the specific conditions that trigger the override. | `§ "Proposal Retrieval — Retrieve by ID"` | Open |
| OQ-Q004 | The `getSchemeDetails` response includes `assetGrantPercentage` per VO category. Confirm whether this should be computed from the local `scheme_snapshot` collection or fetched from the scheme service via HTTP at query time. If HTTP, an HTTP client DDD-REQ will be required. | `§ "Scheme/Reference Lookups — Get Scheme Details"` | Open |
| OQ-Q005 | `memberName` appears in `LoanProposalListItem` but is not a field in the `LoanProposal` aggregate (which stores `memberId`). Confirm whether member name should be denormalised into the read model document at projection time (from `MemberSnapshotDocument`) or enriched at query time. | Inferred | Open |

---

*End of LoanProposal-Query-DDD-EARS.md*
