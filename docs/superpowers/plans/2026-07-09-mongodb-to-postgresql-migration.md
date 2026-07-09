# MongoDB → PostgreSQL Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all MongoDB persistence with PostgreSQL/JPA while keeping the query-side API contract, controller behavior, and test assertions unchanged.

**Architecture:** The `LoanProposalReadDocument` becomes a JPA `@Entity` mapped to a PostgreSQL table. The MongoDB `Criteria`-based dynamic filter builder (`LoanProposalFilterCriteria`) is rewritten as a JPA `Specification<T>` builder. Snapshot documents become JPA entities with their own tables. Complex embedded value objects (nominees, guarantors, etc.) are stored as JSONB columns using JPA `AttributeConverter`. The `bits-ddd-lib:infra` module transitively brings `spring-boot-starter-data-mongodb` — we exclude its auto-configuration but keep the library on the classpath (other infra beans like `TracerIdFilter`, `GlobalExceptionHandler`, `RabbitMQEventProcessWrapper` must still load).

**Tech Stack:** Java 25, Spring Boot 4.0.1, Spring Data JPA, Hibernate, PostgreSQL 16, bits-ddd-lib 1.2.1

## Global Constraints

- bits-ddd-lib version is 1.2.1 — do NOT modify the library itself.
- In any conflict between EARS and the library, always align with the library's patterns.
- Standard REST Controller endpoints must wrap payloads inside the generic `ApiResponse` envelope.
- The domain layer (enums, value objects, entities) must NOT be modified — persistence annotations go only on infrastructure-layer classes.
- All existing unit tests must continue to pass after migration (with import/type adjustments only).
- PostgreSQL connection: `jdbc:postgresql://localhost:5432/loan_proposal_query`, user `postgres`, password `root`.

---

### Task 1: Build Configuration & Auto-Configuration Exclusion

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/java/com/bits/loanproposal/LoanProposalQueryServiceApplication.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Consumes: Nothing (foundation task)
- Produces: Working build with JPA + PostgreSQL driver on classpath, MongoDB auto-config excluded, JPA auto-config active

- [ ] **Step 1: Update `build.gradle` — add JPA and PostgreSQL dependencies**

```gradle
dependencies {
    implementation 'com.bits.ddd:query-core'
    implementation 'com.bits.ddd:shared'
    implementation 'com.bits.ddd:application'
    implementation 'com.bits.ddd:infra'
    implementation 'com.bits.ddd:annotation'
    annotationProcessor 'com.bits.ddd:annotation-processor'
    annotationProcessor 'com.bits.ddd:annotation'

    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Swagger / OpenAPI
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

- [ ] **Step 2: Update `LoanProposalQueryServiceApplication.java` — exclude MongoDB auto-configs, enable JPA repos**

Replace the entire file content:

```java
package com.bits.loanproposal;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class
})
@EnableJpaRepositories
@EnableRabbit
public class LoanProposalQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanProposalQueryServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Update `application.yml` — replace MongoDB URI with PostgreSQL datasource + JPA config**

Replace the entire file content:

```yaml
spring:
  application:
    name: loan-proposal-query
  main:
    allow-bean-definition-overriding: true
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  datasource:
    url: jdbc:postgresql://localhost:5432/loan_proposal_query
    username: postgres
    password: root
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    open-in-view: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
      - org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration
      - com.bits.ddd.infra.autoconfigure.InfraMongoAutoConfiguration

server:
  port: 8082
```

- [ ] **Step 4: Verify the project compiles (expect failures in MongoDB-dependent classes)**

Run: `./gradlew compileJava 2>&1 | head -60`
Expected: Compilation errors in files that still import MongoDB types (this is fine — we fix them in subsequent tasks).

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main/java/com/bits/loanproposal/LoanProposalQueryServiceApplication.java src/main/resources/application.yml
git commit -m "chore: add JPA/PostgreSQL deps, exclude MongoDB auto-configs"
```

---

### Task 2: JPA AttributeConverters for Custom Types

**Files:**
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/LoanProposalStatusConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/DomainStatusConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/JsonAttributeConverter.java`

**Interfaces:**
- Consumes: `LoanProposalStatus.of(String)`, `DomainStatus.of(String)`, `DomainStatus.code()`, `LoanProposalStatus.code()`
- Produces: JPA `AttributeConverter` beans used by `@Convert` annotations in Task 3

