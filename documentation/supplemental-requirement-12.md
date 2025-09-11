# Supplemental Requirement 12 - Posts to Execution Service in Bulk

Currently, this service (the Globeco Trade Service) posts executions to the GlobeCo Execution Service one-by-one.  This creates enormous overhead and leads to missed SLAs and failures.  It is especially problematic under load.  The actual call is made in ExecutionServiceImpl.submitExecution, which is passed a single execution id.  This method needs to be replaced with a method that calls the execution service in bulk, using the Execution Service's POST /api/v1/executions/batch API (see [the execution service openapi spec](execution-service-openapi.yaml)).  This method currently calls POST /api/v1/executions one-by-one.  All places that call the ExecutionService.submitExecution method now will have to be redesigned to submit in bulk.  Bulk submissions should be limited to 100 executions, which is the API limit.  That amount should be configurable.

Error handling should mimic the Execution Service API.  A similar level of detail should be returned.  Retry logic should be based on the results.

The goal of this enhancement is to improve performance.