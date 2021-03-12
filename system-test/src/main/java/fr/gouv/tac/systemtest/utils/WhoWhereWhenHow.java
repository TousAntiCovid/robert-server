package fr.gouv.tac.systemtest.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class WhoWhereWhenHow {

    private final String who;
    private final String where;
    private final String when;
    private final String covidStatus;
    private final String outcome;
}
