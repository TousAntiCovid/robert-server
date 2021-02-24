package fr.gouv.stopc.robert.server.batch.partitioner;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import fr.gouv.stopc.robert.server.batch.utils.ItemProcessingCounterUtils;

public class RangePartitioner implements Partitioner {

    public static final String START_KEY = "start";
    public static final String END_KEY = "end";
    public static final String NAME_KEY = "name";
    public static final String NAME_VALUE = "Thread ";
    public static final String PARTITION_NAME = "Partition ";

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> result = new HashMap<>(gridSize);

        long itemIdMappingCount = ItemProcessingCounterUtils.getInstance().getCurrentIdFromItemIdMapping();
        long elementCountByPartition = (long) Math.ceil((double) itemIdMappingCount / (double) gridSize);

        IntStream.rangeClosed(1, gridSize).forEach(i -> {
            long from = 1 + ((i-1) * elementCountByPartition),
                 to =  Math.min(i * elementCountByPartition, itemIdMappingCount);
            if (from > itemIdMappingCount) {
                // no more item to process for current partition
                from = 0;
                to = 0;
            };
            ExecutionContext value = new ExecutionContext();
            value.putLong(START_KEY, from);
            value.putLong(END_KEY, to);
            value.putString(NAME_KEY, NAME_VALUE + i);
            result.put(PARTITION_NAME + i, value);
        });

        return result;
    }
}
