package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "scheme_snapshot")
public class SchemeSnapshotDocument {
    @Id
    private Long schemeId;
    private String name;
    private Map<String, BigDecimal> assetGrantPercentageByVoCategory;

    public BigDecimal assetGrantPercentageForVoCategory(String voCategory) {
        if (voCategory == null || assetGrantPercentageByVoCategory == null) {
            return BigDecimal.ZERO;
        }
        return assetGrantPercentageByVoCategory.getOrDefault(voCategory, BigDecimal.ZERO);
    }
}
