package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "village_organisation_snapshot")
public class VillageOrganisationSnapshotDocument {
    @Id
    private Long voId;
    private String category;
}
