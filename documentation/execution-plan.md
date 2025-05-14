# Step-by-Step Instructions

Please perform each step when instructed.  Only perform one step at a time.

Within these instructions, use the following table to map between table names, resource names, and class names.

| table name | resource name | class name |
| --- | --- | --- |
| trade_order | tradeOrder | TradeOrder |
| execution | execution | Execution |
| blotter | blotter | Blotter |
| trade_block | tradeBlock | TradeBlock |
| trade_block_allocation | tradeBlockAllocation | TradeBlockAllocation |
| trade_type | tradeType | TradeType |
| trade_status | tradeStatus | TradeStatus |
| execution_status | executionStatus | ExecutionStatus |
---
<br>

## Steps

1. Configure the project to connect to the PostgreSQL database on host `globeco-trade-service-postgresql`  port 32800 and database `postgres`.  The user is  "postgres".  No password is required. Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.  Do not delete or replace anything in cursor-log.md
2. Configure Flyway with the same configuration as in step 1.  Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.
3. Create a Flyway migration to deploy the schema for this project.  The schema is in @trade-service.sql in the project root.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
4. Create a Flyway migration for the blotter data in #### Initialization Data for `blotter`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
5. Create a Flyway migration for the blotter data in #### Initialization Data for `trade_type`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
6. Create a Flyway migration for the blotter data in #### Initialization Data for `trade_status`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
6. Create a Flyway migration for the blotter data in #### Initialization Data for `execution_status`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

1. Security data comes from the security service.  The security service API is implemented in documentation/security-service-openapi.yaml.  Please generate the entity, service interface, and service implementation to call the security service to GET securities and security types.  Do not implement POST, PUT, or DELETE.  Please implement caching for the security data to avoid excessive calls to the security service following the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **blotter**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **blotter** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **blotter** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **blotter** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with an introduction and full documentation of the **blotter** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please create an OpenAPI schema `openapi.yaml' in the project root.  Please include the full specification for the **blotter** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

1. Please implement the entity, repository, service interface, and service implementation for **tradeOrder** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeOrder**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **tradeOrder** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **tradeOrder** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **tradeOrder** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **tradeOrder** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **tradeOrder** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **tradeOrder** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **execution** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **execution**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **execution** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **execution** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **execution** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **execution** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **execution** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **execution** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **tradeBlock** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeBlock**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **tradeBlock** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **tradeBlock** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **tradeBlock** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **tradeBlock** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **tradeBlock** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **tradeBlock** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **tradeBlockAllocation** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeBlockAllocation**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **tradeBlockAllocation** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **tradeBlockAllocation** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **tradeBlockAllocation** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **tradeBlockAllocation** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **tradeBlockAllocation** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **tradeBlockAllocation** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **tradeType** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeType**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **tradeType** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **tradeType** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **tradeType** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **tradeType** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **tradeType** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **tradeType** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **tradeStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeStatus**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **tradeStatus** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **tradeStatus** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **tradeStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **tradeStatus** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **tradeStatus** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **tradeStatus** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.



1. Please implement the entity, repository, service interface, and service implementation for **executionStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **executionStatus**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **executionStatus** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **executionStatus** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **executionStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **executionStatus** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **executionStatus** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **executionStatus** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

1. Please compare the README.md file to the code to verify that the README is completely consistent with the code.  If it is not, please make the required updates. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
1. Please compare the openapi.yaml file to the code to verify that the openapi spec is completely consistent with the code.  If it is not, please make the required updates. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
23. Please create a Dockerfile for this application.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
24. We will be deploying this service to Kubernetes.  Please implement liveness, readiness, and startup health checks.  Please update the README.md file and openapi.yaml spec with the health check APIs.  Please be sure the URLs in the spec match the URLs in the API. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
25. Please create all the files necessary to deploy to this application as a service to Kubernetes.  Please include the liveness, readiness, and startup probes you just created.  The deployment should start with one instance of the service and should scale up to a maximum of 100 instances.  It should have up 100 millicores and 200 MiB of memory.  The liveness probe should have a timeout (`timeoutSeconds`) of 240 seconds.  The name of the service is `globeco-trade-service` in the `globeco` namespace.  You do not need to create the namespace. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
26. Please expose the OpenAPI schema as an endpoint using Springdoc OpenAPI.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
