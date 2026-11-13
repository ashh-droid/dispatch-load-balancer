package com.freightfox.dispatch.service;

import com.freightfox.dispatch.domain.Order;
import com.freightfox.dispatch.domain.Priority;
import com.freightfox.dispatch.dto.OrderInput;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.PageResponse;
import com.freightfox.dispatch.exception.DuplicateIdException;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.exception.ResourceNotFoundException;
import com.freightfox.dispatch.repository.BatchUpsertRepository;
import com.freightfox.dispatch.repository.OrderRepository;
import java.util.HashSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final BatchUpsertRepository upserts;
    private final OrderRepository repository;
    public OrderService(BatchUpsertRepository upserts, OrderRepository repository) {
        this.upserts = upserts; this.repository = repository;
    }
    @Transactional
    public void accept(OrdersRequest request) {
        var seen = new HashSet<String>();
        for (OrderInput order : request.orders()) {
            if (!seen.add(order.orderId()))
                throw new DuplicateIdException("Duplicate orderId in request: " + order.orderId());
        }
        upserts.upsertOrders(request.orders());
    }
    @Transactional(readOnly = true)
    public PageResponse<OrderInput> list(int page, int size, String priority) {
        var pageable = PageRequest.of(page, size, Sort.by("orderId").ascending());
        var results = priority == null ? repository.findAll(pageable)
                : repository.findByPriority(parsePriority(priority), pageable);
        return new PageResponse<>(results.getContent().stream().map(OrderService::toDto).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }
    @Transactional(readOnly = true)
    public OrderInput get(String id) {
        return repository.findById(id).map(OrderService::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
    @Transactional public void delete(String id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Order not found: " + id);
        repository.deleteById(id);
    }
    @Transactional public void clear() { repository.deleteAllInBatch(); }
    private static OrderInput toDto(Order order) {
        return new OrderInput(order.getOrderId(), order.getLatitude(), order.getLongitude(), order.getAddress(),
                order.getPackageWeight(), order.getPriority());
    }
    private static Priority parsePriority(String input) {
        try { return Priority.valueOf(input.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new InvalidRequestException("priority must be one of HIGH, MEDIUM, LOW"); }
    }
}