- [ ] **Step 1: Create `JsonAttributeConverter.java` — generic JSONB converter**

This reusable converter serializes/deserializes any Java type to/from PostgreSQL JSONB using Jackson.

```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;

public abstract class JsonAttributeConverter<T> implements AttributeConverter<T, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final TypeReference<T> typeRef;

    protected JsonAttributeConverter(TypeReference<T> typeRef) {
        this.typeRef = typeRef;
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error serializing to JSON", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error deserializing from JSON", e);
        }
    }
}
```

- [ ] **Step 2: Create `LoanProposalStatusConverter.java`**

```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoanProposalStatusConverter implements AttributeConverter<LoanProposalStatus, String> {

    @Override
    public String convertToDatabaseColumn(LoanProposalStatus status) {
        return status != null ? status.code() : null;
    }

    @Override
    public LoanProposalStatus convertToEntityAttribute(String code) {
        return code != null ? LoanProposalStatus.of(code) : null;
    }
}
```

- [ ] **Step 3: Create `DomainStatusConverter.java`**

```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.ddd.shared.domain.value.DomainStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DomainStatusConverter implements AttributeConverter<DomainStatus, String> {

    @Override
    public String convertToDatabaseColumn(DomainStatus status) {
        return status != null ? status.code() : null;
    }

    @Override
    public DomainStatus convertToEntityAttribute(String code) {
        return code != null ? DomainStatus.of(code) : null;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/
git commit -m "feat: add JPA AttributeConverters for DomainStatus, LoanProposalStatus, and JSON types"
```

---

### Task 3: Convert Documents to JPA Entities

**Files:**
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/document/LoanProposalReadDocument.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/SchemeSnapshotDocument.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/MemberSnapshotDocument.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/VillageOrganisationSnapshotDocument.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/InsuranceProductSnapshotDocument.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/NomineeListConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/GuarantorListConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/StringListConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/SecondInsurerConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/GuardianConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/CoBorrowerConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/FireInsuranceDetailsConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/OtcModeOfPaymentConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/AutoDebitCollectionConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/ProgotiDocumentChecklistConverter.java`
- Create: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/converter/AssetGrantPercentageMapConverter.java`

**Interfaces:**
- Consumes: `JsonAttributeConverter<T>` from Task 2, `LoanProposalStatusConverter`, `DomainStatusConverter`
- Produces: JPA `@Entity`-annotated classes ready for Hibernate schema generation

- [ ] **Step 1: Create all JSONB converters for complex embedded types**

Each extends `JsonAttributeConverter<T>` with the appropriate `TypeReference`.

`NomineeListConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.entity.Nominee;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class NomineeListConverter extends JsonAttributeConverter<List<Nominee>> {
    public NomineeListConverter() {
        super(new TypeReference<>() {});
    }
}
```

`GuarantorListConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.entity.Guarantor;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class GuarantorListConverter extends JsonAttributeConverter<List<Guarantor>> {
    public GuarantorListConverter() {
        super(new TypeReference<>() {});
    }
}
```

`StringListConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class StringListConverter extends JsonAttributeConverter<List<String>> {
    public StringListConverter() {
        super(new TypeReference<>() {});
    }
}
```

`SecondInsurerConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.entity.SecondInsurer;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class SecondInsurerConverter extends JsonAttributeConverter<SecondInsurer> {
    public SecondInsurerConverter() {
        super(new TypeReference<>() {});
    }
}
```

`GuardianConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.entity.Guardian;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class GuardianConverter extends JsonAttributeConverter<Guardian> {
    public GuardianConverter() {
        super(new TypeReference<>() {});
    }
}
```

`CoBorrowerConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.entity.CoBorrower;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class CoBorrowerConverter extends JsonAttributeConverter<CoBorrower> {
    public CoBorrowerConverter() {
        super(new TypeReference<>() {});
    }
}
```

`FireInsuranceDetailsConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.valueobject.FireInsuranceDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class FireInsuranceDetailsConverter extends JsonAttributeConverter<FireInsuranceDetails> {
    public FireInsuranceDetailsConverter() {
        super(new TypeReference<>() {});
    }
}
```

`OtcModeOfPaymentConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.valueobject.OtcModeOfPayment;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class OtcModeOfPaymentConverter extends JsonAttributeConverter<OtcModeOfPayment> {
    public OtcModeOfPaymentConverter() {
        super(new TypeReference<>() {});
    }
}
```

`AutoDebitCollectionConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.valueobject.AutoDebitCollection;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class AutoDebitCollectionConverter extends JsonAttributeConverter<AutoDebitCollection> {
    public AutoDebitCollectionConverter() {
        super(new TypeReference<>() {});
    }
}
```

`ProgotiDocumentChecklistConverter.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.bits.loanproposal.domain.valueobject.ProgotiDocumentChecklist;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

@Converter
public class ProgotiDocumentChecklistConverter extends JsonAttributeConverter<ProgotiDocumentChecklist> {
    public ProgotiDocumentChecklistConverter() {
        super(new TypeReference<>() {});
    }
}
```

`AssetGrantPercentageMapConverter.java` (for `SchemeSnapshotDocument`):
```java
package com.bits.loanproposal.infrastructure.readmodel.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.util.Map;

@Converter
public class AssetGrantPercentageMapConverter extends JsonAttributeConverter<Map<String, BigDecimal>> {
    public AssetGrantPercentageMapConverter() {
        super(new TypeReference<>() {});
    }
}
```

- [ ] **Step 2: Convert `LoanProposalReadDocument` from `@Document` to `@Entity`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.document;

import com.bits.loanproposal.domain.entity.*;
import com.bits.loanproposal.domain.enums.*;
import com.bits.loanproposal.domain.valueobject.*;
import com.bits.loanproposal.infrastructure.readmodel.converter.*;
import com.bits.ddd.shared.domain.value.DomainStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Entity
@Table(name = "loan_proposal_read", indexes = {
        @Index(name = "idx_read_branch_datasource_status", columnList = "branchCode, dataSource, loanProposalStatus"),
        @Index(name = "idx_read_member_status", columnList = "memberId, loanProposalStatus"),
        @Index(name = "idx_read_created_at", columnList = "createdAt"),
        @Index(name = "idx_read_proposal_number", columnList = "proposalNumber", unique = true),
        @Index(name = "idx_read_proposal_type", columnList = "loanProposalType"),
        @Index(name = "idx_read_is_active", columnList = "isActive")
})
public class LoanProposalReadDocument {

    @Id
    private String id;
    private Long loanProposalId;

    @Column(unique = true)
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

    @Convert(converter = LoanProposalStatusConverter.class)
    private LoanProposalStatus loanProposalStatus;

    @Enumerated(EnumType.STRING)
    private LoanProposalType loanProposalType;

    private String approvalFlowStatus;
    private String approvalStatus;

    @Enumerated(EnumType.STRING)
    private ApiDataSource dataSource;

    @Convert(converter = DomainStatusConverter.class)
    private DomainStatus domainStatus;

    private Boolean microInsurance;
    private Long policyTypeId;
    private Long insuranceProductId;
    private BigDecimal premiumAmount;

    @Convert(converter = SecondInsurerConverter.class)
    @Column(columnDefinition = "TEXT")
    private SecondInsurer secondInsurer;

    private Boolean wantsFireInsurance;
    private Long fireInsuranceProductId;

    @Convert(converter = FireInsuranceDetailsConverter.class)
    @Column(columnDefinition = "TEXT")
    private FireInsuranceDetails fireInsuranceDetails;

    @Convert(converter = OtcModeOfPaymentConverter.class)
    @Column(columnDefinition = "TEXT")
    private OtcModeOfPayment modeOfPayment;

    @Convert(converter = AutoDebitCollectionConverter.class)
    @Column(columnDefinition = "TEXT")
    private AutoDebitCollection autoDebitCollection;

    private Boolean isDigitalDisbursement;
    private String transactionDescription;

