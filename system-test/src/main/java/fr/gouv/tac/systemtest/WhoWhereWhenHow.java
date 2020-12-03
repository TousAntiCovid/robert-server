package fr.gouv.tac.systemtest;

public class WhoWhereWhenHow {



    String who;
    String where;
    String when;
    String covidStatus;
    String outcome;

    public WhoWhereWhenHow( String who, String where, String when, String covidStatus, String outcome) {
        this.who = who;
        this.when = when;
        this.where = where;
        this.covidStatus = covidStatus;
        this.outcome = outcome;
    }

    public String getWho() {
        return this.who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public String getWhere() {
        return this.where;
    }

    public void setWhere(final String where) {
        this.where = where;
    }

    public String getWhen() {
        return this.when;
    }

    public void setWhen(final String when) {
        this.when = when;
    }

    public String getCovidStatus() {
        return this.covidStatus;
    }

    public void setCovidStatus(final String covidStatus) {
        this.covidStatus = covidStatus;
    }

    public String getOutcome() { return this.outcome;
    }
}
