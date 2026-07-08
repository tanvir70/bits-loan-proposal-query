package com.bits.loanproposal.infrastructure.messaging;

import com.bits.ddd.shared.util.JsonUtil;
import com.bits.loanproposal.application.projection.LoanProposalProjectionHandler;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LoanProposalProjectionListener {

    private final LoanProposalProjectionHandler projectionHandler;

    public LoanProposalProjectionListener(LoanProposalProjectionHandler projectionHandler) {
        this.projectionHandler = projectionHandler;
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_CREATED_EVENT_QUEUE)
    public void onLoanProposalCreated(Message message) {
        projectionHandler.handle(deserialize(message, LoanProposalCreatedEvent.class));
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_UPDATED_EVENT_QUEUE)
    public void onLoanProposalUpdated(Message message) {
        projectionHandler.handle(deserialize(message, LoanProposalUpdatedEvent.class));
    }

    @RabbitListener(queues = RabbitMQConstants.LOAN_PROPOSAL_DELETED_EVENT_QUEUE)
    public void onLoanProposalDeleted(Message message) {
        projectionHandler.handle(deserialize(message, LoanProposalDeletedEvent.class));
    }

    private <T> T deserialize(Message message, Class<T> type) {
        return JsonUtil.deserialize(message.getBody(), type);
    }
}
