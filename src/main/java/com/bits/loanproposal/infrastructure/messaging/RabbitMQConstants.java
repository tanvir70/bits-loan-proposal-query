package com.bits.loanproposal.infrastructure.messaging;

public final class RabbitMQConstants {

    public static final String TOPIC_EXCHANGE = "loan-proposal.exchange";

    public static final String LOAN_PROPOSAL_CREATED_EVENT_QUEUE = "loan-proposal.created.queue";
    public static final String LOAN_PROPOSAL_UPDATED_EVENT_QUEUE = "loan-proposal.updated.queue";
    public static final String LOAN_PROPOSAL_DELETED_EVENT_QUEUE = "loan-proposal.deleted.queue";

    public static final String CREATED_ROUTING_KEY = "loan-proposal.created";
    public static final String UPDATED_ROUTING_KEY = "loan-proposal.updated";
    public static final String DELETED_ROUTING_KEY = "loan-proposal.deleted";

    private RabbitMQConstants() {
    }
}
