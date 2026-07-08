package com.bits.loanproposal.application.projection.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanProposalUpdatedEvent extends LoanProposalEventPayload {
}
