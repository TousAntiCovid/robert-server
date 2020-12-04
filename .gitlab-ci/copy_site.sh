#!/bin/bash

copy_site () {
   mkdir public/$1
   cp -r $1/target/site/* public/$1
}

copy_site_rename () {
   mkdir public/$2
   cp -r $1/target/site/* public/$2
}

copy_site_rename tac-warning tac-warning-server
copy_site_rename tac-warning/tac-warning-database tac-warning-server/tac-warning-database
copy_site_rename tac-warning/tac-warning-ws-rest tac-warning-server/tac-warning-ws-rest
copy_site robert-server
copy_site robert-server/robert-crypto-grpc-server
copy_site robert-server/robert-crypto-grpc-server-messaging
copy_site robert-server/robert-crypto-grpc-server-storage
copy_site robert-server/robert-server-batch
copy_site robert-server/robert-server-common
copy_site robert-server/robert-server-crypto
copy_site robert-server/robert-server-database
copy_site robert-server/robert-server-ws-rest

 
