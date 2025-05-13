# Step-by-Step Instructions

Please perform each step when instructed.  Only perform one step at a time.

1. Configure the project to connect to the PostgreSQL database on host `globeco-trade-service-postgresql`  port 32800 and database `postgres`.  The user is  "postgres".  No password is required. Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.  Do not delete or replace anything in cursor-log.md
2. Configure Flyway with the same configuration as in step 1.  Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.
3. Create a Flyway migration to deploy the schema for this project.  The schema is in @trade-service.sql in the project root.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
4. Create a Flyway migration for the blotter data in #### Initialization Data for `blotter`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
5. Create a Flyway migration for the blotter data in #### Initialization Data for `trade_type`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
6. Create a Flyway migration for the blotter data in #### Initialization Data for `trade_status`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
6. Create a Flyway migration for the blotter data in #### Initialization Data for `execution_status`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

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


1. Please implement the entity, repository, service interface, and service implementation for **trade_block** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **trade_block**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **trade_block** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **trade_block** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **trade_block** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **trade_block** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **trade_block** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **trade_block** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **blotter**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **blotter** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **blotter** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **blotter** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **blotter** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **blotter** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **blotter**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **blotter** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **blotter** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **blotter** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **blotter** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **blotter** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


1. Please implement the entity, repository, service interface, and service implementation for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the unit tests for the entity, repository, service interface, and service implementation for **blotter**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please impelement caching for **blotter** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement unit testing for **blotter** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please implement the APIs for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please generate the unit tests for the **blotter** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the README.md file with full documentation of the **blotter** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
1. Please update the OpenAPI schema @openapi.yaml in the project root.  Please include the full specification for the **blotter** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.







11. Please implement the APIs for orderType using the requirements provided in @requirements.md.  Use the code for status as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
12. Please create unit tests for the code you created in the previous step.  Please use the code you created in the previous unit tests as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
13. Please add documentation for the orderType data model and API to readme.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
14. Please update the openapi schema `openapi.yaml` with the spec for orderType.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
15. Please implement the APIs for blotter using the requirements provided in @requirements.md.  Use the code for status as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
16. Please create unit tests for the code you created in the previous step.  Please use the code you created in the previous unit tests as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
17. Please add documentation for the blotter data model and API to readme.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
18. Please update the openapi schema `openapi.yaml` with the spec for blotter.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
19. Please implement the APIs for order using the requirements provided in @requirements.md.  Use the code for status as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
20. Please create unit tests for the code you created in the previous step.  Please use the code you created in the previous unit tests as an example.  Strive for consistency.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
21. Please add documentation for the order data model and API to readme.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
22. Please update the openapi schema `openapi.yaml` with the spec for order.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
23. Please create a Dockerfile for this application.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
24. We will be deploying this service to Kubernetes.  Please implement liveness, readiness, and startup health checks.  Please update the README.md file and openapi.yaml spec with the health check APIs.  Please be sure the URLs in the spec match the URLs in the API. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
25. Please create all the files necessary to deploy to this application as a service to Kubernetes.  Please include the liveness, readiness, and startup probes you just created.  The deployment should start with one instance of the service and should scale up to a maximum of 100 instances.  It should have up 100 millicores and 200 MiB of memory.  The liveness probe should have a timeout (`timeoutSeconds`) of 240 seconds.  The name of the service is `globeco-order-service` in the `globeco` namespace.  You do not need to create the namespace. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
26. Please expose the OpenAPI schema as an endpoint.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
