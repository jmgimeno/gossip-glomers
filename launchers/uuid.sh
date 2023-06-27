#!/bin/bash

# http://mywiki.wooledge.org/BashFAQ/028
if [[ $BASH_SOURCE = */* ]]; then
    DIR=${BASH_SOURCE%/*}/
else
    DIR=./
fi

exec java -Xmx256M -cp "$DIR/../target/scala-3.3.0/gossip-glomers-assembly-0.1.0-SNAPSHOT.jar" "uuid.Main"