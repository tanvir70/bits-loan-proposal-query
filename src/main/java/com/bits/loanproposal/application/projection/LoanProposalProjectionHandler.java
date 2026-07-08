package com.bits.loanproposal.application.projection;

import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalEventPayload;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import com.bits.loanproposal.domain.enums.DomainStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Maintains the loan_proposal_read collection from command-side domain events (DDD-REQ-Q002).
 */
@Slf4j
@Component
public class LoanProposalProjectionHandler {

    private final LoanProposalReadRepository readRepository;
    private final InsuranceProductSnapshotRepository insuranceProductSnapshotRepository;

    public LoanProposalProjectionHandler(
            LoanProposalReadRepository readRepository,
            InsuranceProductSnapshotRepository insuranceProductSnapshotRepository) {
        this.readRepository = readRepository;
        this.insuranceProductSnapshotRepository = insuranceProductSnapshotRepository;
    }

    public void handle(LoanProposalCreatedEvent event) {
        LoanProposalReadDocument doc = LoanProposalReadMapper.toReadDocument(
                event,
                creditShieldExpiry(event),
                fireInsuranceExpiry(event),
                fireInsuranceProductName(event, null));
        doc.setIsActive(true);
        readRepository.save(doc);
        log.info("Projected LoanProposalCreatedEvent traceId={} id={}", event.getTraceId(), event.getId());
    }

    public void handle(LoanProposalUpdatedEvent event) {
        LoanProposalReadDocument existing = readRepository.findById(event.getId())
                .orElseGet(LoanProposalReadDocument::new);
        String existingFireInsuranceProductName = existing.getFireInsuranceProductName();
        LoanProposalReadDocument updated = LoanProposalReadMapper.mergeUpdatedFields(existing, event);
        updated.setCreditShieldExpiryDate(creditShieldExpiry(updated));
        updated.setFireInsuranceExpiryDate(fireInsuranceExpiry(updated));
        updated.setFireInsuranceProductName(
                fireInsuranceProductName(updated.getFireInsuranceProductId(), existingFireInsuranceProductName));
        if (updated.getIsActive() == null) {
            updated.setIsActive(true);
        }
        readRepository.save(updated);
        log.info("Projected LoanProposalUpdatedEvent traceId={} id={}", event.getTraceId(), event.getId());
    }

    public void handle(LoanProposalDeletedEvent event) {
        Optional<LoanProposalReadDocument> existing = readRepository.findById(event.getId());
        if (existing.isEmpty()) {
            return; // idempotent: skip if already gone
        }
        LoanProposalReadDocument doc = existing.get();
        doc.setIsActive(false);
        doc.setDomainStatus(DomainStatus.INACTIVE);
        doc.setLastModifiedAt(event.getDeletedAt());
        doc.setLastModifiedBy(event.getDeletedBy());
        readRepository.save(doc);
        log.info("Projected LoanProposalDeletedEvent traceId={} id={}", event.getTraceId(), event.getId());
    }

    private LocalDate creditShieldExpiry(LoanProposalEventPayload event) {
        if (Boolean.TRUE.equals(event.getMicroInsurance())
                && event.getApplicationDate() != null
                && event.getProposalDurationInMonths() != null) {
            return event.getApplicationDate().plusMonths(event.getProposalDurationInMonths());
        }
        return null;
    }

    private LocalDate fireInsuranceExpiry(LoanProposalEventPayload event) {
        if (Boolean.TRUE.equals(event.getWantsFireInsurance())
                && event.getApplicationDate() != null
                && event.getFireInsuranceDetails() != null
                && event.getFireInsuranceDetails().durationOfFireInsurance() != null) {
            return event.getApplicationDate().plusMonths(event.getFireInsuranceDetails().durationOfFireInsurance());
        }
        return null;
    }

    private String fireInsuranceProductName(LoanProposalEventPayload event, String fallback) {
        return fireInsuranceProductName(event.getFireInsuranceProductId(), fallback);
    }

    private LocalDate creditShieldExpiry(LoanProposalReadDocument doc) {
        if (Boolean.TRUE.equals(doc.getMicroInsurance())
                && doc.getApplicationDate() != null
                && doc.getProposalDurationInMonths() != null) {
            return doc.getApplicationDate().plusMonths(doc.getProposalDurationInMonths());
        }
        return null;
    }

    private LocalDate fireInsuranceExpiry(LoanProposalReadDocument doc) {
        if (Boolean.TRUE.equals(doc.getWantsFireInsurance())
                && doc.getApplicationDate() != null
                && doc.getFireInsuranceDetails() != null
                && doc.getFireInsuranceDetails().durationOfFireInsurance() != null) {
            return doc.getApplicationDate().plusMonths(doc.getFireInsuranceDetails().durationOfFireInsurance());
        }
        return null;
    }

    private String fireInsuranceProductName(Long fireInsuranceProductId, String fallback) {
        if (fireInsuranceProductId == null) {
            return fallback;
        }
        return insuranceProductSnapshotRepository.findById(fireInsuranceProductId)
                .map(InsuranceProductSnapshotDocument::getName)
                .orElse(fallback);
    }
}
