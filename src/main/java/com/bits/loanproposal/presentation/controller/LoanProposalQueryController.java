package com.bits.loanproposal.presentation.controller;

import com.bits.ddd.infra.core.bus.QueryBus;
import com.bits.loanproposal.application.query.*;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-proposals")
public class LoanProposalQueryController {

    private final QueryBus queryBus;

    public LoanProposalQueryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/{branchKey}/{id}")
    public ResponseEntity<Object> getById(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @PathVariable String branchKey,
            @PathVariable String id) {
        return ResponseEntity.ok(queryBus.handle(
                new GetLoanProposalByIdQuery(traceId(traceId), branchKey, id)));
    }

    @GetMapping("/{branchKey}")
    public ResponseEntity<Object> list(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @PathVariable String branchKey,
            @RequestParam(required = false) Long voId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) Long schemeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) List<LoanProposalStatus> statuses,
            @RequestParam(required = false) LoanProposalType proposalType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(queryBus.handle(new ListLoanProposalsQuery(
                traceId(traceId), branchKey, voId, memberId, loanProductId, schemeId, projectId,
                fromDate, toDate, statuses, proposalType, page, size)));
    }

    @GetMapping("/v2/{branchKey}")
    public ResponseEntity<Object> searchV2(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @PathVariable String branchKey,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long voId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) List<LoanProposalStatus> statuses,
            @RequestParam(required = false) LoanProposalType proposalType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(queryBus.handle(new SearchLoanProposalsV2Query(
                traceId(traceId), branchKey, searchTerm, voId, memberId, loanProductId,
                statuses, proposalType, fromDate, toDate, page, size)));
    }

    @GetMapping("/scheme-details")
    public ResponseEntity<Object> schemeDetails(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @RequestParam Long memberId,
            @RequestParam Long loanProductId,
            @RequestParam Long schemeId,
            @RequestParam Long branchId,
            @RequestParam(required = false) Long voId) {
        return ResponseEntity.ok(queryBus.handle(new GetSchemeDetailsQuery(
                traceId(traceId), memberId, loanProductId, schemeId, branchId, voId)));
    }

    @GetMapping("/upg-tup/{branchKey}")
    public ResponseEntity<Object> upgTupExistingLoans(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @PathVariable String branchKey,
            @RequestParam Long loanProductId) {
        return ResponseEntity.ok(queryBus.handle(
                new GetUPGTUPExistingLoansQuery(traceId(traceId), branchKey, loanProductId)));
    }

    @GetMapping("/monitor")
    public ResponseEntity<Object> monitoringFeed(
            @RequestAttribute(name = "trace_id", required = false) String traceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime) {
        return ResponseEntity.ok(queryBus.handle(
                new GetMonitoringFeedQuery(traceId(traceId), fromDateTime, toDateTime)));
    }

    private String traceId(String traceId) {
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
