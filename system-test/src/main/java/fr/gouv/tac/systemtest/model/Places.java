package fr.gouv.tac.systemtest.model;

import java.util.ArrayList;
import java.util.List;

public class Places {

    List<Place> list = new ArrayList<>();

    public Places() {
    }

    public Place getPlaceByName(String name) {
        if (!list.isEmpty()) {
            for (Place place : list) {
                if (place.getName().equals(name)) {
                    return place;
                }
            }
        }
        Place newPlace = new Place(name);
        list.add(newPlace);
        return newPlace;
    }
}
