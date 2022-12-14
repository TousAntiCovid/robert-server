package fr.gouv.stopc.e2e.mobileapplication.repository;

import fr.gouv.stopc.e2e.mobileapplication.repository.model.Captcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class PostgresCaptchaRepository implements CaptchaRepository {

    private final SimpleJdbcInsert captcha;

    public PostgresCaptchaRepository(final DataSource captchaDataSource) {
        captcha = new SimpleJdbcInsert(captchaDataSource)
                .withTableName("captcha")
                .usingColumns("cp_captcha_id", "cp_answer", "cp_enabled", "cp_type", "cp_creation_date");
    }

    @Override
    public Captcha saveRandomCaptcha() {
        final var captchaId = String.format("%032d", ThreadLocalRandom.current().nextLong(0L, Long.MAX_VALUE));
        final var answer = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 9999));
        log.debug("Injecting captcha id={} answer={}", captchaId, answer);
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
