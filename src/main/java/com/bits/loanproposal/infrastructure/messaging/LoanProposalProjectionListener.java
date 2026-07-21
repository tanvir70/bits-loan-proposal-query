package com.bits.loanproposal.infrastructure.messaging;

import com.bits.ddd.service.EventProcessWrapper;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanProposalProjectionListener {

    private final EventProcessWrapper<Message> eventProcessWrapper;

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_CREATED_EVENT_QUEUE)
    public void onLoanProposalCreated(Message message) {
        eventProcessWrapper.processMessage(message, LoanProposalCreatedEvent.class);
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_UPDATED_EVENT_QUEUE)
    public void onLoanProposalUpdated(Message message) {
        eventProcessWrapper.processMessage(message, LoanProposalUpdatedEvent.class);
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_DELETED_EVENT_QUEUE)
    public void onLoanProposalDeleted(Message message) {
        eventProcessWrapper.processMessage(message, LoanProposalDeletedEvent.class);
    }
}
