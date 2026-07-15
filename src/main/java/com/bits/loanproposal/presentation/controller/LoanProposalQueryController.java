package com.bits.loanproposal.presentation.controller;

import com.bits.ddd.infra.core.bus.QueryBus;
import com.bits.ddd.shared.constants.MdcConstants;
import com.bits.ddd.shared.dto.ApiResponse;
import com.bits.loanproposal.application.query.GetLoanProposalByIdQuery;
import com.bits.loanproposal.application.query.GetUPGTUPExistingLoansQuery;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.domain.enums.LoanProposalType;
import com.bits.loanproposal.presentation.dto.*;
import com.bits.loanproposal.presentation.dto.request.GetMonitoringFeedRequest;
import com.bits.loanproposal.presentation.dto.request.GetSchemeDetailsRequest;
import com.bits.loanproposal.presentation.dto.request.ListLoanProposalsRequest;
import com.bits.loanproposal.presentation.dto.request.SearchLoanProposalsV2Request;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.bits.loanproposal.presentation.constant.RouteConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(LOAN_PROPOSALS_BASE)
public class LoanProposalQueryController {

    private final QueryBus queryBus;

    @GetMapping(GET_BY_ID)
    public ResponseEntity<ApiResponse<LoanProposalResponse>> getById(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
            @PathVariable String branchKey, @PathVariable String id) {

        String resolvedTraceId = traceId(traceId);

        LoanProposalResponse loanProposalResponse = queryBus.handle(
                new GetLoanProposalByIdQuery(resolvedTraceId, branchKey, id));

        return ok(loanProposalResponse, resolvedTraceId);
    }

    @GetMapping(LIST)
    public ResponseEntity<ApiResponse<Page<LoanProposalListItem>>> list(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
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
        String resolvedTraceId = traceId(traceId);

        ListLoanProposalsRequest listLoanProposalsRequest = new ListLoanProposalsRequest(
                voId, memberId, loanProductId, schemeId, projectId, fromDate, toDate, statuses, proposalType, page, size);

        Page<LoanProposalListItem> loanProposalListItems = queryBus.handle(
                listLoanProposalsRequest.toQuery(resolvedTraceId, branchKey));

        return ok(loanProposalListItems, resolvedTraceId);
    }

    @GetMapping(SEARCH_V2)
    public ResponseEntity<ApiResponse<Page<LoanProposalListItem>>> searchV2(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
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
        String resolvedTraceId = traceId(traceId);

        SearchLoanProposalsV2Request searchLoanProposalsV2Request = new SearchLoanProposalsV2Request(
                searchTerm, voId, memberId, loanProductId, statuses, proposalType, fromDate, toDate, page, size);

        Page<LoanProposalListItem> loanProposalListItems = queryBus.handle(
                searchLoanProposalsV2Request.toQuery(resolvedTraceId, branchKey));

        return ok(loanProposalListItems, resolvedTraceId);
    }

    @GetMapping(SCHEME_DETAILS)
    public ResponseEntity<ApiResponse<SchemeDetailsResponse>> schemeDetails(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
            @RequestParam Long memberId,
            @RequestParam Long loanProductId,
            @RequestParam Long schemeId,
            @RequestParam Long branchId,
            @RequestParam(required = false) Long voId) {
        String resolvedTraceId = traceId(traceId);

        GetSchemeDetailsRequest getSchemeDetailsRequest =
                new GetSchemeDetailsRequest(memberId, loanProductId, schemeId, branchId, voId);

        SchemeDetailsResponse schemeDetailsResponse = queryBus.handle(getSchemeDetailsRequest.toQuery(resolvedTraceId));

        return ok(schemeDetailsResponse, resolvedTraceId);
    }

    @GetMapping(UPG_TUP_EXISTING_LOANS)
    public ResponseEntity<ApiResponse<UPGTUPExistingLoansResponse>> upgTupExistingLoans(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
            @PathVariable String branchKey,
            @RequestParam Long loanProductId) {
        String resolvedTraceId = traceId(traceId);

        UPGTUPExistingLoansResponse upgTupExistingLoansResponse = queryBus.handle(
                new GetUPGTUPExistingLoansQuery(resolvedTraceId, branchKey, loanProductId));

        return ok(upgTupExistingLoansResponse, resolvedTraceId);
    }

    @GetMapping(MONITORING_FEED)
    public ResponseEntity<ApiResponse<MonitoringFeedResponse>> monitoringFeed(
            @RequestAttribute(name = MdcConstants.TRACE_ID, required = false) String traceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime) {
        String resolvedTraceId = traceId(traceId);

        GetMonitoringFeedRequest getMonitoringFeedRequest = new GetMonitoringFeedRequest(fromDateTime, toDateTime);

        MonitoringFeedResponse monitoringFeedResponse = queryBus.handle(getMonitoringFeedRequest.toQuery(resolvedTraceId));

        return ok(monitoringFeedResponse, resolvedTraceId);
    }

    private String traceId(String traceId) {
        return traceId != null && !traceId.isBlank() ? traceId : "missing-trace";
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data, String traceId) {
        return ResponseEntity.status(HttpStatus.OK)
                .header(MdcConstants.TRACE_ID_HEADER, traceId)
                .body(ApiResponse.success(data, "Success", HttpStatus.OK.value(), traceId));
    }
}
