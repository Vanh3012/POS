package com.restaurant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.restaurant.dto.request.CloseOrderRequest;
import com.restaurant.dto.request.CreateOrderItemRequest;
import com.restaurant.dto.request.CreateOrderRequest;
import com.restaurant.dto.response.CashierViewDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.dto.response.OrderDTO;
import com.restaurant.dto.response.OrderItemDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.Category;
import com.restaurant.models.entity.MenuItem;
import com.restaurant.models.entity.Order;
import com.restaurant.models.entity.OrderItem;
import com.restaurant.models.entity.Payment;
import com.restaurant.models.entity.RestaurantTable;
import com.restaurant.models.entity.User;
import com.restaurant.models.enums.OrderStatus;
import com.restaurant.models.enums.OrderType;
import com.restaurant.models.enums.PaymentMethod;
import com.restaurant.models.enums.PaymentStatus;
import com.restaurant.models.enums.TableStatus;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.TableRepository;
import com.restaurant.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashierService {
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(11);
    private static final BigDecimal TAX_MULTIPLIER = BigDecimal.valueOf(0.11);

    private final CategoryRepository categoryRepo;
    private final MenuItemRepository menuItemRepo;
    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final TableRepository tableRepo;
    private final UserRepository userRepo;
    private final TableService tableService;

    // trang view
    // -----------------------------------------------------------------------------------------
    @Transactional
    public CashierViewDTO getCashierView(String keyword) {
        return CashierViewDTO.builder()
                .categories(getCategories())
                .menuItems(getMenuItems(null, keyword))
                .tables(tableService.getAllTables())
                .orders(getOpenOrders())
                .build();
    }

    @Transactional
    public List<OrderDTO> getOpenOrders() {
        return orderRepo.findByStatusOrderByCreatedAtDesc(OrderStatus.BEING_COOKED)
                .stream()
                .map(this::toOrderDTO)
                .toList();
    }

    // tạo order
    // -----------------------------------------------------------------------------------------
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        User user = getCurrentUser();
        RestaurantTable table = resolveTableForOrder(request);

        Order order = new Order();
        order.setTable(table);
        order.setUser(user);
        order.setCustomerName(blankToNull(request.getCustomerName()));
        order.setOrderType(request.getOrderType());
        order.setNote(blankToNull(request.getNote()));
        order.setStatus(OrderStatus.BEING_COOKED);
        order.setTaxRate(TAX_RATE);

        BigDecimal subTotal = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepo.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                            "Khong tim thay Menu Item"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(money(menuItem.getPrice()));
            orderItem.setNote(blankToNull(itemRequest.getNote()));
            order.getOrderItems().add(orderItem);

            subTotal = subTotal.add(lineTotal(orderItem));
        }

        order.setSubTotal(money(subTotal));
        order.setTax(money(subTotal.multiply(TAX_MULTIPLIER)));
        order.setTotalPrice(money(order.getSubTotal().add(order.getTax())));

        BigDecimal amount = order.getTotalPrice();
        BigDecimal received = resolveReceived(request.getPaymentMethod(), request.getReceived(), amount);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(amount);
        payment.setReceived(received);
        payment.setChangeAmount(money(received.subtract(amount)));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaidBy(user);
        order.setPayment(payment);

        Order savedOrder = orderRepo.save(order);
        paymentRepo.save(payment);

        if (table != null) {
            table.setStatus(TableStatus.USED);
            tableRepo.save(table);
        }

        return toOrderDTO(savedOrder);
    }

    // đóng order
    // -----------------------------------------------------------------------------------------
    @Transactional
    public OrderDTO closeOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay Order"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ApiException("ORDER_ALREADY_CLOSED", HttpStatus.BAD_REQUEST, "Order da duoc close");
        }

        order.setStatus(OrderStatus.DELIVERED);

        if (order.getTable() != null) {
            order.getTable().setStatus(TableStatus.AVAILABLE);
            tableRepo.save(order.getTable());
        }

        return toOrderDTO(orderRepo.save(order));
    }

    // lấy categories
    // -----------------------------------------------------------------------------------------
    @Transactional
    public List<CategoryDTO> getCategories() {
        return categoryRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCategoryDTO)
                .toList();
    }

    // menu items
    // -----------------------------------------------------------------------------------------
    @Transactional
    public List<MenuItemDTO> getMenuItems(Long categoryId, String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();

        return menuItemRepo.findAll()
                .stream()
                .filter(item -> categoryId == null
                        || (item.getCategory() != null && categoryId.equals(item.getCategory().getId())))
                .filter(item -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || item.getName().toLowerCase().contains(normalizedKeyword))
                .sorted(Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMenuItemDTO)
                .toList();
    }

    @Transactional
    public MenuItemDTO getMenuItem(Long menuItemId) {
        MenuItem menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Menu Item"));
        return toMenuItemDTO(menuItem);
    }

    private CategoryDTO toCategoryDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .menuItemIds(category.getMenuItems()
                        .stream()
                        .map(MenuItem::getId)
                        .toList())
                .build();
    }

    private MenuItemDTO toMenuItemDTO(MenuItem menuItem) {
        return MenuItemDTO.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .description(menuItem.getDescription())
                .categoryId(menuItem.getCategory() != null ? menuItem.getCategory().getId() : null)
                .categoryName(menuItem.getCategory() != null ? menuItem.getCategory().getName() : null)
                .build();
    }

    // order
    // -----------------------------------------------------------------------------------------
    private OrderDTO toOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getCustomerName())
                .subTotal(order.getSubTotal())
                .taxRate(order.getTaxRate())
                .tax(order.getTax())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .note(order.getNote())
                .paymentMethod(order.getPayment() != null && order.getPayment().getPaymentMethod() != null
                        ? order.getPayment().getPaymentMethod().name()
                        : null)
                .paymentStatus(order.getPayment() != null && order.getPayment().getStatus() != null
                        ? order.getPayment().getStatus().name()
                        : null)
                .received(order.getPayment() != null ? order.getPayment().getReceived() : null)
                .changeAmount(order.getPayment() != null ? order.getPayment().getChangeAmount() : null)
                .createdAt(order.getCreatedAt())
                .items(order.getOrderItems()
                        .stream()
                        .map(this::toOrderItemDTO)
                        .toList())
                .build();
    }

    private OrderItemDTO toOrderItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem().getId())
                .menuItemName(item.getMenuItem().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(lineTotal(item))
                .note(item.getNote())
                .build();
    }

    private RestaurantTable resolveTableForOrder(CreateOrderRequest request) {
        if (request.getOrderType() != OrderType.DINE_IN) {
            return null;
        }

        if (request.getTableId() == null) {
            throw new ApiException("TABLE_REQUIRED", HttpStatus.BAD_REQUEST, "Dine-in order must select table");
        }

        RestaurantTable table = tableRepo.findById(request.getTableId())
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));

        if (!table.isActive()) {
            throw new ApiException("TABLE_INACTIVE", HttpStatus.BAD_REQUEST, "Inactive table cannot create order");
        }

        if (table.getStatus() == TableStatus.USED) {
            throw new ApiException("TABLE_IN_USE", HttpStatus.BAD_REQUEST, "Table is already in use");
        }

        return table;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ApiException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException exception) {
            throw new ApiException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return userRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));
    }

    private BigDecimal resolveReceived(PaymentMethod paymentMethod, BigDecimal received, BigDecimal amount) {
        BigDecimal resolved = paymentMethod == PaymentMethod.CASH
                ? received
                : amount;

        if (resolved == null) {
            throw new ApiException("RECEIVED_REQUIRED", HttpStatus.BAD_REQUEST, "Received amount is required");
        }

        resolved = money(resolved);
        if (resolved.compareTo(amount) < 0) {
            throw new ApiException("INSUFFICIENT_PAYMENT", HttpStatus.BAD_REQUEST,
                    "Received amount is less than total");
        }

        return resolved;
    }

    private BigDecimal lineTotal(OrderItem item) {
        return money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
