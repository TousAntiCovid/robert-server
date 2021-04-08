package fr.gouv.clea.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.sql.DataSource;

@Component
public class NoPersistenceBatchConfigurer extends DefaultBatchConfigurer {
 
	private DataSource dataSource;

	@Autowired(required = false)
	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		super.setDataSource(dataSource);
	}
	
	@Override
	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(getTransactionManager());
		
		// this serializer ignore input so executionContext store Null values
		factory.setSerializer(new ExecutionContextSerializer() {
			
			@Override
			public Map<String, Object> deserialize(InputStream inputStream) {
				return null;
			}
			
			@Override
			public void serialize(Map<String, Object> object, OutputStream outputStream) {
				// Noop
			}
		});
		factory.afterPropertiesSet();
		return factory.getObject();
	}
}
