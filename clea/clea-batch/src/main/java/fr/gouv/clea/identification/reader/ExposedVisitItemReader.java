package fr.gouv.clea.identification.reader;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;

import fr.gouv.clea.entity.ExposedVisit;
import fr.gouv.clea.identification.ExposedVisitRowMapper;

public class ExposedVisitItemReader extends JdbcPagingItemReader<ExposedVisit> {

    public ExposedVisitItemReader(final DataSource dataSource, final PagingQueryProvider queryProvider, final Map<String, Object> parameterValues) {
        setRowMapper(new ExposedVisitRowMapper());
        setName(this.getClass().getName());
        setDataSource(dataSource);
        setQueryProvider(queryProvider);
        setParameterValues(parameterValues);
    }
}
