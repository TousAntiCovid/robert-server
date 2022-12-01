package fr.gouv.stopc.e2e.mobileapplication.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApplicationIdentityRepository {

    private final JdbcOperations cryptoJdbcOperations;

    public String findLastInsertedIdA() {
        return cryptoJdbcOperations.queryForObject("select ida from identity order by id desc limit 1", String.class);
    }
}
