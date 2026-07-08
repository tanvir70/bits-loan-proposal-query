package com.bits.loanproposal.infrastructure.readmodel.repository;

import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanProposalReadRepository
        extends MongoRepository<LoanProposalReadDocument, String>, LoanProposalReadRepositoryCustom {

    Optional<LoanProposalReadDocument> findByIdAndBranchCodeAndIsActive(String id, String branchCode, Boolean isActive);

    Optional<LoanProposalReadDocument> findByProposalNumber(String proposalNumber);

    List<LoanProposalReadDocument> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
