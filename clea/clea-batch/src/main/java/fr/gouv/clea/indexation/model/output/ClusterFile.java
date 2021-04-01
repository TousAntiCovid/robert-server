package fr.gouv.clea.indexation.model.output;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Data
@ToString
@EqualsAndHashCode
public class ClusterFile {

    private List<ClusterFileItem> items;
    
    public ClusterFile() {
        items = new ArrayList<>();
    }

    public boolean addItem(ClusterFileItem item) {
        return items.add(item);
    }
}