    @Convert(converter = NomineeListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Nominee> nominees;

    @Convert(converter = GuardianConverter.class)
    @Column(columnDefinition = "TEXT")
    private Guardian guardian;

    @Convert(converter = CoBorrowerConverter.class)
    @Column(columnDefinition = "TEXT")
    private CoBorrower coBorrower;

    @Convert(converter = GuarantorListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Guarantor> guarantors;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> specialSavingsAccountIds;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
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

    @Convert(converter = ProgotiDocumentChecklistConverter.class)
    @Column(columnDefinition = "TEXT")
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
```

- [ ] **Step 3: Convert `SchemeSnapshotDocument` from `@Document` to `@Entity`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import com.bits.loanproposal.infrastructure.readmodel.converter.AssetGrantPercentageMapConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scheme_snapshot")
public class SchemeSnapshotDocument {
    @Id
    private Long schemeId;
    private String name;

    @Convert(converter = AssetGrantPercentageMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, BigDecimal> assetGrantPercentageByVoCategory;

    public BigDecimal assetGrantPercentageForVoCategory(String voCategory) {
        if (voCategory == null || assetGrantPercentageByVoCategory == null) {
            return BigDecimal.ZERO;
        }
        return assetGrantPercentageByVoCategory.getOrDefault(voCategory, BigDecimal.ZERO);
    }
}
```

- [ ] **Step 4: Convert `MemberSnapshotDocument` from `@Document` to `@Entity`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ponytail: slim read-only view of member_snapshot — only fields the query side needs
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "member_snapshot")
public class MemberSnapshotDocument {
    @Id
    private Long memberId;
    private String status;
    private Long voId;
}
```

- [ ] **Step 5: Convert `VillageOrganisationSnapshotDocument` from `@Document` to `@Entity`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "village_organisation_snapshot")
public class VillageOrganisationSnapshotDocument {
    @Id
    private Long voId;
    private String category;
}
```

- [ ] **Step 6: Convert `InsuranceProductSnapshotDocument` from `@Document` to `@Entity`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "insurance_product_snapshot")
public class InsuranceProductSnapshotDocument {
    @Id
    private Long insuranceProductId;
    private String name;
}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/bits/loanproposal/infrastructure/readmodel/
git commit -m "feat: convert all documents to JPA entities with JSONB converters"
```

---

### Task 4: Migrate Repositories from MongoRepository to JpaRepository

**Files:**
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/repository/LoanProposalReadRepository.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/repository/LoanProposalReadRepositoryCustom.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/repository/LoanProposalReadRepositoryImpl.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/SchemeSnapshotRepository.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/MemberSnapshotRepository.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/VillageOrganisationSnapshotRepository.java`
- Modify: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/snapshot/InsuranceProductSnapshotRepository.java`

**Interfaces:**
- Consumes: JPA entities from Task 3
- Produces: `LoanProposalReadRepository` with `findAll(Specification<LoanProposalReadDocument>, Pageable)` via `JpaSpecificationExecutor`, all snapshot repos as `JpaRepository`

- [ ] **Step 1: Update `LoanProposalReadRepositoryCustom` — remove MongoDB `Criteria` from the interface**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;

import java.math.BigDecimal;
import java.util.List;

public interface LoanProposalReadRepositoryCustom {

    long countByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses);

    BigDecimal sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses);

    long countByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses);

    BigDecimal sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses);
}
```

Note: The `findAll(Criteria, Pageable)` method is removed. The query handlers will now use `JpaSpecificationExecutor.findAll(Specification, Pageable)` which comes from the repository extending `JpaSpecificationExecutor`.

- [ ] **Step 2: Update `LoanProposalReadRepository` — switch from `MongoRepository` to `JpaRepository` + `JpaSpecificationExecutor`**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanProposalReadRepository
        extends JpaRepository<LoanProposalReadDocument, String>,
                JpaSpecificationExecutor<LoanProposalReadDocument>,
                LoanProposalReadRepositoryCustom {

    Optional<LoanProposalReadDocument> findByIdAndBranchCodeAndIsActive(String id, String branchCode, Boolean isActive);

    Optional<LoanProposalReadDocument> findByProposalNumber(String proposalNumber);

    List<LoanProposalReadDocument> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
```

- [ ] **Step 3: Rewrite `LoanProposalReadRepositoryImpl` — replace `MongoTemplate` with JPA `EntityManager` + JPQL**

Replace the entire file content:

```java
package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class LoanProposalReadRepositoryImpl implements LoanProposalReadRepositoryCustom {

    private final EntityManager entityManager;

    public LoanProposalReadRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public long countByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses) {
        List<String> statusCodes = statuses.stream().map(LoanProposalStatus::code).collect(Collectors.toList());
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(d) FROM LoanProposalReadDocument d " +
                "WHERE d.memberId = :memberId AND d.schemeId = :schemeId " +
                "AND d.loanProposalStatus IN :statuses AND d.isActive = true", Long.class);
        query.setParameter("memberId", memberId);
        query.setParameter("schemeId", schemeId);
        query.setParameter("statuses", statuses);
        return query.getSingleResult();
    }

