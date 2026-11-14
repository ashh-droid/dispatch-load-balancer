package io.github.ashhdroid.dispatch.service;

import io.github.ashhdroid.dispatch.domain.Vehicle;
import io.github.ashhdroid.dispatch.dto.VehicleInput;
import io.github.ashhdroid.dispatch.dto.VehicleView;
import io.github.ashhdroid.dispatch.dto.VehiclesRequest;
import io.github.ashhdroid.dispatch.dto.PageResponse;
import io.github.ashhdroid.dispatch.exception.DuplicateIdException;
import io.github.ashhdroid.dispatch.exception.ResourceNotFoundException;
import io.github.ashhdroid.dispatch.repository.BatchUpsertRepository;
import io.github.ashhdroid.dispatch.repository.VehicleRepository;
import java.util.HashSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    private final BatchUpsertRepository upserts;
    private final VehicleRepository repository;
    public VehicleService(BatchUpsertRepository upserts, VehicleRepository repository) {
        this.upserts = upserts; this.repository = repository;
    }
    @Transactional
    public void accept(VehiclesRequest request) {
        var seen = new HashSet<String>();
        for (VehicleInput vehicle : request.vehicles()) {
            if (!seen.add(vehicle.vehicleId()))
                throw new DuplicateIdException("Duplicate vehicleId in request: " + vehicle.vehicleId());
        }
        upserts.upsertVehicles(request.vehicles());
    }
    @Transactional(readOnly = true)
    public PageResponse<VehicleView> list(int page, int size) {
        var results = repository.findAll(PageRequest.of(page, size, Sort.by("vehicleId").ascending()));
        return new PageResponse<>(results.getContent().stream().map(VehicleService::toDto).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }
    @Transactional(readOnly = true)
    public VehicleView get(String id) {
        return repository.findById(id).map(VehicleService::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }
    @Transactional public void delete(String id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Vehicle not found: " + id);
        repository.deleteById(id);
    }
    @Transactional public void clear() { repository.deleteAllInBatch(); }
    private static VehicleView toDto(Vehicle vehicle) {
        return new VehicleView(vehicle.getVehicleId(), vehicle.getCapacity(), vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(), vehicle.getCurrentAddress());
    }
}
