package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.service.ExecutionService.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionService.ExecutionSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionService.SubmitResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the ExecutionService interface enhancements for bulk methods
 */
public class ExecutionServiceInterfaceTest {

    @Test
    public void testExecutionSubmitResultClass() {
        // Test default constructor
        ExecutionSubmitResult result1 = new ExecutionSubmitResult();
        assertNull(result1.getExecutionId());
        assertNull(result1.getStatus());
        assertNull(result1.getMessage());
        assertNull(result1.getExecutionServiceId());

        // Test constructor with basic fields
        ExecutionSubmitResult result2 = new ExecutionSubmitResult(123, "SUCCESS", "Execution submitted successfully");
        assertEquals(Integer.valueOf(123), result2.getExecutionId());
        assertEquals("SUCCESS", result2.getStatus());
        assertEquals("Execution submitted successfully", result2.getMessage());
        assertNull(result2.getExecutionServiceId());

        // Test constructor with all fields
        ExecutionSubmitResult result3 = new ExecutionSubmitResult(456, "SUCCESS", "Execution submitted successfully", 789);
        assertEquals(Integer.valueOf(456), result3.getExecutionId());
        assertEquals("SUCCESS", result3.getStatus());
        assertEquals("Execution submitted successfully", result3.getMessage());
        assertEquals(Integer.valueOf(789), result3.getExecutionServiceId());

        // Test setters
        result1.setExecutionId(999);
        result1.setStatus("FAILED");
        result1.setMessage("Execution failed");
        result1.setExecutionServiceId(111);

        assertEquals(Integer.valueOf(999), result1.getExecutionId());
        assertEquals("FAILED", result1.getStatus());
        assertEquals("Execution failed", result1.getMessage());
        assertEquals(Integer.valueOf(111), result1.getExecutionServiceId());
    }

    @Test
    public void testBulkSubmitResultClass() {
        List<ExecutionSubmitResult> results = Arrays.asList(
            new ExecutionSubmitResult(1, "SUCCESS", "OK"),
            new ExecutionSubmitResult(2, "FAILED", "Error")
        );

        // Test default constructor
        BulkSubmitResult bulkResult1 = new BulkSubmitResult();
        assertEquals(0, bulkResult1.getTotalRequested());
        assertEquals(0, bulkResult1.getSuccessful());
        assertEquals(0, bulkResult1.getFailed());
        assertNull(bulkResult1.getResults());
        assertNull(bulkResult1.getOverallStatus());
        assertNull(bulkResult1.getMessage());

        // Test constructor with all fields
        BulkSubmitResult bulkResult2 = new BulkSubmitResult(2, 1, 1, results, "PARTIAL_SUCCESS", "1 of 2 succeeded");
        assertEquals(2, bulkResult2.getTotalRequested());
        assertEquals(1, bulkResult2.getSuccessful());
        assertEquals(1, bulkResult2.getFailed());
        assertEquals(results, bulkResult2.getResults());
        assertEquals("PARTIAL_SUCCESS", bulkResult2.getOverallStatus());
        assertEquals("1 of 2 succeeded", bulkResult2.getMessage());

        // Test setters
        bulkResult1.setTotalRequested(5);
        bulkResult1.setSuccessful(3);
        bulkResult1.setFailed(2);
        bulkResult1.setResults(results);
        bulkResult1.setOverallStatus("PARTIAL_SUCCESS");
        bulkResult1.setMessage("3 of 5 succeeded");

        assertEquals(5, bulkResult1.getTotalRequested());
        assertEquals(3, bulkResult1.getSuccessful());
        assertEquals(2, bulkResult1.getFailed());
        assertEquals(results, bulkResult1.getResults());
        assertEquals("PARTIAL_SUCCESS", bulkResult1.getOverallStatus());
        assertEquals("3 of 5 succeeded", bulkResult1.getMessage());
    }

    @Test
    public void testBackwardCompatibilityWithSubmitResult() {
        // Verify that the existing SubmitResult class is still available and functional
        SubmitResult result = new SubmitResult();
        assertNull(result.getStatus());
        assertNull(result.getError());

        SubmitResult result2 = new SubmitResult("SUCCESS", null);
        assertEquals("SUCCESS", result2.getStatus());
        assertNull(result2.getError());

        SubmitResult result3 = new SubmitResult("FAILED", "Network error");
        assertEquals("FAILED", result3.getStatus());
        assertEquals("Network error", result3.getError());

        result.setStatus("PENDING");
        result.setError("Timeout");
        assertEquals("PENDING", result.getStatus());
        assertEquals("Timeout", result.getError());
    }

    @Test
    public void testExecutionServiceImplHasBulkMethods() {
        // This test verifies that ExecutionServiceImpl implements the new bulk methods
        // by checking that it can be instantiated and the methods exist (even if they throw UnsupportedOperationException)
        
        // We can't easily instantiate ExecutionServiceImpl in a unit test due to dependencies,
        // but we can verify the interface contract exists by checking method signatures exist
        // This is implicitly tested by the compilation success
        
        // Verify the interface has the expected method signatures
        try {
            ExecutionService.class.getMethod("submitExecutions", List.class);
            ExecutionService.class.getMethod("submitExecutionsBatch", List.class, int.class);
            ExecutionService.class.getMethod("submitExecution", Integer.class);
        } catch (NoSuchMethodException e) {
            fail("Expected method not found in ExecutionService interface: " + e.getMessage());
        }
    }
}