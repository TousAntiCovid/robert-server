package fr.gouv.clea.ws.vo;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class ReportRequest {
    @NotNull
    private List<Visit> visits;
    private Long pivotDate;
}
