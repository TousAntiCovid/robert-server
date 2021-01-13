# TAC-W scenarios

A few helper scripts to semi-interactively run scenarios in absence of a client.

## Running the scenarios

- Ensure that system requirements described in the `apt.txt` file are installed
- Ensure that the backend is running
- Ensure the following env variables are set 
    ```
    ##### End points
    export ROBERT_BASE_URL=http://localhost/api
    export TACW_BASE_URL=https://localhost/api
  
    ##### API Versions 
    export ROBERT_VERSION=v4
    export CAPTCHA_ROBERT_VERSION=v2
    export TACW_VERSION=v1

    ##### Configuration parameters
    export SALT_RANGE=2
    export TIME_ROUNDING=900
    export NUMBER_OF_VISITS_TO_REPORT=5
    export NB_OF_RETENTION_DAY=12
  
    ##### User 0 for FALSE and 1 for TRUE
    export USE_CAPTCHA=1
    export READ_REPORT_QR_CODE_FROM_USER=0
    ```

- Run the scenarios (e.g. `./01-warning.sh`)

## Debugging

Run `bash -x ./01-warning.sh` (or any other scenario) to display an execution trace.