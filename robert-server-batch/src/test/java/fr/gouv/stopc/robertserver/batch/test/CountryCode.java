package fr.gouv.stopc.robertserver.batch.test;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;

@RequiredArgsConstructor
public enum CountryCode {

    GERMANY(49),
    FRANCE(33);

    @Getter
    private final int numericCode;

    public byte[] asByteArray() {
        return BigInteger.valueOf(numericCode).toByteArray();
    }

    public ByteString asByteString() {
        return ByteString.copyFrom(asByteArray());
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name(), numericCode);
    }
}
