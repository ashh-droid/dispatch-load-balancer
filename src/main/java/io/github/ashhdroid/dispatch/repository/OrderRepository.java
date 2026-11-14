package io.github.ashhdroid.dispatch.repository;

import io.github.ashhdroid.dispatch.domain.Order;
import io.github.ashhdroid.dispatch.domain.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByPriority(Priority priority, Pageable pageable);
}
