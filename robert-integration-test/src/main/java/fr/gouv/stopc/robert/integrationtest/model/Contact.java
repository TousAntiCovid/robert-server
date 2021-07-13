package fr.gouv.stopc.robert.integrationtest.model;


import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class Contact {
    private byte[] ebid;
    private byte[] ecc;
    private List<HelloMessageDetail> ids = new ArrayList<>();

    public Contact addIdsItem(HelloMessageDetail idsItem) {
        this.ids.add(idsItem);
        return this;
    }
}
