package fr.gouv.tac.systemtest.robert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ClientIdentifierBundle {

    private byte[] id;
    private byte[] keyForMac;
    private byte[] keyForTuples;

}