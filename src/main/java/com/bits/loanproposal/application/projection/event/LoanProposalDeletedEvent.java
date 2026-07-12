package com.bits.loanproposal.application.projection.event;

import com.bits.ddd.shared.messaging.EventMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanProposalDeletedEvent extends EventMessage {
    private String id;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
