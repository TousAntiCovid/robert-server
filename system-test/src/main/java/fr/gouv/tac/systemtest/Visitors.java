package fr.gouv.tac.systemtest;

import java.util.ArrayList;
import java.util.List;

public class Visitors {


    List<Visitor> list = new ArrayList<Visitor>();

    Visitor getVisitorByName(String name){
        if (!list.isEmpty()) {
            for (Visitor visitor : list) {
                if (visitor.getName().equals(name)) {
                    return visitor;
                }
            }
        }
        Visitor newVisitor = new Visitor(name);
        list.add(newVisitor);
        return newVisitor;
    }

    public List<Visitor> getList() {
        return this.list;
    }

    public void setList(final List<Visitor> list) {
        this.list = list;
    }
}
