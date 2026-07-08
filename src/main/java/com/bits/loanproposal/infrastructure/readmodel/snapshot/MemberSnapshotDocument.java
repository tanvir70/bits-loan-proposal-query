package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// ponytail: slim read-only view of member_snapshot — only fields the query side needs
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "member_snapshot")
public class MemberSnapshotDocument {
    @Id
    private Long memberId;
    private String status;
    private Long voId;
}
