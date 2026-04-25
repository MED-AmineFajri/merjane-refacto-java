package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class OrderService {
    private final ProductRepository productRepository;

    private final ProductService productService;

    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, ProductService productService, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.orderRepository = orderRepository;
    }

    public ProcessOrderResponse processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        System.out.println(order);

        Set<Product> products = order.getItems();
        for (Product p : products) {
            processProduct(p);
        }
        return new ProcessOrderResponse(order.getId());
    }

    private void processProduct(Product product) {

        switch (product.getType()) {

            case NORMAL:
                handleNormal(product);
                break;

            case SEASONAL:
                handleSeasonal(product);
                break;

            case EXPIRABLE:
                handleExpirable(product);
                break;
        }
    }

    private void handleNormal(Product product) {
        if (product.getAvailable() > 0) {
            decreaseStock(product);
        } else {
            int leadTime = product.getLeadTime();
            if (leadTime > 0) {
                productService.notifyDelay(leadTime, product);
            }
        }
    }

    private void handleSeasonal(Product product) {
        // Add new season rules
        if ((LocalDate.now().isAfter(product.getSeasonStartDate()) && LocalDate.now().isBefore(product.getSeasonEndDate())
                && product.getAvailable() > 0)) {
            decreaseStock(product);
        } else {
            productService.handleSeasonalProduct(product);
        }
    }

    private void handleExpirable(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            decreaseStock(product);
        } else {
            productService.handleExpiredProduct(product);
        }
    }

    private void decreaseStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
