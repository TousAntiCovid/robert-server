package fr.gouv.clea.indexation.model.output;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@EqualsAndHashCode
public class ClusterFile {

    private String name;

    private List<ClusterFileItem> items;
    
    public ClusterFile() {
        items = new ArrayList<>();
    }

    public boolean addItem(ClusterFileItem item) {
        return items.add(item);
    }
}
