package com.bits.loanproposal.application.projection;

import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalEventPayload;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import com.bits.loanproposal.domain.enums.DomainStatus;
import com.bits.loanproposal.infrastructure.messaging.RabbitMQConstants;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final ObjectMapper objectMapper;

    public LoanProposalProjectionHandler(
            LoanProposalReadRepository readRepository,
            InsuranceProductSnapshotRepository insuranceProductSnapshotRepository,
            ObjectMapper objectMapper) {
        this.readRepository = readRepository;
        this.insuranceProductSnapshotRepository = insuranceProductSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_CREATED_EVENT_QUEUE)
    public void onLoanProposalCreated(Message message) {
        LoanProposalCreatedEvent event = deserialize(message, LoanProposalCreatedEvent.class);
        LoanProposalReadDocument doc = LoanProposalReadMapper.toReadDocument(
                event,
                creditShieldExpiry(event),
                fireInsuranceExpiry(event),
                fireInsuranceProductName(event, null));
        doc.setIsActive(true);
        readRepository.save(doc);
        log.info("Projected LoanProposalCreatedEvent traceId={} id={}", event.getTraceId(), event.getId());
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_UPDATED_EVENT_QUEUE)
    public void onLoanProposalUpdated(Message message) {
        LoanProposalUpdatedEvent event = deserialize(message, LoanProposalUpdatedEvent.class);
        LoanProposalReadDocument existing = readRepository.findById(event.getId())
                .orElseGet(LoanProposalReadDocument::new);
        LoanProposalReadDocument updated = LoanProposalReadMapper.mergeUpdatedFields(
                existing,
                event,
                creditShieldExpiry(event),
                fireInsuranceExpiry(event),
                fireInsuranceProductName(event, existing.getFireInsuranceProductName()));
        if (updated.getIsActive() == null) {
            updated.setIsActive(true);
        }
        readRepository.save(updated);
        log.info("Projected LoanProposalUpdatedEvent traceId={} id={}", event.getTraceId(), event.getId());
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_DELETED_EVENT_QUEUE)
    public void onLoanProposalDeleted(Message message) {
        LoanProposalDeletedEvent event = deserialize(message, LoanProposalDeletedEvent.class);
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
        if (event.getFireInsuranceProductId() == null) {
            return fallback;
        }
        return insuranceProductSnapshotRepository.findById(event.getFireInsuranceProductId())
                .map(product -> product.getName())
                .orElse(fallback);
    }

    private <T> T deserialize(Message message, Class<T> type) {
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize " + type.getSimpleName(), e);
        }
    }
}
