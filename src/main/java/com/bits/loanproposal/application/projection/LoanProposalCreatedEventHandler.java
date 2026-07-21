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

@Slf4j
@Component
@RegisterEventHandler
@RequiredArgsConstructor
public class LoanProposalCreatedEventHandler implements EventHandler<LoanProposalCreatedEvent> {

    private final LoanProposalReadRepository readRepository;
    private final InsuranceProductSnapshotRepository insuranceProductSnapshotRepository;
    private final LoanProposalReadMapper loanProposalReadMapper;

    @Override
    public void handle(LoanProposalCreatedEvent loanProposalCreatedEvent) {
        LoanProposalReadDocument loanProposalReadDocument = loanProposalReadMapper.toReadDocument(
                loanProposalCreatedEvent,
                LoanProposalEventEnrichment.creditShieldExpiry(loanProposalCreatedEvent),
                LoanProposalEventEnrichment.fireInsuranceExpiry(loanProposalCreatedEvent),
                LoanProposalEventEnrichment.fireInsuranceProductName(
                        loanProposalCreatedEvent.getFireInsuranceProductId(), null, insuranceProductSnapshotRepository));

        loanProposalReadDocument.setIsActive(true);

        readRepository.save(loanProposalReadDocument);
        log.info("Projected LoanProposalCreatedEvent traceId={} id={}", loanProposalCreatedEvent.getTracerId(), loanProposalCreatedEvent.getId());
    }
}
