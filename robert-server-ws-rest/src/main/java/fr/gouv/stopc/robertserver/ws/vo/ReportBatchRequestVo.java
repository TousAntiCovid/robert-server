package fr.gouv.stopc.robertserver.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReportBatchRequestVo {

    @NotNull
    @ToString.Exclude
    private String token;

    private List<ContactVo> contacts;

    private String contactsAsBinary;

}
