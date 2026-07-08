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
public class SecondInsurer {
    private String id;
    private String name;
    private Long genderId;
    private Long relationshipId;
    private LocalDate dateOfBirth;
    private String nationalId;
    
    // Flags for validation (would be populated from external verification or lookup)
    private boolean isEngagedWithOtherLoans;
    private boolean isEngagedWithOtherInsurance;
    private boolean hasOtherLoanAccounts;

    public boolean sameIdentityAs(String memberNationalId) {
        return this.nationalId != null && this.nationalId.equals(memberNationalId);
    }
}
