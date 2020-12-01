package fr.gouv.tac.systemtest;

public class WhoWhereWhenHow {



    String who;
    String where;
    String when;
    String covidStatus;

    public WhoWhereWhenHow( String who, String where, String when, String covidStatus) {
        this.who = who;
        this.when = when;
        this.where = where;
        this.covidStatus = covidStatus;

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


}
