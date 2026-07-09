package com.bits.loanproposal.application.projection;

import com.bits.ddd.shared.util.JsonUtil;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import com.bits.loanproposal.domain.enums.ApiDataSource;
import com.bits.ddd.shared.domain.value.DomainStatus;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoanProposalProjectionHandlerTest {

    @Mock
    private LoanProposalReadRepository readRepository;

    @Mock
    private InsuranceProductSnapshotRepository insuranceProductSnapshotRepository;

    @InjectMocks
    private LoanProposalProjectionHandler projectionHandler;

    @Test
    void onLoanProposalUpdatedMergesCommandPayloadAndRecomputesDerivedFields() {
        LoanProposalReadDocument existing = existingDocument();
        InsuranceProductSnapshotDocument product = new InsuranceProductSnapshotDocument();
        product.setInsuranceProductId(77L);
        product.setName("Updated Fire Product");

        when(readRepository.findById("proposal-1")).thenReturn(Optional.of(existing));
        when(insuranceProductSnapshotRepository.findById(77L)).thenReturn(Optional.of(product));

        projectionHandler.handle(commandUpdateEvent());

        ArgumentCaptor<LoanProposalReadDocument> savedCaptor =
                ArgumentCaptor.forClass(LoanProposalReadDocument.class);
        verify(readRepository).save(savedCaptor.capture());
        LoanProposalReadDocument saved = savedCaptor.getValue();

        assertThat(saved.getProposalNumber()).isEqualTo("LP-UPDATED");
        assertThat(saved.getProposedLoanAmount()).isEqualByComparingTo("50000.00");
        assertThat(saved.getApprovedLoanAmount()).isEqualByComparingTo("45000.00");
        assertThat(saved.getLoanProposalType()).isEqualTo(LoanProposalType.NORMAL_LOAN);
        assertThat(saved.getLoanProposalStatus()).isEqualTo(LoanProposalStatus.PENDING);
        assertThat(saved.getDataSource()).isEqualTo(ApiDataSource.OTC);
        assertThat(saved.getDomainStatus()).isEqualTo(DomainStatus.UPDATED);
        assertThat(saved.getApplicationDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(saved.getCreditShieldExpiryDate()).isEqualTo(LocalDate.of(2027, 1, 15));
        assertThat(saved.getFireInsuranceExpiryDate()).isEqualTo(LocalDate.of(2028, 1, 15));
        assertThat(saved.getFireInsuranceProductName()).isEqualTo("Updated Fire Product");

        assertThat(saved.getMemberName()).isEqualTo("Existing Member");
        assertThat(saved.getMemberMobileNumber()).isEqualTo("01700000000");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
        assertThat(saved.getCreatedBy()).isEqualTo("creator");
        assertThat(saved.getLoanAccountBalance()).isEqualByComparingTo("1000.00");
        assertThat(saved.getEnrollmentStatusOverride()).isEqualTo("ENROLLED");
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    void onLoanProposalUpdatedKeepsExistingProductNameWhenSnapshotIsMissing() {
        LoanProposalReadDocument existing = existingDocument();
        when(readRepository.findById("proposal-1")).thenReturn(Optional.of(existing));
        when(insuranceProductSnapshotRepository.findById(77L)).thenReturn(Optional.empty());

        projectionHandler.handle(commandUpdateEvent());

        ArgumentCaptor<LoanProposalReadDocument> savedCaptor =
                ArgumentCaptor.forClass(LoanProposalReadDocument.class);
        verify(readRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getFireInsuranceProductName()).isEqualTo("Existing Fire Product");
    }

    private LoanProposalReadDocument existingDocument() {
        LoanProposalReadDocument doc = new LoanProposalReadDocument();
        doc.setId("proposal-1");
        doc.setMemberName("Existing Member");
        doc.setMemberMobileNumber("01700000000");
        doc.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        doc.setCreatedBy("creator");
        doc.setIsActive(true);
        doc.setMicroInsurance(true);
        doc.setWantsFireInsurance(true);
        doc.setFireInsuranceProductId(77L);
        doc.setFireInsuranceProductName("Existing Fire Product");
        doc.setLoanAccountBalance(new BigDecimal("1000.00"));
        doc.setEnrollmentStatusOverride("ENROLLED");
        return doc;
    }

    private LoanProposalUpdatedEvent commandUpdateEvent() {
        return JsonUtil.deserialize(commandUpdateJson().getBytes(), LoanProposalUpdatedEvent.class);
    }

    public static String commandUpdateJson() {
        return """
                {
                  "eventIdentifier": "event-1",
                  "eventType": "LoanProposalUpdatedEvent",
                  "tracerId": "trace-from-bits",
                  "timestamp": "2026-01-15T10:00:00Z",
                  "aggregateIdentifier": "proposal-1",
                  "aggregateType": "LoanProposal",
                  "version": 2,
                  "id": "proposal-1",
                  "loanProposalId": 1001,
                  "proposalNumber": "LP-UPDATED",
                  "branchId": 10,
                  "branchCode": "0010",
                  "projectId": 20,
                  "memberId": 30,
                  "loanProductId": 40,
                  "loanProductDetailsId": 41,
                  "loanProductPolicyId": 42,
                  "schemeId": 50,
                  "frequencyId": 60,
                  "proposedLoanAmount": 50000.00,
                  "approvedLoanAmount": 45000.00,
                  "proposedGrantAmount": 500.00,
                  "approvedGrantAmount": 400.00,
                  "installmentAmount": 2500.00,
                  "proposalDurationInMonths": 12,
                  "loanProposalType": "NORMAL_LOAN",
                  "loanProposalStatus": "PENDING",
                  "dataSource": "OTC",
                  "domainStatus": "UPDATED",
                  "isDigitalDisbursement": false,
                  "nominees": [
                    {
                      "id": "nominee-1",
                      "name": "Updated Nominee",
                      "relationshipId": 5,
                      "sharePercentage": 100.0
                    }
                  ],
                  "fireInsuranceDetails": {
                    "businessName": "Business",
                    "businessAddress": "Address",
                    "businessPhone": "Phone",
                    "businessEmail": "email@example.com",
                    "divisionId": 1,
                    "districtId": 2,
                    "thanaId": 3,
                    "businessTypeId": 4,
                    "constructionOfPremisesId": 5,
                    "fireInsurancePremiumAmount": 10.00,
                    "fireInsuranceInsuredAmount": 1000.00,
                    "durationOfFireInsurance": 24,
                    "fireInsuranceProductName": "Payload Product",
                    "bracCommissionAmount": 1.00,
                    "memberCommissionAmount": 2.00
                  },
                  "modeOfPayment": null,
                  "applicationDate": "2026-01-15",
                  "proposalReferenceNumber": "REF-UPDATED",
                  "traceId": "trace-1"
                }
                """;
    }
}
