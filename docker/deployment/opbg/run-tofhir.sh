#!/bin/bash

docker run -d \
    -v ./data-integration-suite:/usr/local/tofhir/conf \
    -v ./tofhir-logs:/usr/local/tofhir/logs \
    --network=aic_net \
    --env APP_CONF_FILE=/usr/local/tofhir/conf/docker/tofhir.conf \
    --env LOGBACK_CONF_FILE=/usr/local/tofhir/conf/docker/logback.conf \
    --env CONTEXT_PATH=conf \
    --env DATA_FOLDER_PATH=data \
    --env FHIR_REPO_URL=http://onfhir:8080/fhir \
    --name tofhir srdc/tofhir:latest \
    run --job /usr/local/tofhir/conf/mapping-jobs/pilot3-p1/pilot3-p1-mappingjob-deploy.json
