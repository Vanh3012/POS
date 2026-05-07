package com.restaurant.controller;

import org.springframework.web.bind.annotation.RestController;

import com.restaurant.dto.response.TableDTO;
import com.restaurant.dto.request.TableRequest;
import com.restaurant.service.TableService;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.restaurant.dto.request.TableRequestAdmin;

import com.restaurant.models.enums.TableStatus;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {
    private final TableService tableService;

    @GetMapping
    public List<TableDTO> getAllTables() {
        return tableService.getAllTables();
    }

    @GetMapping("/admin")
    public List<TableDTO> getAllTablesAdmin() {
        return tableService.getAllTablesAdmin();
    }

    @PostMapping
    public TableDTO createTable(@Valid @RequestBody TableRequest request) {
        return tableService.createTable(request);
    }

    @PutMapping("/{tableId}")
    public TableDTO updateTable(@PathVariable Long tableId, @Valid @RequestBody TableRequestAdmin request) {
        return tableService.updateTable(tableId, request);
    }

    @PatchMapping("/{tableId}/status")
    public TableDTO updateTableStatus(@PathVariable Long tableId, @RequestParam TableStatus status) {
        return tableService.updateTableStatus(tableId, status);
    }

    @PatchMapping("/{tableId}/toggle-status")
    public TableDTO toggleTableStatus(@PathVariable Long tableId) {
        return tableService.toggleTableStatus(tableId);
    }

    @PatchMapping("/{tableId}/active")
    public TableDTO updateTableActive(@PathVariable Long tableId, @RequestParam boolean isActive) {
        return tableService.updateTableActive(tableId, isActive);
    }

    @DeleteMapping("/{tableId}")
    public void deleteTable(@PathVariable Long tableId) {
        tableService.deleteTable(tableId);
    }
}
