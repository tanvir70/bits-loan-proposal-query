package com.bits.loanproposal.infrastructure.messaging;

import com.bits.ddd.service.EventProcessWrapper;
import com.bits.loanproposal.application.projection.event.LoanProposalCreatedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalDeletedEvent;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanProposalProjectionListenerTest {

    @Mock
    private EventProcessWrapper<Message> eventProcessWrapper;

    @Test
    void routesEachQueueToTheMatchingEventType() {
        LoanProposalProjectionListener listener = new LoanProposalProjectionListener(eventProcessWrapper);
        Message message = message("{}");

        listener.onLoanProposalCreated(message);
        verify(eventProcessWrapper).processMessage(message, LoanProposalCreatedEvent.class);

        listener.onLoanProposalUpdated(message);
        verify(eventProcessWrapper).processMessage(message, LoanProposalUpdatedEvent.class);

        listener.onLoanProposalDeleted(message);
        verify(eventProcessWrapper).processMessage(message, LoanProposalDeletedEvent.class);
    }

    private Message message(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
