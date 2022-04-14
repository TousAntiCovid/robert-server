# mocks

Mocks haven a relatively predictable initial configuration.

## captcha

Any request to validate a captcha with an id containg `success` will return a _SUCCESS_ response.

```http request
GET /private/api/v1/captcha/id-captcha-for-success/checkAnswer

{ "result": "SUCCESS" }
```

```
GET /private/api/v1/captcha/id-captcha-for-error/checkAnswer

{ "result": "WRONG" }
```

By default, the endpoint to create captcha returns ids containing `success`, so they will return a _SUCCESS_ response no matter the answser which was given.

```http request
POST /private/api/v1/captcha

{
  "id": "this-captcha-is-made-for-success",
  "captchaId": "this-captcha-is-made-for-success"
}
```


## submission-code-server

Any request to validate a code of length 6, 12 or 36 and starting with `valid` will succeed.

```http request
GET /api/v1/verify?code=validZ

{ "valid": true }
```

```http request
GET /api/v1/verify?code=validZZZZZZZ

{ "valid": true }
```

```http request
GET /api/v1/verify?code=validB32-7552-44C1-B98A-DDE5F75B1729

{ "valid": true }
```

Any other request will fail.
```http request
GET /api/v1/verify?code=AZERTY

{ "valid": false }
```

## push-notif-server

Any registration request with a token starting with `valid` request will result in a _created_ response.
```http request
POST /api/v1/push-token

{
  "token": "valid-token",
  "locale": "fr-FR",
  "timezone": "Europe/Paris"
}

201 Created
```

Registration requests malformed or with a token not starting with `valid` request will result in a _bad request_ response.
```http request
POST /api/v1/push-token

{
  "token": "bad-token",
  "locale": "fr-FR",
  "timezone": "Europe/Paris"
}

400 Bad Request
```

Any request with a token starting with `valid` request will result in an _accepted_ response.
```http request
DELETE /api/v1/push-token/valid-token

202 Accepted
```

Any other request will fail.
```http request
DELETE /api/v1/push-token/some-token

400 Bad Request
```
