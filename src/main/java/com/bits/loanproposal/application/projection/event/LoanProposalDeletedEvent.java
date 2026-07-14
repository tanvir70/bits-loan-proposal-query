package com.bits.loanproposal.application.projection.event;

import com.bits.ddd.shared.messaging.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanProposalDeletedEvent extends Event {
    private String id;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
