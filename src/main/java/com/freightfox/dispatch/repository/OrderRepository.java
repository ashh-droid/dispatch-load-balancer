package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.domain.Order;
import com.freightfox.dispatch.domain.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByPriority(Priority priority, Pageable pageable);
}
