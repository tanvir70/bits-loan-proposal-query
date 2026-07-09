package com.bits.loanproposal.presentation.controller;

import com.bits.ddd.infra.core.bus.QueryBus;
import com.bits.ddd.shared.constants.MdcConstants;
import com.bits.loanproposal.application.query.*;
import com.bits.loanproposal.domain.enums.LoanProposalStatus;
import com.bits.loanproposal.presentation.dto.LoanProposalListItem;
import com.bits.loanproposal.presentation.dto.LoanProposalResponse;
import com.bits.loanproposal.presentation.dto.MonitoringFeedResponse;
import com.bits.loanproposal.presentation.dto.SchemeDetailsResponse;
import com.bits.loanproposal.presentation.dto.UPGTUPExistingLoansResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanProposalQueryController.class)
class LoanProposalQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryBus queryBus;

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.Import(LoanProposalQueryController.class)
    static class TestConfig {
    }

    @Test
    void getByIdEndpointCallsQueryBusAndReturnsDto() throws Exception {
        LoanProposalResponse response = LoanProposalResponse.builder()
                .id("proposal-1")
                .proposalNumber("LP-123")
                .build();

        when(queryBus.handle(any(GetLoanProposalByIdQuery.class))).thenReturn(response);

        mockMvc.perform(get("/api/loan-proposals/0010/proposal-1")
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

        when(queryBus.handle(any(ListLoanProposalsQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/loan-proposals/0010")
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

        when(queryBus.handle(any(SearchLoanProposalsV2Query.class))).thenReturn(page);

        mockMvc.perform(get("/api/loan-proposals/v2/0010")
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

        when(queryBus.handle(any(GetSchemeDetailsQuery.class))).thenReturn(response);

        mockMvc.perform(get("/api/loan-proposals/scheme-details")
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

        when(queryBus.handle(any(GetUPGTUPExistingLoansQuery.class))).thenReturn(response);

        mockMvc.perform(get("/api/loan-proposals/upg-tup/0010")
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

        when(queryBus.handle(any(GetMonitoringFeedQuery.class))).thenReturn(response);

        mockMvc.perform(get("/api/loan-proposals/monitor")
                        .param("fromDateTime", "2026-01-15T09:00:00")
                        .param("toDateTime", "2026-01-15T15:00:00")
                        .header(MdcConstants.TRACE_ID_HEADER, "test-trace")
                        .requestAttr(MdcConstants.TRACE_ID, "test-trace")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
