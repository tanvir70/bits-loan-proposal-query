package com.bits.loanproposal.infrastructure.readmodel.snapshot;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VillageOrganisationSnapshotRepository extends MongoRepository<VillageOrganisationSnapshotDocument, Long> {
}
