package fr.gouv.stopc.robert.server.batch.partitioner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import fr.gouv.stopc.robert.server.batch.utils.ItemProcessingCounterUtils;

public class RangePartitionerTest {

    @BeforeEach
    public void setUp() {
        ItemProcessingCounterUtils.getInstance().resetCounters();
    }
    
    @Test
    public void testPartitionerReturnsExpectedNumberOfPartitions() {
        int nbPartitions = 5;
        RangePartitioner partioner = new RangePartitioner();
        
        Map<String, ExecutionContext> partitions = partioner.partition(nbPartitions);
        
        assertThat(partitions.size()).isEqualTo(nbPartitions);
    }

    @Test
    public void testPartitionerIndexesWhenNbItemsIsAMultipleOfNbPartitions() {
        int nbPartitions = 2,
            nbItems = 10;
        ItemProcessingCounterUtils counter = ItemProcessingCounterUtils.getInstance();
        IntStream.rangeClosed(1, nbItems)
            .forEach(i -> counter.incrementCurrentIdOfItemIdMapping());
        
        Map<String, ExecutionContext> partitions = new RangePartitioner().partition(nbPartitions);
        
        ExecutionContext contextPartition1 = partitions.get(RangePartitioner.PARTITION_NAME+"1");
        assertThat(contextPartition1.getLong(RangePartitioner.START_KEY)).isEqualTo(1);
        assertThat(contextPartition1.getLong(RangePartitioner.END_KEY)).isEqualTo(5);
        ExecutionContext contextPartition2 = partitions.get(RangePartitioner.PARTITION_NAME+"2");
        assertThat(contextPartition2.getLong(RangePartitioner.START_KEY)).isEqualTo(6);
        assertThat(contextPartition2.getLong(RangePartitioner.END_KEY)).isEqualTo(10);
    }
    
    @Test
    public void testPartitionerIndexesWhenNbItemsIsNotAMultipleOfNbPartitions() {
        int nbPartitions = 3,
            nbItems = 20;
        ItemProcessingCounterUtils counter = ItemProcessingCounterUtils.getInstance();
        IntStream.rangeClosed(1, nbItems)
            .forEach(i -> counter.incrementCurrentIdOfItemIdMapping());
        
        Map<String, ExecutionContext> partitions = new RangePartitioner().partition(nbPartitions);
        
        ExecutionContext contextPartition1 = partitions.get(RangePartitioner.PARTITION_NAME+"1");
        assertThat(contextPartition1.getLong(RangePartitioner.START_KEY)).isEqualTo(1);
        assertThat(contextPartition1.getLong(RangePartitioner.END_KEY)).isEqualTo(7);
        ExecutionContext contextPartition2 = partitions.get(RangePartitioner.PARTITION_NAME+"2");
        assertThat(contextPartition2.getLong(RangePartitioner.START_KEY)).isEqualTo(8);
        assertThat(contextPartition2.getLong(RangePartitioner.END_KEY)).isEqualTo(14);
        ExecutionContext contextPartition3 = partitions.get(RangePartitioner.PARTITION_NAME+"3");
        assertThat(contextPartition3.getLong(RangePartitioner.START_KEY)).isEqualTo(15);
        assertThat(contextPartition3.getLong(RangePartitioner.END_KEY)).isEqualTo(20);
    }

    
    @Test
    public void testPartitionerIndexesWhenNbItemsIsNotEnougItemsForNbPartitions() {
        int nbPartitions = 6,
            nbItems = 3;
        ItemProcessingCounterUtils counter = ItemProcessingCounterUtils.getInstance();
        IntStream.rangeClosed(1, nbItems)
            .forEach(i -> counter.incrementCurrentIdOfItemIdMapping());
        
        Map<String, ExecutionContext> partitions = new RangePartitioner().partition(nbPartitions);
        
        ExecutionContext contextPartition1 = partitions.get(RangePartitioner.PARTITION_NAME+"1");
        assertThat(contextPartition1.getLong(RangePartitioner.START_KEY)).isEqualTo(1);
        assertThat(contextPartition1.getLong(RangePartitioner.END_KEY)).isEqualTo(1);
        ExecutionContext contextPartition2 = partitions.get(RangePartitioner.PARTITION_NAME+"2");
        assertThat(contextPartition2.getLong(RangePartitioner.START_KEY)).isEqualTo(2);
        assertThat(contextPartition2.getLong(RangePartitioner.END_KEY)).isEqualTo(2);
        ExecutionContext contextPartition3 = partitions.get(RangePartitioner.PARTITION_NAME+"3");
        assertThat(contextPartition3.getLong(RangePartitioner.START_KEY)).isEqualTo(3);
        assertThat(contextPartition3.getLong(RangePartitioner.END_KEY)).isEqualTo(3);
        IntStream.rangeClosed(nbItems + 1, nbPartitions).
            forEach(i -> {
                ExecutionContext contextPartition = partitions.get(RangePartitioner.PARTITION_NAME+i);
                assertThat(contextPartition.getLong(RangePartitioner.START_KEY)).isEqualTo(0);
                assertThat(contextPartition.getLong(RangePartitioner.END_KEY)).isEqualTo(0);                
            });
    }
    
    @Test
    public void testPartitionerWhenNoElementToProcess() {
        int nbPartitions = 2;
        
        Map<String, ExecutionContext> partitions = new RangePartitioner().partition(nbPartitions);
        
        ExecutionContext contextPartition1 = partitions.get(RangePartitioner.PARTITION_NAME+"1");
        assertThat(contextPartition1.getLong(RangePartitioner.START_KEY)).isEqualTo(0);
        assertThat(contextPartition1.getLong(RangePartitioner.END_KEY)).isEqualTo(0);
        ExecutionContext contextPartition2 = partitions.get(RangePartitioner.PARTITION_NAME+"2");
        assertThat(contextPartition2.getLong(RangePartitioner.START_KEY)).isEqualTo(0);
        assertThat(contextPartition2.getLong(RangePartitioner.END_KEY)).isEqualTo(0);
    }
}
