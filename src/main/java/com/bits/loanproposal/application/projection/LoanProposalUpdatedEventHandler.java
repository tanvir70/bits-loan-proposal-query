package com.bits.loanproposal.application.projection;

import com.bits.ddd.annotation.RegisterEventHandler;
import com.bits.ddd.handler.EventHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RegisterEventHandler
public class LoanProposalUpdatedEventHandler implements EventHandler<LoanProposalUpdatedEvent> {

    private final LoanProposalReadRepository readRepository;
    private final InsuranceProductSnapshotRepository insuranceProductSnapshotRepository;

    public LoanProposalUpdatedEventHandler(
            LoanProposalReadRepository readRepository,
            InsuranceProductSnapshotRepository insuranceProductSnapshotRepository) {
        this.readRepository = readRepository;
        this.insuranceProductSnapshotRepository = insuranceProductSnapshotRepository;
    }

    @Override
    public void handle(LoanProposalUpdatedEvent event) {
        LoanProposalReadDocument existing = readRepository.findById(event.getId())
                .orElseGet(LoanProposalReadDocument::new);
        String existingProductName = existing.getFireInsuranceProductName();
        LoanProposalReadDocument updated = LoanProposalReadMapper.mergeUpdatedFields(existing, event);
        updated.setCreditShieldExpiryDate(LoanProposalEventEnrichment.creditShieldExpiry(updated));
        updated.setFireInsuranceExpiryDate(LoanProposalEventEnrichment.fireInsuranceExpiry(updated));
        updated.setFireInsuranceProductName(LoanProposalEventEnrichment.fireInsuranceProductName(
                updated.getFireInsuranceProductId(), existingProductName, insuranceProductSnapshotRepository));
        if (updated.getIsActive() == null) {
            updated.setIsActive(true);
        }
        readRepository.save(updated);
        log.info("Projected LoanProposalUpdatedEvent traceId={} id={}", event.getTracerId(), event.getId());
    }
}
