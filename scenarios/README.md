# TAC-W scenarios

A few helper scripts to semi-interactively run scenarios in absence of a client.

## Running the scenarios

- Ensure that system requirements described in the `apt.txt` file are installed
- Ensure that the backend is running
- ensure the following env variables are set 
    ```
    export ROBERT_BASE_URL=http://localhost/api
    export ROBERT_VERSION=v4
    export TACW_BASE_URL=https://localhost/api
    export TACW_VERSION=v1
    export SALT_RANGE=1000
    export TIME_ROUNDING=900
    export USE_CAPTCHA=true
    ```

- Run the scenarios (e.g. `./01-warning.sh`)

## Debugging

Run `bash -x ./01-warning.sh` (or any other scenario) to display an execution trace.