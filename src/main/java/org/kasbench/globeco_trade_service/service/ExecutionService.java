package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Execution;
import java.util.List;
import java.util.Optional;

public interface ExecutionService {
    List<Execution> getAllExecutions();
    Optional<Execution> getExecutionById(Integer id);
    Execution createExecution(Execution execution);
    Execution updateExecution(Integer id, Execution execution);
    void deleteExecution(Integer id, Integer version);
} 