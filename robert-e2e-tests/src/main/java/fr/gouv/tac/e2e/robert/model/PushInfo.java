package fr.gouv.tac.e2e.robert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PushInfo {

    private String token;

    private String locale;

    private String timezone;

}
