package com.bits.loanproposal.infrastructure.messaging;

import com.bits.loanproposal.application.projection.LoanProposalProjectionHandler;
import com.bits.loanproposal.application.projection.event.LoanProposalUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static com.bits.loanproposal.application.projection.LoanProposalProjectionHandlerTest.commandUpdateJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanProposalProjectionListenerTest {

    @Mock
    private LoanProposalProjectionHandler projectionHandler;

    @Test
    void onLoanProposalUpdatedDeserializesMessageAndDelegatesToProjectionHandler() {
        LoanProposalProjectionListener listener = new LoanProposalProjectionListener(projectionHandler);

        listener.onLoanProposalUpdated(message(commandUpdateJson()));

        ArgumentCaptor<LoanProposalUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(LoanProposalUpdatedEvent.class);
        verify(projectionHandler).handle(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getId()).isEqualTo("proposal-1");
        assertThat(eventCaptor.getValue().getTraceId()).isEqualTo("trace-1");
        assertThat(eventCaptor.getValue().getProposalNumber()).isEqualTo("LP-UPDATED");
    }

    private Message message(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