    @Override
    public BigDecimal sumProposedLoanAmountByMemberIdAndSchemeIdAndStatuses(Long memberId, Long schemeId, List<LoanProposalStatus> statuses) {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(d.proposedLoanAmount), 0) FROM LoanProposalReadDocument d " +
                "WHERE d.memberId = :memberId AND d.schemeId = :schemeId " +
                "AND d.loanProposalStatus IN :statuses AND d.isActive = true", BigDecimal.class);
        query.setParameter("memberId", memberId);
        query.setParameter("schemeId", schemeId);
        query.setParameter("statuses", statuses);
        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(d) FROM LoanProposalReadDocument d " +
                "WHERE d.branchCode = :branchCode AND d.loanProductId = :loanProductId " +
                "AND d.loanProposalStatus IN :statuses AND d.isActive = true", Long.class);
        query.setParameter("branchCode", branchCode);
        query.setParameter("loanProductId", loanProductId);
        query.setParameter("statuses", statuses);
        return query.getSingleResult();
    }

    @Override
    public BigDecimal sumProposedLoanAmountByBranchCodeAndLoanProductIdAndStatuses(String branchCode, Long loanProductId, List<LoanProposalStatus> statuses) {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(d.proposedLoanAmount), 0) FROM LoanProposalReadDocument d " +
                "WHERE d.branchCode = :branchCode AND d.loanProductId = :loanProductId " +
                "AND d.loanProposalStatus IN :statuses AND d.isActive = true", BigDecimal.class);
        query.setParameter("branchCode", branchCode);
        query.setParameter("loanProductId", loanProductId);
        query.setParameter("statuses", statuses);
        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}
