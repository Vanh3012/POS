# Cashier Flow

## Muc tieu UI

Man hinh cashier theo Figma gom:

- Sidebar cashier/table/report/history/supply.
- Khu order list dang mo.
- Khu menu theo category va search.
- Gio hang ben phai.
- Dropdown chon table.
- Dropdown chon order type.
- Modal sua customer name.
- Modal sua customer notes.
- Payment method: credit card, cash, QRIS.
- Nut `Place Order` va `Close Order`.

## Da co trong backend

### Cashier API doc du lieu

File: `src/main/java/com/restaurant/controller/CashierController.java`

Da co:

- `GET /cashier`
- `GET /cashier/categories`
- `GET /cashier/menu-items?categoryId=&keyword=`
- `GET /cashier/menu-items/{menuItemId}`

### Cashier service

File: `src/main/java/com/restaurant/service/CashierService.java`

Da co:

- Load categories.
- Load menu items.
- Search menu item theo keyword.
- Filter menu item theo category.
- Load tables thong qua `TableService`.

Con trong `getCashierView()`:

```java
CashierViewDTO {
    categories,
    menuItems,
    tables,
    orders
}
```

### Table service

File: `src/main/java/com/restaurant/service/TableService.java`

Da co:

- Lay danh sach table active.
- Admin CRUD table.
- Update table status.
- Toggle table status.

### Entity nen tang

Da co entity:

- `Order`
- `OrderItem`
- `Payment`
- `RestaurantTable`
- `MenuItem`
- `Category`
- `User`

`Order` da co cac field can cho cashier:

- `table`
- `user`
- `customerName`
- `subTotal`
- `taxRate`
- `tax`
- `totalPrice`
- `status`
- `orderType`
- `note`
- `createdAt`
- `orderItems`
- `payment`

`OrderItem` da co:

- `order`
- `menuItem`
- `quantity`
- `unitPrice`
- `note`

`Payment` da co:

- `order`
- `paymentMethod`
- `amount`
- `received`
- `changeAmount`
- `status`
- `paidAt`
- `paidBy`

### Enum da co

`OrderType`:

- `DINE_IN`
- `TAKE_AWAY`

`OrderStatus`:

- `DELIVERED`
- `BEING_COOKED`

`PaymentMethod`:

- `CASH`
- `CREDIT_CARD`
- `QRIS`

`PaymentStatus`:

- `PENDING`
- `COMPLETED`
- `REFUNDED`

## Con thieu

- `CashierService.getOpenOrders()` hien dang return list rong.
- Chua co `OrderRepository`.
- Chua co `PaymentRepository`.
- Chua co request DTO cho tao order.
- Chua co request DTO cho close order/payment.
- Chua co API tao order.
- Chua co API close order.
- Chua co mapper `Order -> OrderDTO`.
- Chua co mapper `OrderItem -> OrderItemDTO`.
- Chua co logic tinh `subTotal`, `tax`, `totalPrice`.
- Chua co logic doi table status khi tao/close order.
- `SecurityConfig` hien moi cho `GET /cashier/**`, chua cho `POST/PATCH /cashier/**`.

## Flow frontend nen lam

### 1. Load man cashier

Frontend goi:

```http
GET /cashier
```

Backend tra:

- `categories`
- `menuItems`
- `tables`
- `orders`

Dung de render:

- Category tabs.
- Menu grid.
- Table dropdown.
- Order list.

### 2. Search menu

Frontend goi:

```http
GET /cashier/menu-items?keyword=burger
```

Neu chon category:

```http
GET /cashier/menu-items?categoryId=1&keyword=burger
```

### 3. Chon table

UI dropdown `Select Table`.

Rule:

- Bat buoc neu `orderType = DINE_IN`.
- Khong bat buoc neu `orderType = TAKE_AWAY`.

### 4. Chon order type

UI dropdown `Order Type`.

Gia tri gui backend:

- `DINE_IN`
- `TAKE_AWAY`

### 5. Them mon vao cart

Khi click menu item:

- Neu item chua co trong cart: them item voi `quantity = 1`.
- Neu item da co trong cart: tang quantity len 1.
- Nut minus giam quantity.
- Quantity ve 0 thi xoa item khoi cart.

Khuyen nghi: cart ben phai nen giu o frontend truoc, chua tao order trong database ngay.

### 6. Edit customer name

Nut pencil o header ben phai mo modal.

Frontend luu vao draft:

```json
{
  "customerName": "Avita Desi"
}
```

### 7. Edit customer notes

Nut note/pencil mo modal.

Frontend luu vao draft:

```json
{
  "note": "Customer asks for extra cutlery for three people."
}
```

### 8. Chon payment method

Frontend luu payment method trong draft:

- `CASH`
- `CREDIT_CARD`
- `QRIS`

### 9. Place Order

Frontend goi endpoint de tao order that.

De xuat:

```http
POST /cashier/orders
```

Body:

