
# Working with the bucket:

install awscli-local that add the ability to specify the AWS api endpoint url.

```
$ pip3 install awscli-local
$ awslocal --version
aws-cli/2.1.14 Python/3.7.3 Linux/5.4.0-67-generic exe/x86_64.ubuntu.18 prompt/off
```

## Option 1 

define a profile in ~/.aws/credentials :

```
[inria]
# minio secrets
aws_access_key_id = AKIAIOSFODNN7EXAMPLE 
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```
and use awslocal with --profile=inria

```bashRiskConfigurationService
awslocal --endpoint-url=http://localhost:9000 --profile=inria s3 ls
```

or

```bash
$ export AWS_PROFILE=inria

awslocal --endpoint-url=http://localhost:9000 s3 ls
```


## Option 2 (don't use it because secret are more exposed)

```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

awslocal --endpoint-url=http://localhost:9000 s3 ls
```
