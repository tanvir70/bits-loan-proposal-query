package com.bits.loanproposal.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guarantor {
    private String id;
    private String name;
    private Long relationshipId;
    private String nationalId;
    private LocalDate dateOfBirth;
}
