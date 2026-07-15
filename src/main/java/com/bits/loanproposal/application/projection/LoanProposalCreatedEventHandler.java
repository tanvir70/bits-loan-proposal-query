package com.bits.loanproposal.application.projection;

import com.bits.ddd.annotation.RegisterEventHandler;
import com.bits.ddd.handler.EventHandler;
import com.bits.loanproposal.application.mapper.LoanProposalReadMapper;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maintains the loan_proposal_read collection from command-side domain events (DDD-REQ-Q002).
 */
@Slf4j
@Component
@RegisterEventHandler
@RequiredArgsConstructor
public class LoanProposalCreatedEventHandler implements EventHandler<LoanProposalCreatedEvent> {

    private final LoanProposalReadRepository readRepository;
    private final InsuranceProductSnapshotRepository insuranceProductSnapshotRepository;
    private final LoanProposalReadMapper loanProposalReadMapper;

    @Override
    public void handle(LoanProposalCreatedEvent event) {
        LoanProposalReadDocument doc = loanProposalReadMapper.toReadDocument(
                event,
                LoanProposalEventEnrichment.creditShieldExpiry(event),
                LoanProposalEventEnrichment.fireInsuranceExpiry(event),
                LoanProposalEventEnrichment.fireInsuranceProductName(
                        event.getFireInsuranceProductId(), null, insuranceProductSnapshotRepository));
        doc.setIsActive(true);
        readRepository.save(doc);
        log.info("Projected LoanProposalCreatedEvent traceId={} id={}", event.getTracerId(), event.getId());
    }
}
