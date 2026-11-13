package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.*;
import com.freightfox.dispatch.exception.DeleteDisabledException;
import com.freightfox.dispatch.service.DispatchService;
import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/dispatch")
@Tag(name = "Freight Dispatch", description = "Capacity-constrained dispatch planning and optional data management")
public class DispatchController {
    private final OrderService orders;
    private final VehicleService vehicles;
    private final DispatchService dispatch;
    @Value("${app.delete-endpoints-enabled:true}") private boolean deleteEndpointsEnabled;
    private void requireDeletesEnabled() { if (!deleteEndpointsEnabled) throw new DeleteDisabledException(); }
    public DispatchController(OrderService orders, VehicleService vehicles, DispatchService dispatch) {
        this.orders = orders; this.vehicles = vehicles; this.dispatch = dispatch;
    }
    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert delivery orders", responses = {
        @ApiResponse(responseCode = "200", description = "Orders accepted"),
        @ApiResponse(responseCode = "400", description = "Invalid order data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "415", description = "Use application/json", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<com.freightfox.dispatch.dto.ApiResponse> upsertOrders(@Valid @RequestBody OrdersRequest request) {
        orders.accept(request);
        return ResponseEntity.ok(com.freightfox.dispatch.dto.ApiResponse.success("Delivery orders accepted."));
    }
    @PostMapping(value = "/vehicles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert fleet vehicles", responses = {
        @ApiResponse(responseCode = "200", description = "Vehicles accepted"),
        @ApiResponse(responseCode = "400", description = "Invalid fleet data", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<com.freightfox.dispatch.dto.ApiResponse> upsertVehicles(@Valid @RequestBody VehiclesRequest request) {
        vehicles.accept(request);
        return ResponseEntity.ok(com.freightfox.dispatch.dto.ApiResponse.success("Vehicle details accepted."));
    }
    @GetMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Plan delivery routes", description = "Default greedy strategy or optional distance-aware 2-opt route reorder, preserving all assigned freight.")
    public ResponseEntity<DispatchPlanResponse> getPlan(@RequestParam(defaultValue = "greedy") String strategy) {
        return ResponseEntity.ok(dispatch.plan(strategy));
    }
    @GetMapping("/orders")
    @Operation(summary = "List stored delivery orders (paginated)")
    public PageResponse<OrderInput> listOrders(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String priority) { return orders.list(page, size, priority); }
    @GetMapping("/orders/{id}")
    @Operation(summary = "Retrieve an order by ID")
    public OrderInput getOrder(@PathVariable String id) { return orders.get(id); }
    @DeleteMapping("/orders/{id}")
    @Operation(summary = "Delete an order by ID (demo only)")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) { requireDeletesEnabled(); orders.delete(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/orders")
    @Operation(summary = "Clear orders for repeatable test runs (demo only)")
    public ResponseEntity<Void> clearOrders() { requireDeletesEnabled(); orders.clear(); return ResponseEntity.noContent().build(); }
    @GetMapping("/vehicles")
    @Operation(summary = "List stored fleet vehicles (paginated)")
    public PageResponse<VehicleInput> listVehicles(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) { return vehicles.list(page, size); }
    @GetMapping("/vehicles/{id}")
    @Operation(summary = "Retrieve a vehicle by ID")
    public VehicleInput getVehicle(@PathVariable String id) { return vehicles.get(id); }
    @DeleteMapping("/vehicles/{id}")
    @Operation(summary = "Delete a vehicle by ID (demo only)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable String id) { requireDeletesEnabled(); vehicles.delete(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/vehicles")
    @Operation(summary = "Clear vehicles for repeatable test runs (demo only)")
    public ResponseEntity<Void> clearVehicles() { requireDeletesEnabled(); vehicles.clear(); return ResponseEntity.noContent().build(); }
}
