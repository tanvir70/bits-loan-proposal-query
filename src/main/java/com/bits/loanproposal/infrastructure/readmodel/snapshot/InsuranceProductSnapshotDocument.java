package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "insurance_product_snapshot")
public class InsuranceProductSnapshotDocument {
    @Id
    private Long insuranceProductId;
    private String name;
}
