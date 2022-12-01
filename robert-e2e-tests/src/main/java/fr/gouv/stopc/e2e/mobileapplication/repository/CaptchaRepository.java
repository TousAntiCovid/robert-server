package fr.gouv.stopc.e2e.mobileapplication.repository;

import fr.gouv.stopc.e2e.mobileapplication.repository.model.Captcha;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Repository
@ConditionalOnBean(name = "captchaDataSource")
public class CaptchaRepository {

    private final SimpleJdbcInsert captcha;

    public CaptchaRepository(final DataSource captchaDataSource) {
        captcha = new SimpleJdbcInsert(captchaDataSource)
                .withTableName("captcha")
                .usingColumns("cp_captcha_id", "cp_answer", "cp_enabled", "cp_type", "cp_creation_date");
    }

    public Captcha saveRandomCaptcha() {
        final var captchaId = String.format("%032d", ThreadLocalRandom.current().nextLong(0L, Long.MAX_VALUE));
        final var answer = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 9999));
        captcha.execute(
                Map.of(
                        "cp_captcha_id", captchaId,
                        "cp_answer", answer,
                        "cp_enabled", true,
                        "cp_type", "IMAGE",
                        "cp_creation_date", Timestamp.from(Instant.now())
                )
        );
        return new Captcha(captchaId, answer);
    }
}
