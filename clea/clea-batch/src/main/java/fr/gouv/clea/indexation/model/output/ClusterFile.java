package fr.gouv.clea.indexation.model.output;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class ClusterFile {

    private List<ClusterFileItem> items;
    
    public ClusterFile() {
        items = new ArrayList<ClusterFileItem>();
    }

    public boolean addItem(ClusterFileItem item) {
        return items.add(item);
    }
}
