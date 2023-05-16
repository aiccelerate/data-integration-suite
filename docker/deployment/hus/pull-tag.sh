#!/bin/bash

docker pull hfdtu3132022aiccelparkprivatecr.azurecr.io/mongo:4.4
docker tag hfdtu3132022aiccelparkprivatecr.azurecr.io/mongo:4.4 mongo:4.4

docker pull hfdtu3132022aiccelparkprivatecr.azurecr.io/srdc/onfhir:r4
docker tag hfdtu3132022aiccelparkprivatecr.azurecr.io/srdc/onfhir:r4 srdc/onfhir:r4

docker pull hfdtu3132022aiccelparkprivatecr.azurecr.io/srdc/tofhir:latest
docker tag  hfdtu3132022aiccelparkprivatecr.azurecr.io/srdc/tofhir:latest srdc/tofhir:latest
