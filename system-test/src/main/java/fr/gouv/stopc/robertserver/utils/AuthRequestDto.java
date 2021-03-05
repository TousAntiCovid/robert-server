package fr.gouv.stopc.robertserver.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AuthRequestDto {

    private byte[] ebid;

    private Integer epochId;

    private byte[] time;

    private byte[] mac;
}
