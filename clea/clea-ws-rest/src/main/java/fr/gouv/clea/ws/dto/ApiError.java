package fr.gouv.clea.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private int httpStatus;
    private Instant timestamp;
    private String message;
    private Set<ApiSubError> subErrors;
}