```json
{
  "tableId": 6,
  "customerName": "Avita Desi",
  "orderType": "DINE_IN",
  "note": "Customer asks for extra cutlery for three people.",
  "paymentMethod": "QRIS",
  "items": [
    {
      "menuItemId": 1,
      "quantity": 1,
      "note": ""
    },
    {
      "menuItemId": 2,
      "quantity": 2,
      "note": ""
    }
  ]
}
```

Backend xu ly:

- Validate cart khong rong.
- Validate menu item ton tai.
- Neu `DINE_IN`, validate table ton tai va active.
- Tinh tien.
- Tao `Order`.
- Tao cac `OrderItem`.
- Tao `Payment` status `PENDING`.
- Set order status `BEING_COOKED`.
- Neu `DINE_IN`, set table status `USED`.
- Tra ve `OrderDTO`.

### 10. Close Order

Khi bam `Close Order`, backend hoan tat payment va order.

De xuat:

```http
PATCH /cashier/orders/{orderId}/close
```

Body:

```json
{
  "paymentMethod": "CASH",
  "received": 20.00
}
```

Backend xu ly:

- Lay order dang mo.
- Cap nhat payment method.
- Set amount = order total.
- Neu cash: tinh change = received - amount.
- Set payment status `COMPLETED`.
- Set `paidAt`.
- Set order status `DELIVERED`.
- Neu order dine-in co table: set table status `AVAILABLE`.
- Tra ve `OrderDTO`.

## Flow backend can code tiep

### Buoc 1. Tao repository

Can them:

- `src/main/java/com/restaurant/repository/OrderRepository.java`
- `src/main/java/com/restaurant/repository/PaymentRepository.java`

`OrderRepository` nen co query lay order dang mo:

```java
List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);
```

Hoac don gian:

```java
List<Order> findAllByOrderByCreatedAtDesc();
```

Sau do filter status trong service.

### Buoc 2. Tao request DTO

Can them:

- `CreateOrderRequest`
- `CreateOrderItemRequest`
- `CloseOrderRequest`

De xuat field:

`CreateOrderRequest`:

- `Long tableId`
- `String customerName`
- `OrderType orderType`
- `String note`
- `PaymentMethod paymentMethod`
- `List<CreateOrderItemRequest> items`

`CreateOrderItemRequest`:

- `Long menuItemId`
- `Integer quantity`
- `String note`

`CloseOrderRequest`:

- `PaymentMethod paymentMethod`
- `BigDecimal received`

### Buoc 3. Implement mapper DTO

Trong `CashierService`, them:

- `toOrderDTO(Order order)`
- `toOrderItemDTO(OrderItem item)`

`lineTotal = unitPrice * quantity`.

### Buoc 4. Implement getOpenOrders

Thay:

```java
public List<OrderDTO> getOpenOrders() {
    return List.of();
}
```

Bang logic lay order status `BEING_COOKED`.

### Buoc 5. Implement createOrder

Trong `CashierService`, them:

```java
public OrderDTO createOrder(CreateOrderRequest request)
```

Logic:

- Validate request.
- Build order.
- Build order items.
- Calculate totals.
- Build payment pending.
- Save order.
- Update table status neu can.
- Return DTO.

Tax theo UI dang la 11%, co the hard-code tam:

```java
private static final BigDecimal TAX_RATE = BigDecimal.valueOf(11);
private static final BigDecimal TAX_MULTIPLIER = BigDecimal.valueOf(0.11);
```

### Buoc 6. Implement closeOrder

Trong `CashierService`, them:

```java
public OrderDTO closeOrder(Long orderId, CloseOrderRequest request)
```

Logic:

- Tim order.
- Validate order chua close.
- Cap nhat payment.
- Cap nhat status order.
- Cap nhat table status.
- Return DTO.

### Buoc 7. Them endpoint vao CashierController

Them:

```java
@PostMapping("/orders")
public OrderDTO createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return cashierService.createOrder(request);
}

@PatchMapping("/orders/{orderId}/close")
public OrderDTO closeOrder(
        @PathVariable Long orderId,
        @Valid @RequestBody CloseOrderRequest request) {
    return cashierService.closeOrder(orderId, request);
}
```

### Buoc 8. Update SecurityConfig

File: `src/main/java/com/restaurant/config/SecurityConfig.java`

Hien co:

```java
.requestMatchers(HttpMethod.GET, "/cashier/**").authenticated()
```

Can them:

```java
.requestMatchers(HttpMethod.POST, "/cashier/**").authenticated()
.requestMatchers(HttpMethod.PATCH, "/cashier/**").authenticated()
```

## Ghi chu implementation

- Nen de cart la draft frontend cho toi khi bam `Place Order`.
- Backend chi luu order that khi `POST /cashier/orders`.
- Sau khi place order thanh cong, frontend clear cart va reload `GET /cashier`.
- `Close Order` nen dung cho order da tao, khong dung cho draft cart.
- Neu can status ro hon, nen mo rong `OrderStatus` ve sau:
  - `PENDING`
  - `BEING_COOKED`
  - `DELIVERED`
  - `COMPLETED`
  - `CANCELLED`

