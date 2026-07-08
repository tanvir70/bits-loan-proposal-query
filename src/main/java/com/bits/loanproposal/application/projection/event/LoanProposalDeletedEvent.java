package com.bits.loanproposal.application.projection.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LoanProposalDeletedEvent {
    private String id;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String traceId;
}
