package fr.gouv.clea.ws.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class ReportRequest {
    @NotNull
    private List<Visit> visits;

    @JsonProperty("pivotDate")
    private Long pivotDateAsNtpTimestamp;
}
