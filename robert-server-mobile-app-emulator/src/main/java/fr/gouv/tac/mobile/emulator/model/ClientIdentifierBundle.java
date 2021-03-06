package fr.gouv.tac.mobile.emulator.model;

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