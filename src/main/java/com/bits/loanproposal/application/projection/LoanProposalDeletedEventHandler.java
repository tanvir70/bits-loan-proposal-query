package com.bits.loanproposal.application.projection;

import com.bits.ddd.annotation.RegisterEventHandler;
import com.bits.ddd.handler.EventHandler;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.ddd.shared.domain.value.DomainStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.repository.LoanProposalReadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RegisterEventHandler
public class LoanProposalDeletedEventHandler implements EventHandler<LoanProposalDeletedEvent> {

    private final LoanProposalReadRepository readRepository;

    public LoanProposalDeletedEventHandler(LoanProposalReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public void handle(LoanProposalDeletedEvent event) {
        readRepository.findById(event.getId()).ifPresentOrElse(doc -> {
            doc.setIsActive(false);
            doc.setDomainStatus(DomainStatus.INACTIVE);
            doc.setLastModifiedAt(event.getDeletedAt());
            doc.setLastModifiedBy(event.getDeletedBy());
            readRepository.save(doc);
            log.info("Projected LoanProposalDeletedEvent traceId={} id={}", event.getTracerId(), event.getId());
        }, () -> log.info("Skipping LoanProposalDeletedEvent for already-absent id={}", event.getId()));
    }
}
