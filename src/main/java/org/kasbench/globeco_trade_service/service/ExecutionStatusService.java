package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import java.util.List;
import java.util.Optional;

public interface ExecutionStatusService {
    List<ExecutionStatus> getAllExecutionStatuses();
    Optional<ExecutionStatus> getExecutionStatusById(Integer id);
    ExecutionStatus createExecutionStatus(ExecutionStatus executionStatus);
    ExecutionStatus updateExecutionStatus(Integer id, ExecutionStatus executionStatus);
    void deleteExecutionStatus(Integer id, Integer version);
} 