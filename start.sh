#!/bin/sh
until java -jar target/blockdb-1.0-SNAPSHOT.jar; do
    echo "Server crashed with exit code $?.  Respawning.." >&2
    sleep 1
done

