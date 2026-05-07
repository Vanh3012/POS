package com.restaurant.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.restaurant.repository.TableRepository;

import jakarta.transaction.Transactional;

import com.restaurant.dto.response.TableDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.RestaurantTable;
import com.restaurant.models.enums.TableStatus;
import com.restaurant.dto.request.TableRequestAdmin;
import com.restaurant.dto.request.TableRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {
    private final TableRepository tableRepo;

    public List<TableDTO> getAllTables() {
        return tableRepo.findAll()
                .stream()
                .filter(RestaurantTable::isActive)
                .map(this::toTableDTO)
                .toList();
    }

    public List<TableDTO> getAllTablesAdmin() {
        return tableRepo.findAll()
                .stream()
                .map(this::toTableDTO)
                .toList();
    }

    public TableDTO toTableDTO(RestaurantTable table) {
        return TableDTO.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .floor(table.getFloor())
                .status(table.getStatus() != null ? table.getStatus().name() : null)
                .isActive(table.isActive())
                .build();
    }

    @Transactional
    public TableDTO createTable(TableRequest request) {
        String tableNumber = normalizeTableNumber(request.getTableNumber());
        if (tableRepo.existsByTableNumber(tableNumber)) {
            throw new ApiException("TABLE_EXISTS", HttpStatus.BAD_REQUEST, "Table number already exists");
        }
        RestaurantTable table = toTable(request, tableNumber);
        tableRepo.save(table);
        return toTableDTO(table);
    }

    @Transactional
    public TableDTO updateTable(Long tableId, TableRequestAdmin request) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));

        String tableNumber = normalizeTableNumber(request.getTableNumber());
        if (tableNumber != null && !tableNumber.equals(table.getTableNumber())) {
            if (tableRepo.existsByTableNumber(tableNumber)) {
                throw new ApiException("TABLE_EXISTS", HttpStatus.BAD_REQUEST, "Table number already exists");
            }
            table.setTableNumber(tableNumber);
        }
        if (request.getFloor() != null)
            table.setFloor(request.getFloor());

        if (request.getStatus() != null)
            table.setStatus(request.getStatus());

        if (request.getIsActive() != null)
            applyActive(table, request.getIsActive());

        tableRepo.save(table);
        return toTableDTO(table);
    }

    @Transactional
    public TableDTO updateTableActive(Long tableId, boolean isActive) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        applyActive(table, isActive);
        tableRepo.save(table);
        return toTableDTO(table);
    }

    @Transactional
    public void activeTable(Long tableId) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        table.setActive(true);
        if (table.getStatus() == null) {
            table.setStatus(TableStatus.AVAILABLE);
        }
        tableRepo.save(table);
    }

    @Transactional
    public TableDTO deleteTable(Long tableId) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        applyActive(table, false);
        tableRepo.save(table);
        return toTableDTO(table);
    }

    @Transactional
    public TableDTO updateTableStatus(Long tableId, TableStatus status) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        ensureActive(table);
        table.setStatus(status);
        tableRepo.save(table);
        return toTableDTO(table);
    }

    @Transactional
    public TableDTO toggleTableStatus(Long tableId) {
        RestaurantTable table = tableRepo.findById(tableId)
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        ensureActive(table);

        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.setStatus(TableStatus.USED);
        } else {
            table.setStatus(TableStatus.AVAILABLE);
        }

        tableRepo.save(table);
        return toTableDTO(table);
    }

    private RestaurantTable toTable(TableRequest request, String tableNumber) {
        return RestaurantTable.builder()
                .tableNumber(tableNumber)
                .floor(request.getFloor())
                .status(TableStatus.AVAILABLE)
                .isActive(true)
                .build();
    }

    private String normalizeTableNumber(String tableNumber) {
        return tableNumber == null ? null : tableNumber.trim();
    }

    private void applyActive(RestaurantTable table, boolean active) {
        table.setActive(active);
        if (!active) {
            table.setStatus(TableStatus.AVAILABLE);
        } else if (table.getStatus() == null) {
            table.setStatus(TableStatus.AVAILABLE);
        }
    }

    private void ensureActive(RestaurantTable table) {
        if (!table.isActive()) {
            throw new ApiException("TABLE_INACTIVE", HttpStatus.BAD_REQUEST, "Inactive table cannot change status");
        }
    }
}
