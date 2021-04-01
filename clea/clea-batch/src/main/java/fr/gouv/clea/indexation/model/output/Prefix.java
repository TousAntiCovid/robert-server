package fr.gouv.clea.indexation.model.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Prefix {

    private String prefix;

    public static String of(String uuid, int length) {
        return uuid.substring(0, length);
    }

}
