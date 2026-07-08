package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SchemeSnapshotRepository extends MongoRepository<SchemeSnapshotDocument, Long> {
}
