package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static com.nimbleways.springboilerplate.entities.Product.ProductType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testng.Assert.assertThrows;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private ProductService productService;
    @InjectMocks
    private OrderService orderService;

    @Test
    public void testNotifDelay() {
        // GIVEN
        Product product = new Product(null, 15, 0, NORMAL, "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void testProcessOrder() {
        // GIVEN
        Product product1 = new Product(null, 15, 2, NORMAL, "RJ45 Cable", null, null, null);
        Product product2 = new Product(null, 20, 3, SEASONAL, "Watermelon", null, LocalDate.now().minusDays(5), LocalDate.now().plusDays(60));
        Product product3 = new Product(null, 20, 3, EXPIRABLE, "Yogurt", LocalDate.now().plusDays(15), LocalDate.now().minusDays(5), LocalDate.now().plusDays(60));

        Mockito.when(productRepository.save(product1)).thenReturn(product1);
        Mockito.when(productRepository.save(product2)).thenReturn(product2);
        Mockito.when(productRepository.save(product3)).thenReturn(product3);

        Set<Product> set = new HashSet<>();
        set.add(product1);
        set.add(product2);
        set.add(product3);
        Order order = new Order(1L, set);

        Mockito.when(orderRepository.save(order)).thenReturn(order);

        Mockito.when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        // WHEN
        orderService.processOrder(1L);

        // THEN
        assertEquals(1, product1.getAvailable());
        assertEquals(2, product2.getAvailable());
        assertEquals(2, product3.getAvailable());
        Mockito.verify(orderRepository, Mockito.times(1)).findById(1L);
    }

    @Test
    public void handleSeasonalAndExpired() {
        // GIVEN
        Product product2 = new Product(null, 20, 3, SEASONAL, "Watermelon", null, LocalDate.now().plusDays(15), LocalDate.now().plusDays(60));
        Product product3 = new Product(null, 20, 3, EXPIRABLE, "Yogurt", LocalDate.now().plusDays(15), LocalDate.now().minusDays(5), LocalDate.now().plusDays(60));

        Mockito.when(productRepository.save(product2)).thenReturn(product2);
        Mockito.when(productRepository.save(product3)).thenReturn(product3);

        Set<Product> set = new HashSet<>();
        set.add(product2);
        set.add(product3);
        Order order = new Order(1L, set);

        Mockito.when(orderRepository.save(order)).thenReturn(order);

        Mockito.when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        // WHEN
        productService.handleSeasonalProduct(product2);
        productService.handleExpiredProduct(product3);

        // THEN
        assertEquals(2, product3.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product2);
    }

    @Test
    public void testProcessOrderExpirableExpired() {
        // GIVEN
        Product product2 = new Product(null, 20, 3, EXPIRABLE, "Yogurt", LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(10));

        Mockito.when(productRepository.save(product2)).thenReturn(product2);

        Set<Product> set = new HashSet<>();
        set.add(product2);
        Order order = new Order(1L, set);

        Mockito.when(orderRepository.save(order)).thenReturn(order);

        Mockito.when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        // WHEN
        productService.handleExpiredProduct(product2);

        // THEN
        assertEquals(0, product2.getAvailable());
    }

    @Test
    public void shouldThrowExceptionWhenNoOrder() {
        // GIVEN
        Mockito.when(orderRepository.findById(2L))
                .thenReturn(Optional.empty());

        // WHEN

        // THEN
        assertThrows(NoSuchElementException.class, () -> {
            orderService.processOrder(2L);
        });
    }
}