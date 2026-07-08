package com.bits.loanproposal.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange loanProposalExchange() {
        return new TopicExchange(RabbitMQConstants.TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue loanProposalCreatedQueue() {
        return new Queue(RabbitMQConstants.LOAN_PROPOSAL_CREATED_EVENT_QUEUE, true);
    }

    @Bean
    public Queue loanProposalUpdatedQueue() {
        return new Queue(RabbitMQConstants.LOAN_PROPOSAL_UPDATED_EVENT_QUEUE, true);
    }

    @Bean
    public Queue loanProposalDeletedQueue() {
        return new Queue(RabbitMQConstants.LOAN_PROPOSAL_DELETED_EVENT_QUEUE, true);
    }

    @Bean
    public Binding createdBinding() {
        return BindingBuilder.bind(loanProposalCreatedQueue())
                .to(loanProposalExchange()).with(RabbitMQConstants.CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding updatedBinding() {
        return BindingBuilder.bind(loanProposalUpdatedQueue())
                .to(loanProposalExchange()).with(RabbitMQConstants.UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding deletedBinding() {
        return BindingBuilder.bind(loanProposalDeletedQueue())
                .to(loanProposalExchange()).with(RabbitMQConstants.DELETED_ROUTING_KEY);
    }
}