```

- [ ] **Step 4: Update all snapshot repositories from `MongoRepository` to `JpaRepository`**

`SchemeSnapshotRepository.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemeSnapshotRepository extends JpaRepository<SchemeSnapshotDocument, Long> {
}
```

`MemberSnapshotRepository.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSnapshotRepository extends JpaRepository<MemberSnapshotDocument, Long> {
}
```

`VillageOrganisationSnapshotRepository.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VillageOrganisationSnapshotRepository extends JpaRepository<VillageOrganisationSnapshotDocument, Long> {
}
```

`InsuranceProductSnapshotRepository.java`:
```java
package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProductSnapshotRepository extends JpaRepository<InsuranceProductSnapshotDocument, Long> {
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bits/loanproposal/infrastructure/readmodel/
git commit -m "feat: migrate repositories from MongoRepository to JpaRepository"
```

---

### Task 5: Rewrite Filter Criteria Builder & Query Handlers (MongoDB Criteria → JPA Specification)

**Files:**
- Modify: `src/main/java/com/bits/loanproposal/application/queryhandler/LoanProposalFilterCriteria.java`
- Modify: `src/main/java/com/bits/loanproposal/application/queryhandler/ListLoanProposalsQueryHandler.java`
- Modify: `src/main/java/com/bits/loanproposal/application/queryhandler/SearchLoanProposalsV2QueryHandler.java`

**Interfaces:**
- Consumes: `LoanProposalReadRepository.findAll(Specification<LoanProposalReadDocument>, Pageable)` from Task 4
- Produces: Functionally equivalent query handlers that build JPA `Specification` objects instead of MongoDB `Criteria`

- [ ] **Step 1: Rewrite `LoanProposalFilterCriteria` — replace MongoDB `Criteria` with JPA `Specification`**

Replace the entire file content:

```java
package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared filter building for list and searchV2 (DDD-REQ-Q006 / Q008).
 * Fixed filters: dataSource=OTC, exclude GOOD_LOAN, isActive=true,
 * default status exclusions unless statuses explicitly supplied.
 */
final class LoanProposalFilterCriteria {

    private static final List<LoanProposalStatus> DEFAULT_EXCLUDED_STATUSES =
            List.of(LoanProposalStatus.BM_APPROVAL_PENDING, LoanProposalStatus.DRAFT, LoanProposalStatus.BM_REJECT);

    private final List<Specification<LoanProposalReadDocument>> specs = new ArrayList<>();

    private LoanProposalFilterCriteria(String branchKey, List<LoanProposalStatus> statuses, LoanProposalType proposalType) {
        specs.add((root, query, cb) -> cb.equal(root.get("branchCode"), branchKey));
        specs.add((root, query, cb) -> cb.equal(root.get("dataSource"), ApiDataSource.OTC));
        specs.add((root, query, cb) -> cb.equal(root.get("isActive"), true));

        if (proposalType != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("loanProposalType"), proposalType));
        } else {
            specs.add((root, query, cb) -> cb.notEqual(root.get("loanProposalType"), LoanProposalType.GOOD_LOAN));
        }

        if (statuses == null || statuses.isEmpty()) {
            specs.add((root, query, cb) -> cb.not(root.get("loanProposalStatus").in(DEFAULT_EXCLUDED_STATUSES)));
        } else {
            specs.add((root, query, cb) -> root.get("loanProposalStatus").in(statuses));
        }
    }

    static LoanProposalFilterCriteria of(String branchKey, List<LoanProposalStatus> statuses, LoanProposalType proposalType) {
        return new LoanProposalFilterCriteria(branchKey, statuses, proposalType);
    }

    LoanProposalFilterCriteria eq(String field, Object value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    LoanProposalFilterCriteria applicationDateBetween(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("applicationDate"), fromDate));
        }
        if (toDate != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("applicationDate"), toDate));
        }
        return this;
    }

    LoanProposalFilterCriteria searchTerm(String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("proposalNumber")), pattern),
                    cb.like(cb.lower(root.get("memberName")), pattern)
            ));
        }
        return this;
    }

    Specification<LoanProposalReadDocument> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<LoanProposalReadDocument> spec : specs) {
                predicates.add(spec.toPredicate(root, query, cb));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

- [ ] **Step 2: Update `ListLoanProposalsQueryHandler` — use `Specification` instead of `Criteria`**

Replace the entire file content:

```java
package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.ListLoanProposalsQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<LoanProposalReadDocument> spec = LoanProposalFilterCriteria
                .of(query.branchKey(), query.statuses(), query.proposalType())
                .eq("villageOrganisationId", query.voId())
                .eq("memberId", query.memberId())
                .eq("loanProductId", query.loanProductId())
                .eq("schemeId", query.schemeId())
                .eq("projectId", query.projectId())
                .applicationDateBetween(query.fromDate(), query.toDate())
                .build();

        PageRequest pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        return readRepository.findAll(spec, pageable).map(LoanProposalReadMapper::toListItem);
    }
}
```

- [ ] **Step 3: Update `SearchLoanProposalsV2QueryHandler` — use `Specification` instead of `Criteria`**

Replace the entire file content:

```java
package com.bits.loanproposal.application.queryhandler;

import com.bits.ddd.annotation.RegisterQueryHandler;
import com.bits.ddd.handler.QueryHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.query.SearchLoanProposalsV2Query;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RegisterQueryHandler
public class SearchLoanProposalsV2QueryHandler
        implements QueryHandler<SearchLoanProposalsV2Query, Page<LoanProposalListItem>> {

    private final LoanProposalReadRepository readRepository;

    public SearchLoanProposalsV2QueryHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public Page<LoanProposalListItem> handle(SearchLoanProposalsV2Query query) {
        Specification<LoanProposalReadDocument> spec = LoanProposalFilterCriteria
                .of(query.branchKey(), query.statuses(), query.proposalType())
                .searchTerm(query.searchTerm())
                .eq("villageOrganisationId", query.voId())
                .eq("memberId", query.memberId())
                .eq("loanProductId", query.loanProductId())
                .applicationDateBetween(query.fromDate(), query.toDate())
                .build();

        PageRequest pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        return readRepository.findAll(spec, pageable).map(LoanProposalReadMapper::toListItem);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/bits/loanproposal/application/queryhandler/
git commit -m "feat: rewrite filter criteria and query handlers from MongoDB Criteria to JPA Specification"
```

---

### Task 6: Delete MongoDB Configuration & Update Tests

**Files:**
- Delete: `src/main/java/com/bits/loanproposal/infrastructure/readmodel/config/LoanProposalReadPersistenceConfiguration.java`
- Modify: `src/test/java/com/bits/loanproposal/application/queryhandler/ListLoanProposalsQueryHandlerTest.java`
- Modify: `src/test/java/com/bits/loanproposal/application/queryhandler/SearchLoanProposalsV2QueryHandlerTest.java`

**Interfaces:**
- Consumes: All JPA-based types from Tasks 3–5
- Produces: Clean compilation, all tests passing

- [ ] **Step 1: Delete `LoanProposalReadPersistenceConfiguration.java`**

```bash
rm src/main/java/com/bits/loanproposal/infrastructure/readmodel/config/LoanProposalReadPersistenceConfiguration.java
```

This file registered MongoDB custom conversions and programmatic indexes — both replaced by JPA `@Convert` annotations (Task 3) and `@Index` on the `@Table` annotation.

- [ ] **Step 2: Update `ListLoanProposalsQueryHandlerTest` — replace `Criteria` with `Specification`**

Replace the entire file content:

```java
package com.bits.loanproposal.application.queryhandler;

import com.bits.loanproposal.application.query.ListLoanProposalsQuery;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListLoanProposalsQueryHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @InjectMocks
    private ListLoanProposalsQueryHandler handler;

    @Test
    void handleFetchesPageOfListItems() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");
        doc.setProposalNumber("LP-123");

        Page<LoanProposalReadDocument> page = new PageImpl<>(List.of(doc));
        when(readRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ListLoanProposalsQuery query = new ListLoanProposalsQuery(
                "trace-1", "0010", null, null, null, null, null, null, null, null, null, 0, 10);

        Page<LoanProposalListItem> result = handler.handle(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("proposal-1");
    }

    @Test
    void handleFetchesPageOfListItemsWithOptionalFilters() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-2");
        doc.setProposalNumber("LP-456");

        Page<LoanProposalReadDocument> page = new PageImpl<>(List.of(doc));
        when(readRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ListLoanProposalsQuery query = new ListLoanProposalsQuery(
                "trace-1",
                "0010",
                10L,
                20L,
                30L,
                40L,
                50L,
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                List.of(LoanProposalStatus.PENDING),
                LoanProposalType.NORMAL_LOAN,
                0,
                10
        );

        Page<LoanProposalListItem> result = handler.handle(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("proposal-2");
    }
}
```

- [ ] **Step 3: Update `SearchLoanProposalsV2QueryHandlerTest` — replace `Criteria` with `Specification`**

View the existing test to understand its structure, then replace MongoDB `Criteria` matchers with `Specification` matchers. The pattern is identical to `ListLoanProposalsQueryHandlerTest`: change `any(Criteria.class)` to `any(Specification.class)` and the import from `org.springframework.data.mongodb.core.query.Criteria` to `org.springframework.data.jpa.domain.Specification`.

- [ ] **Step 4: Run full test suite**

Run: `./gradlew test`
Expected: All tests pass. No MongoDB-related import errors.

- [ ] **Step 5: Commit**

```bash
git rm src/main/java/com/bits/loanproposal/infrastructure/readmodel/config/LoanProposalReadPersistenceConfiguration.java
git add src/test/
git commit -m "chore: remove MongoDB config, update tests for JPA Specification"
```

---

### Task 7: Verify Full Compilation, Tests, and Application Boot

**Files:**
- No new files

**Interfaces:**
- Consumes: Everything from Tasks 1–6
- Produces: Verified clean build, all tests passing, application starts and connects to PostgreSQL

- [ ] **Step 1: Run full compilation**

Run: `./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL with zero errors

- [ ] **Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests pass

- [ ] **Step 3: Verify application boots**

Run: `./gradlew bootRun &` (wait 15 seconds), then:
```bash
curl -s http://localhost:8082/actuator/health | head -5
```
Expected: `{"status":"UP"}` — confirms JPA/PostgreSQL connection is established.

Kill the server after verifying.

- [ ] **Step 4: Verify PostgreSQL tables were auto-created**

```bash
PGPASSWORD=root psql -h localhost -U postgres -d loan_proposal_query -c "\dt"
```
Expected output should list tables: `loan_proposal_read`, `scheme_snapshot`, `member_snapshot`, `village_organisation_snapshot`, `insurance_product_snapshot`.

- [ ] **Step 5: Verify API endpoint**

```bash
curl -s http://localhost:8082/api/v1/loan-proposals/LP-001?branchKey=0010 | head -20
```
Expected: API response (even if data is empty, the endpoint should return a valid JSON response).

- [ ] **Step 6: Final commit (if any remaining files need staging)**

```bash
git add -A
git commit -m "chore: verify MongoDB-to-PostgreSQL migration complete"
```
