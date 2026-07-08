package com.bits.loanproposal.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nominee {
    private String id;
    private String name;
    private Long relationshipId;
    private Double sharePercentage;
    private List<String> insuranceTypes;
}
