package com.bits.loanproposal;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
@EnableRabbit
public class LoanProposalQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanProposalQueryServiceApplication.class, args);
    }
}
