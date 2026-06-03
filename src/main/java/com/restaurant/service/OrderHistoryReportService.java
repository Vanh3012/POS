package com.restaurant.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.restaurant.dto.request.CreateOrderItemRequest;
import com.restaurant.dto.request.CreateOrderRequest;
import com.restaurant.dto.response.OrderDTO;
import com.restaurant.dto.response.OrderItemDTO;
import com.restaurant.dto.response.OrderListItemDTO;
import com.restaurant.dto.response.OrderPageDTO;
import com.restaurant.dto.response.ReportDTO;
import com.restaurant.dto.response.ReportSummaryDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.MenuItem;
import com.restaurant.models.entity.Order;
import com.restaurant.models.entity.OrderItem;
import com.restaurant.models.entity.Payment;
import com.restaurant.models.entity.RestaurantTable;
import com.restaurant.models.enums.OrderType;
import com.restaurant.models.enums.PaymentMethod;
import com.restaurant.models.enums.PaymentStatus;
import com.restaurant.models.enums.TableStatus;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.TableRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderHistoryReportService {
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(11);
    private static final BigDecimal TAX_MULTIPLIER = BigDecimal.valueOf(0.11);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderRepository orderRepo;
    private final MenuItemRepository menuItemRepo;
    private final PaymentRepository paymentRepo;
    private final TableRepository tableRepo;

    @Transactional
    public OrderPageDTO getHistory(LocalDate date, Integer month, Integer year, LocalDate startDate,
            LocalDate endDate, String search, int page, int size) {
        List<Order> orders = filterOrders(date, month, year, startDate, endDate, search);
        return toPage(orders, page, size);
    }

    @Transactional
    public ReportDTO getReport(LocalDate date, Integer month, Integer year, LocalDate startDate,
            LocalDate endDate, String search, int page, int size) {
        List<Order> orders = filterOrders(date, month, year, startDate, endDate, search);
        return ReportDTO.builder()
                .page(toPage(orders, page, size))
                .summary(toSummary(orders))
                .build();
    }

    @Transactional
    public OrderDTO getOrderDetail(Long orderId) {
        return toOrderDTO(findOrder(orderId));
    }

    @Transactional
    public OrderDTO updateOrder(Long orderId, CreateOrderRequest request) {
        Order order = findOrder(orderId);
        RestaurantTable oldTable = order.getTable();
        RestaurantTable newTable = resolveTable(request, order);

        order.setTable(newTable);
        order.setCustomerName(blankToNull(request.getCustomerName()));
        order.setOrderType(request.getOrderType());
        order.setNote(blankToNull(request.getNote()));
        order.setTaxRate(TAX_RATE);
        order.getOrderItems().clear();

        BigDecimal subTotal = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepo.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                            "Khong tim thay Menu Item"));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItem(menuItem);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(money(menuItem.getPrice()));
            item.setNote(blankToNull(itemRequest.getNote()));
            order.getOrderItems().add(item);
            subTotal = subTotal.add(lineTotal(item));
        }

        order.setSubTotal(money(subTotal));
        order.setTax(money(order.getSubTotal().multiply(TAX_MULTIPLIER)));
        order.setTotalPrice(money(order.getSubTotal().add(order.getTax())));
        updatePayment(order, request);
        updateTableStatus(order, oldTable, newTable);

        return toOrderDTO(orderRepo.save(order));
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = findOrder(orderId);
        RestaurantTable table = order.getTable();
        orderRepo.delete(order);
        if (table != null) {
            table.setStatus(TableStatus.AVAILABLE);
            tableRepo.save(table);
        }
    }

    @Transactional
    public byte[] exportExcel(LocalDate date, Integer month, Integer year, LocalDate startDate,
            LocalDate endDate, String search) {
        List<Order> orders = filterOrders(date, month, year, startDate, endDate, search);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet ordersSheet = workbook.createSheet("Orders");
            writeOrderSheet(ordersSheet, orders);

            Sheet summarySheet = workbook.createSheet("Summary");
            writeSummarySheet(summarySheet, toSummary(orders));

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new ApiException("EXPORT_EXCEL_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Export Excel failed");
        }
    }

    @Transactional
    public byte[] exportPdf(LocalDate date, Integer month, Integer year, LocalDate startDate,
            LocalDate endDate, String search) {
        List<Order> orders = filterOrders(date, month, year, startDate, endDate, search);
        ReportSummaryDTO summary = toSummary(orders);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Restaurant Report"));
            document.add(new Paragraph("Orders: " + summary.getTotalOrders()
                    + " | Customers: " + summary.getTotalCustomers()
                    + " | Revenue: " + summary.getRevenue()
                    + " | Money in: " + summary.getMoneyIn()
                    + " | Money out: " + summary.getMoneyOut()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            List.of("Order Id", "Date", "Customer", "Menu Name", "Type Of Service", "QTY", "Total")
                    .forEach(table::addCell);
            orders.stream().map(this::toListItem).forEach(item -> {
                table.addCell(item.getOrderCode());
                table.addCell(item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FORMAT) : "");
                table.addCell(nullToBlank(item.getCustomerName()));
                table.addCell(nullToBlank(item.getMenuName()));
                table.addCell(formatOrderType(item.getOrderType()));
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(item.getTotalPrice().toString());
            });
            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new ApiException("EXPORT_PDF_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Export PDF failed");
        }
    }

    private List<Order> filterOrders(LocalDate date, Integer month, Integer year, LocalDate startDate,
            LocalDate endDate, String search) {
        LocalDateTime from = resolveFrom(date, month, year, startDate);
        LocalDateTime to = resolveTo(date, month, year, endDate);
        String keyword = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return orderRepo.findAll()
                .stream()
                .filter(order -> from == null || (order.getCreatedAt() != null && !order.getCreatedAt().isBefore(from)))
                .filter(order -> to == null || (order.getCreatedAt() != null && !order.getCreatedAt().isAfter(to)))
                .filter(order -> keyword.isBlank() || matches(order, keyword))
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private LocalDateTime resolveFrom(LocalDate date, Integer month, Integer year, LocalDate startDate) {
        if (date != null) {
            return date.atStartOfDay();
        }
        if (month != null && year != null) {
            return YearMonth.of(year, month).atDay(1).atStartOfDay();
        }
        if (year != null) {
            return LocalDate.of(year, 1, 1).atStartOfDay();
        }
        return startDate != null ? startDate.atStartOfDay() : null;
    }

    private LocalDateTime resolveTo(LocalDate date, Integer month, Integer year, LocalDate endDate) {
        if (date != null) {
            return date.atTime(LocalTime.MAX);
        }
        if (month != null && year != null) {
            return YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX);
        }
        if (year != null) {
            return LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
        }
        return endDate != null ? endDate.atTime(LocalTime.MAX) : null;
    }

    private boolean matches(Order order, String keyword) {
        return contains(orderCode(order.getId()), keyword)
                || contains(order.getCustomerName(), keyword)
                || contains(order.getOrderType() != null ? order.getOrderType().name() : null, keyword)
                || contains(order.getOrderType() != null ? formatOrderType(order.getOrderType().name()) : null, keyword)
                || order.getOrderItems().stream()
                        .anyMatch(item -> contains(item.getMenuItem().getName(), keyword));
    }

    private OrderPageDTO toPage(List<Order> orders, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        int from = Math.min(safePage * safeSize, orders.size());
        int to = Math.min(from + safeSize, orders.size());
        int totalPages = orders.isEmpty() ? 0 : (int) Math.ceil((double) orders.size() / safeSize);

        return OrderPageDTO.builder()
                .orders(orders.subList(from, to).stream().map(this::toListItem).toList())
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .totalItems(orders.size())
                .build();
    }

    private ReportSummaryDTO toSummary(List<Order> orders) {
        BigDecimal revenue = orders.stream()
                .map(Order::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal moneyIn = orders.stream()
                .map(Order::getPayment)
                .filter(Objects::nonNull)
                .map(Payment::getReceived)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal moneyOut = orders.stream()
                .map(Order::getPayment)
                .filter(Objects::nonNull)
                .map(Payment::getChangeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long customers = orders.stream()
                .map(Order::getCustomerName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .count();

        return ReportSummaryDTO.builder()
                .totalOrders(orders.size())
                .totalCustomers(customers)
                .revenue(money(revenue))
                .moneyIn(money(moneyIn))
                .moneyOut(money(moneyOut))
                .build();
    }

    private OrderListItemDTO toListItem(Order order) {
        return OrderListItemDTO.builder()
                .id(order.getId())
                .orderCode(orderCode(order.getId()))
                .createdAt(order.getCreatedAt())
                .customerName(order.getCustomerName())
                .menuName(order.getOrderItems().stream()
                        .map(item -> item.getMenuItem().getName())
                        .distinct()
                        .reduce((left, right) -> left + ", " + right)
                        .orElse(""))
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .quantity(order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .totalPrice(money(order.getTotalPrice()))
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPayment() != null && order.getPayment().getPaymentMethod() != null
                        ? order.getPayment().getPaymentMethod().name()
                        : null)
                .received(order.getPayment() != null ? order.getPayment().getReceived() : null)
                .changeAmount(order.getPayment() != null ? order.getPayment().getChangeAmount() : null)
                .build();
    }

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
                .paymentId(order.getPayment() != null ? order.getPayment().getId() : null)
                .paymentProvider(order.getPayment() != null ? order.getPayment().getProvider() : null)
                .transactionRef(order.getPayment() != null ? order.getPayment().getTransactionRef() : null)
                .providerTransactionNo(order.getPayment() != null ? order.getPayment().getProviderTransactionNo() : null)
                .providerResponseCode(order.getPayment() != null ? order.getPayment().getProviderResponseCode() : null)
                .bankCode(order.getPayment() != null ? order.getPayment().getBankCode() : null)
                .paymentUrl(order.getPayment() != null ? order.getPayment().getPayUrl() : null)
                .qrContent(order.getPayment() != null ? order.getPayment().getPayUrl() : null)
                .received(order.getPayment() != null ? order.getPayment().getReceived() : null)
                .changeAmount(order.getPayment() != null ? order.getPayment().getChangeAmount() : null)
                .createdAt(order.getCreatedAt())
                .items(order.getOrderItems().stream().map(this::toOrderItemDTO).toList())
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

    private void updatePayment(Order order, CreateOrderRequest request) {
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            order.setPayment(payment);
        }

        BigDecimal received = request.getPaymentMethod() == PaymentMethod.CASH
                ? request.getReceived()
                : order.getTotalPrice();
        if (received == null || money(received).compareTo(order.getTotalPrice()) < 0) {
            throw new ApiException("INSUFFICIENT_PAYMENT", HttpStatus.BAD_REQUEST,
                    "Received amount is less than total");
        }

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(order.getTotalPrice());
        payment.setReceived(money(received));
        payment.setChangeAmount(money(received.subtract(order.getTotalPrice())));
        payment.setStatus(PaymentStatus.COMPLETED);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepo.save(payment);
    }

    private RestaurantTable resolveTable(CreateOrderRequest request, Order order) {
        if (request.getOrderType() != OrderType.DINE_IN) {
            return null;
        }
        if (request.getTableId() == null) {
            throw new ApiException("TABLE_REQUIRED", HttpStatus.BAD_REQUEST, "Dine-in order must select table");
        }

        RestaurantTable table = tableRepo.findById(request.getTableId())
                .orElseThrow(() -> new ApiException("TABLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Table not found"));
        boolean sameTable = order.getTable() != null && order.getTable().getId().equals(table.getId());
        if (!sameTable && table.getStatus() == TableStatus.USED) {
            throw new ApiException("TABLE_IN_USE", HttpStatus.BAD_REQUEST, "Table is already in use");
        }
        return table;
    }

    private void updateTableStatus(Order order, RestaurantTable oldTable, RestaurantTable newTable) {
        if (oldTable != null && (newTable == null || !oldTable.getId().equals(newTable.getId()))) {
            oldTable.setStatus(TableStatus.AVAILABLE);
            tableRepo.save(oldTable);
        }
        if (newTable != null) {
            newTable.setStatus(order.getStatus() != null && order.getStatus().name().equals("DELIVERED")
                    ? TableStatus.AVAILABLE
                    : TableStatus.USED);
            tableRepo.save(newTable);
        }
    }

    private void writeOrderSheet(Sheet sheet, List<Order> orders) {
        Row header = sheet.createRow(0);
        List.of("Order Id", "Date", "Customer", "Menu Name", "Type Of Service", "QTY", "Total",
                "Money In", "Money Out")
                .forEach(title -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(title));

        int rowIndex = 1;
        for (Order order : orders) {
            OrderListItemDTO item = toListItem(order);
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getOrderCode());
            row.createCell(1).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FORMAT) : "");
            row.createCell(2).setCellValue(nullToBlank(item.getCustomerName()));
            row.createCell(3).setCellValue(nullToBlank(item.getMenuName()));
            row.createCell(4).setCellValue(formatOrderType(item.getOrderType()));
            row.createCell(5).setCellValue(item.getQuantity());
            row.createCell(6).setCellValue(item.getTotalPrice().doubleValue());
            row.createCell(7).setCellValue(item.getReceived() != null ? item.getReceived().doubleValue() : 0);
            row.createCell(8).setCellValue(item.getChangeAmount() != null ? item.getChangeAmount().doubleValue() : 0);
        }
    }

    private void writeSummarySheet(Sheet sheet, ReportSummaryDTO summary) {
        List<List<String>> rows = List.of(
                List.of("Total Orders", String.valueOf(summary.getTotalOrders())),
                List.of("Total Customers", String.valueOf(summary.getTotalCustomers())),
                List.of("Revenue", summary.getRevenue().toString()),
                List.of("Money In", summary.getMoneyIn().toString()),
                List.of("Money Out", summary.getMoneyOut().toString()));

        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows.get(i).get(0));
            row.createCell(1).setCellValue(rows.get(i).get(1));
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay Order"));
    }

    private BigDecimal lineTotal(OrderItem item) {
        return money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String orderCode(Long id) {
        return "#" + String.format("%04d", id);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String formatOrderType(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", " ").toLowerCase(Locale.ROOT);
    }
}
