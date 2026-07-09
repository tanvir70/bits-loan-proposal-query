package com.bits.loanproposal.infrastructure.readmodel.config;

import com.bits.ddd.infra.persistence.converter.MongoConverters;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class LoanProposalReadPersistenceConfiguration {

    private final MongoTemplate mongoTemplate;

    public LoanProposalReadPersistenceConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Object> converters = new ArrayList<>(MongoConverters.all());
        converters.add(new LoanProposalStatusToStringConverter());
        converters.add(new StringToLoanProposalStatusConverter());
        return new MongoCustomConversions(converters);
    }

    @WritingConverter
    public static class LoanProposalStatusToStringConverter implements Converter<LoanProposalStatus, String> {
        @Override
        public String convert(LoanProposalStatus source) {
            return source != null ? source.code() : null;
        }
    }

    @ReadingConverter
    public static class StringToLoanProposalStatusConverter implements Converter<String, LoanProposalStatus> {
        @Override
        public LoanProposalStatus convert(String source) {
            return source != null ? LoanProposalStatus.of(source) : null;
        }
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
