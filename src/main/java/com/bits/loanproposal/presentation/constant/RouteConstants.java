package com.bits.loanproposal.presentation.constant;

public final class RouteConstants {

    public static final String LOAN_PROPOSALS_BASE = "/api/loan-proposals";

    public static final String GET_BY_ID = "/{branchKey}/{id}";
    public static final String LIST = "/{branchKey}";
    public static final String SEARCH_V2 = "/v2/{branchKey}";
    public static final String SCHEME_DETAILS = "/scheme-details";
    public static final String UPG_TUP_EXISTING_LOANS = "/upg-tup/{branchKey}";
    public static final String MONITORING_FEED = "/monitor";

    private RouteConstants() {
    }
}
