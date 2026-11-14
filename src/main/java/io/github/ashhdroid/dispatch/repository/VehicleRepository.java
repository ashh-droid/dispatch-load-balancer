package io.github.ashhdroid.dispatch.repository;

import io.github.ashhdroid.dispatch.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {}
