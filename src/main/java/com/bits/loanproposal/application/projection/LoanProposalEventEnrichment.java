package com.bits.loanproposal.application.projection;

import com.bits.loanproposal.application.projection.event.LoanProposalEventPayload;
import com.bits.loanproposal.infrastructure.readmodel.document.LoanProposalReadDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotDocument;
import com.bits.loanproposal.infrastructure.readmodel.snapshot.InsuranceProductSnapshotRepository;

import java.time.LocalDate;

/**
 * Expiry-date derivation shared by the created/updated projection handlers.
 * Created events derive from the event payload; updated events derive from the
 * merged read document so partial payloads still recompute correctly.
 */
final class LoanProposalEventEnrichment {

    private LoanProposalEventEnrichment() {
    }

    static LocalDate creditShieldExpiry(LoanProposalEventPayload event) {
        if (Boolean.TRUE.equals(event.getMicroInsurance())
                && event.getApplicationDate() != null
                && event.getProposalDurationInMonths() != null) {
            return event.getApplicationDate().plusMonths(event.getProposalDurationInMonths());
        }
        return null;
    }

    static LocalDate fireInsuranceExpiry(LoanProposalEventPayload event) {
        if (Boolean.TRUE.equals(event.getWantsFireInsurance())
                && event.getApplicationDate() != null
                && event.getFireInsuranceDetails() != null
                && event.getFireInsuranceDetails().durationOfFireInsurance() != null) {
            return event.getApplicationDate().plusMonths(event.getFireInsuranceDetails().durationOfFireInsurance());
        }
        return null;
    }

    static LocalDate creditShieldExpiry(LoanProposalReadDocument doc) {
        if (Boolean.TRUE.equals(doc.getMicroInsurance())
                && doc.getApplicationDate() != null
                && doc.getProposalDurationInMonths() != null) {
            return doc.getApplicationDate().plusMonths(doc.getProposalDurationInMonths());
        }
        return null;
    }

    static LocalDate fireInsuranceExpiry(LoanProposalReadDocument doc) {
        if (Boolean.TRUE.equals(doc.getWantsFireInsurance())
                && doc.getApplicationDate() != null
                && doc.getFireInsuranceDetails() != null
                && doc.getFireInsuranceDetails().durationOfFireInsurance() != null) {
            return doc.getApplicationDate().plusMonths(doc.getFireInsuranceDetails().durationOfFireInsurance());
        }
        return null;
    }

    static String fireInsuranceProductName(
            Long fireInsuranceProductId, String fallback, InsuranceProductSnapshotRepository repository) {
        if (fireInsuranceProductId == null) {
            return fallback;
        }
        return repository.findById(fireInsuranceProductId)
                .map(InsuranceProductSnapshotDocument::getName)
                .orElse(fallback);
    }
}
