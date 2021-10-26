package e2e.context;


import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {

    private final String name;

    private String captchaId;

    private String captchaSolution;
}
