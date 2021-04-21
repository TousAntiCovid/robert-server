# clea-batch unit tests with bats

Bats documentation : https://bats-core.readthedocs.io/en/latest/

This script is suppose to be execute, by [bats](https://github.com/bats-core/bats-core), from the root level of "clea" project.

Those tests check the batch itself, not the functionnality of the java batch, neither the command s3cmd. java and s3cmd can return 0 (success) or <>0 (fails) 

## Dependencies
This module needs bats-core, bats-support and bats-assert.

From the root folder:

```bash
git clone https://github.com/bats-core/bats-core target/bats/core
git clone https://github.com/ztombol/bats-support.git target/bats/support
git clone https://github.com/ztombol/bats-assert.git target/bats/assert
```

## usage 
Example of execution from "clea" root folder

```bash
$ docker run -it --rm -v $PWD:/app --workdir=/app bats/bats  src/test/bats
1..9
ok 1 Should fail if no BUCKET env
ok 2 Should fail on Java failure
ok 3 Should fail if not exists output dir
ok 4 Should fail if not enough generated files
ok 5 Should fail if no Cluster index
ok 6 Should fail if S3cmd copy of cluster files returns error
ok 7 Should fail if S3cmd copy of cluster index returns error
ok 8 Should succeds with presence of ClusterIndex
ok 9 Succeeds to purge temporary files at end of batch
$
```

same result with 

```bash
$ target/bats/core/bin/bats src/test/bats
 ✓ Should fail if no BUCKET env
 ✓ Should fail on Java failure
 ✓ Should fail if not exists output dir
 ✓ Should fail if not enough generated files
 ✓ Should fail if no Cluster index
 ✓ Should fail if S3cmd copy of cluster files returns error
 ✓ Should fail if S3cmd copy of cluster index returns error
 ✓ Should succeds with presence of ClusterIndex
 ✓ Succeeds to purge temporary files at end of batch

9 tests, 0 failures
$
```