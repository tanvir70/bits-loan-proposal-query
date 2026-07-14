package com.bits.loanproposal.shared.exception;

import com.bits.ddd.shared.exception.domain.FailureException;
import com.bits.ddd.shared.exception.enums.ErrorCode;
import com.bits.ddd.shared.localization.LocalizedMessage;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class EntityNotFoundException extends FailureException {

    public EntityNotFoundException(String requestId, String requestType, String entityType, String entityId) {
        super(
                requestId,
                requestType,
                ErrorCode.ENTITY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                false,
                Map.of("entityId", LocalizedMessage.builder()
                        .key("ENTITY_NOT_FOUND")
                        .args(new Object[]{entityType, entityId})
                        .build()));
    }
}
