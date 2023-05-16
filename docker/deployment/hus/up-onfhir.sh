#!/bin/bash

UID_GID="$(id -u):$(id -g)" docker compose -f ./docker-compose-onfhir.yml -p aic up -d
