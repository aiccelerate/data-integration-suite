# AICCELERATE Data Integration Suite

<p align="center">
  <a href="https://www.aiccelerate.eu" target="_blank"><img width="400" src="https://aiccelerate.eu/wp-content/themes/sozpicbase/img/svg/logo.svg" alt="AICCELERATE logo"></a>
</p>

<p align="center">
  <a href="https://github.com/aiccelerate/data-integration-suite"><img src="https://img.shields.io/github/license/aiccelerate/data-integration-suite" alt="License"></a>
</p>

## About

This repository includes the mapping definitions for integration of source data of data owner organizations 
to [AICCELERATE Common Data Model](https://github.com/aiccelerate/common-data-model). For testing purposes, 
this repository also contains synthetic data in the format/model of the source datasets.
In AICCELERATE, the use of the Smart Hospital Care Pathway (SHCP) toolset is demonstrated in three pilots: 

(i) patient flow management in elective and urgent surgical cases and
perioperative care, 

(ii) a digital care pathway focused on Parkinson’s disease,

(iii) paediatric service delivery and patient flow management.

Therefore, there are three pilot mapping definitions for the SHCP toolset in this repository. 
Similarly, mappings jobs and schemas are provided for the three pilots.
The mapping definitions for the 3rd pilot are divided into multiple phases.
Data Integration Suite uses toFHIR to generate the FHIR resources based on the mapping definitions.
You can get more information about how mappings and mapping jobs are defined from [toFHIR](https://github.com/srdc/tofhir).

## Deployment

For demonstration, a toFHIR instance can be run with the mappings defined in this project. A docker compose file is
provided to start up an onFHIR instance bundled with [AICCELERATE Common Data Model](https://github.com/aiccelerate/common-data-model)
and a toFHIR instance. You can configure the mapping job to run from `docker-compose.yml` file under `docker` folder.

```
docker-compose -f ./data-integration-suite/docker/docker-compose.yml --project-directory ./:q
 -p aic up -d
```
