package com.bits.loanproposal.presentation.controller;

import com.bits.ddd.infra.core.bus.QueryBus;
import com.bits.ddd.shared.constants.MdcConstants;
import com.bits.loanproposal.application.query.*;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse;
import com.bits.loanproposal.presentation.dto.SchemeDetailsResponse;
import com.bits.loanproposal.presentation.dto.UPGTUPExistingLoansResponse;
import com.bits.loanproposal.presentation.constant.RouteConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanProposalQueryController.class)
class LoanProposalQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryBus queryBus;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(LoanProposalQueryController.class)
    @EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
    static class TestConfig {
    }

    @Test
    void getByIdEndpointCallsQueryBusAndReturnsDto() throws Exception {
        LoanProposalResponse response = LoanProposalResponse.builder()
                .id("proposal-1")
                .proposalNumber("LP-123")
                .build();

        when(queryBus.handle(argThat(q -> q instanceof GetLoanProposalByIdQuery
                && "0010".equals(((GetLoanProposalByIdQuery) q).branchKey())
                && "proposal-1".equals(((GetLoanProposalByIdQuery) q).id())
                && "test-trace".equals(((GetLoanProposalByIdQuery) q).traceId()))))
                .thenReturn(response);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + "/0010/proposal-1")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(MdcConstants.TRACE_ID_HEADER, "test-trace"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("proposal-1"))
                .andExpect(jsonPath("$.data.proposalNumber").value("LP-123"));
    }

    @Test
    void listEndpointMapsRequestParamsToQuery() throws Exception {
        LoanProposalListItem item = LoanProposalListItem.builder()
                .id("proposal-1")
                .build();
        Page<LoanProposalListItem> page = new PageImpl<>(List.of(item));

        when(queryBus.handle(argThat(q -> q instanceof ListLoanProposalsQuery
                && "0010".equals(((ListLoanProposalsQuery) q).branchKey())
                && Long.valueOf(15L).equals(((ListLoanProposalsQuery) q).voId())
                && List.of(com.bits.loanproposal.domain.enums.LoanProposalStatus.PENDING, com.bits.loanproposal.domain.enums.LoanProposalStatus.APPROVED).equals(((ListLoanProposalsQuery) q).statuses())
                && "test-trace".equals(((ListLoanProposalsQuery) q).traceId()))))
                .thenReturn(page);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + "/0010")
                        .param("voId", "15")
                        .param("statuses", "PENDING,APPROVED")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("proposal-1"));
    }

    @Test
    void searchV2EndpointMapsRequestParamsToQuery() throws Exception {
        LoanProposalListItem item = LoanProposalListItem.builder()
                .id("proposal-1")
                .build();
        Page<LoanProposalListItem> page = new PageImpl<>(List.of(item));

        when(queryBus.handle(argThat(q -> q instanceof SearchLoanProposalsV2Query
                && "0010".equals(((SearchLoanProposalsV2Query) q).branchKey())
                && "john".equals(((SearchLoanProposalsV2Query) q).searchTerm())
                && "test-trace".equals(((SearchLoanProposalsV2Query) q).traceId()))))
                .thenReturn(page);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + "/v2/0010")
                        .param("searchTerm", "john")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void schemeDetailsEndpointMapsParamsToQuery() throws Exception {
        SchemeDetailsResponse response = new SchemeDetailsResponse(50L, true, "CAT_A", 1, BigDecimal.TEN, BigDecimal.ONE);

        when(queryBus.handle(argThat(q -> q instanceof GetSchemeDetailsQuery
                && Long.valueOf(30L).equals(((GetSchemeDetailsQuery) q).memberId())
                && Long.valueOf(40L).equals(((GetSchemeDetailsQuery) q).loanProductId())
                && Long.valueOf(50L).equals(((GetSchemeDetailsQuery) q).schemeId())
                && Long.valueOf(10L).equals(((GetSchemeDetailsQuery) q).branchId())
                && "test-trace".equals(((GetSchemeDetailsQuery) q).traceId()))))
                .thenReturn(response);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + RouteConstants.SCHEME_DETAILS)
                        .param("memberId", "30")
                        .param("loanProductId", "40")
                        .param("schemeId", "50")
                        .param("branchId", "10")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schemeId").value(50L));
    }

    @Test
    void upgTupExistingLoansEndpointMapsParamsToQuery() throws Exception {
        UPGTUPExistingLoansResponse response = new UPGTUPExistingLoansResponse("0010", 40L, 5, BigDecimal.TEN);

        when(queryBus.handle(argThat(q -> q instanceof GetUPGTUPExistingLoansQuery
                && "0010".equals(((GetUPGTUPExistingLoansQuery) q).branchKey())
                && Long.valueOf(40L).equals(((GetUPGTUPExistingLoansQuery) q).loanProductId())
                && "test-trace".equals(((GetUPGTUPExistingLoansQuery) q).traceId()))))
                .thenReturn(response);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + "/upg-tup/0010")
                        .param("loanProductId", "40")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanProductId").value(40L));
    }

    @Test
    void monitoringFeedEndpointMapsParamsToQuery() throws Exception {
        MonitoringFeedResponse response = new MonitoringFeedResponse(
                LocalDateTime.now().minusHours(1), LocalDateTime.now(), 0, Collections.emptyList());

        when(queryBus.handle(argThat(q -> q instanceof GetMonitoringFeedQuery
                && LocalDateTime.of(2026, 1, 15, 9, 0, 0).equals(((GetMonitoringFeedQuery) q).fromDateTime())
                && LocalDateTime.of(2026, 1, 15, 15, 0, 0).equals(((GetMonitoringFeedQuery) q).toDateTime())
                && "test-trace".equals(((GetMonitoringFeedQuery) q).traceId()))))
                .thenReturn(response);

        mockMvc.perform(get(RouteConstants.LOAN_PROPOSALS_BASE + RouteConstants.MONITORING_FEED)
                        .param("fromDateTime", "2026-01-15T09:00:00")
                        .param("toDateTime", "2026-01-15T15:00:00")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
