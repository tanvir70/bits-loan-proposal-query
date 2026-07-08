package com.bits.loanproposal.infrastructure.readmodel.config;

import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class LoanProposalReadPersistenceConfiguration {

    private final MongoTemplate mongoTemplate;

    public LoanProposalReadPersistenceConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(LoanProposalReadDocument.class);

        indexOps.createIndex(new Index()
                .named("idx_read_branch_datasource_status")
                .on("branchCode", Sort.Direction.ASC)
                .on("dataSource", Sort.Direction.ASC)
                .on("loanProposalStatus", Sort.Direction.ASC));

        indexOps.createIndex(new Index()
                .named("idx_read_member_status")
                .on("memberId", Sort.Direction.ASC)
                .on("loanProposalStatus", Sort.Direction.ASC));

        indexOps.createIndex(new Index()
                .named("idx_read_created_at")
                .on("createdAt", Sort.Direction.DESC));

        indexOps.createIndex(new Index()
                .named("idx_read_proposal_number")
                .on("proposalNumber", Sort.Direction.ASC)
                .unique()
                .sparse());

        indexOps.createIndex(new Index()
                .named("idx_read_proposal_type")
                .on("loanProposalType", Sort.Direction.ASC));

        indexOps.createIndex(new Index()
                .named("idx_read_is_active")
                .on("isActive", Sort.Direction.ASC));
    }
}
