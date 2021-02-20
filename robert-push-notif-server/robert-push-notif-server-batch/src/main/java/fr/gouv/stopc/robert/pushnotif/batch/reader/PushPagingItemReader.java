package fr.gouv.stopc.robert.pushnotif.batch.reader;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;

import fr.gouv.stopc.robert.pushnotif.batch.mapper.PushRowMapper;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchConstants;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public class PushPagingItemReader extends JdbcPagingItemReader<PushInfo> {

    public PushPagingItemReader(DataSource dataSource, PagingQueryProvider queryProvider, long minId, long maxId, 
            Date pushDate, int pageSize) {

        Map<String, Object> parameterValues = new HashMap<>(3);
        parameterValues.put(PushBatchConstants.MIN_ID, minId);

        if(minId != maxId) {
            parameterValues.put(PushBatchConstants.MAX_ID, maxId);
        }

        if(Objects.nonNull(pushDate)) {
            parameterValues.put(PushBatchConstants.PUSH_DATE, pushDate);
        }

        setName(this.getClass().getName());
        setDataSource(dataSource);
        setQueryProvider(queryProvider);
        setParameterValues(parameterValues);
        setPageSize(pageSize);
        setRowMapper(new PushRowMapper());
    }
}
