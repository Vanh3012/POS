# Cashier Flow

## Muc Tieu UI

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

Trang cashier la mot man hinh duy nhat tren frontend, nhung co the goi nhieu API khac nhau. Endpoint `GET /cashier` duoc dung nhu API tong hop de load du lieu ban dau cho page. Cac hanh dong nghiep vu nhu place order va close order nam duoi `/cashier/orders`.

## API Da Co

### Load Cashier View

```http
GET /cashier
```

Response gom:

```json
{
  "categories": [],
  "menuItems": [],
  "tables": [],
  "orders": []
}
```

Trong do:

- `categories`: danh sach category.
- `menuItems`: danh sach mon an, co the filter bang `keyword`.
- `tables`: danh sach ban active.
- `orders`: danh sach order dang mo, hien lay status `BEING_COOKED`.

Co the search menu ngay luc load view:

```http
GET /cashier?keyword=burger
```

### Category Va Menu

```http
GET /cashier/categories
GET /cashier/menu-items
GET /cashier/menu-items?categoryId=1
GET /cashier/menu-items?keyword=burger
GET /cashier/menu-items?categoryId=1&keyword=burger
GET /cashier/menu-items/{menuItemId}
```

### Place Order

```http
POST /cashier/orders
```

Body:

```json
{
  "tableId": 1,
  "customerName": "Avita Desi",
  "orderType": "DINE_IN",
  "note": "Extra cutlery",
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
      "note": "No onion"
    }
  ]
}
```

Rule backend:

- `items` bat buoc khong rong.
- Moi item phai co `menuItemId` ton tai va `quantity >= 1`.
- `orderType = DINE_IN` bat buoc co `tableId`.
- Ban dine-in phai ton tai, active, va chua `USED`.
- `orderType = TAKE_AWAY` khong can table; neu gui `tableId` thi backend bo qua.
- Tax hien tinh 11%.
- Tao `Order` status `BEING_COOKED`.
- Tao `Payment` status `PENDING`.
- Neu dine-in thi set table status `USED`.

Response la `OrderDTO`.

### Close Order

```http
PATCH /cashier/orders/{orderId}/close
```

Cash:

```json
{
  "paymentMethod": "CASH",
  "received": 20.00
}
```

Credit card hoac QRIS:

```json
{
  "paymentMethod": "QRIS"
}
```

Rule backend:

- Order phai ton tai.
- Order da `DELIVERED` thi khong close lai.
- Neu cash, `received` bat buoc co va phai >= `totalPrice`.
- Neu credit card hoac QRIS, backend tu set `received = totalPrice`.
- Set payment status `COMPLETED`.
- Set `paidAt` va `paidBy`.
- Set order status `DELIVERED`.
- Neu order co table thi set table status `AVAILABLE`.

Response la `OrderDTO`.

## DTO Chinh

### CashierViewDTO

```json
{
  "categories": [],
  "menuItems": [],
  "tables": [],
  "orders": []
}
```

### OrderDTO

```json
{
  "id": 1,
  "tableId": 1,
  "tableNumber": "Table 20",
  "userId": 3,
  "customerName": "Avita Desi",
  "subTotal": 8.99,
  "taxRate": 11.00,
  "tax": 0.99,
  "totalPrice": 9.98,
  "status": "BEING_COOKED",
  "orderType": "DINE_IN",
  "note": "Extra cutlery",
  "paymentMethod": "QRIS",
  "paymentStatus": "PENDING",
  "received": null,
  "changeAmount": null,
  "createdAt": "2026-05-08T14:00:00",
  "items": [
    {
      "id": 1,
      "menuItemId": 1,
      "menuItemName": "Beef Burger",
      "quantity": 1,
      "unitPrice": 2.50,
      "lineTotal": 2.50,
      "note": ""
    }
  ]
}
```

## Backend Da Implement

### Controller

File: `src/main/java/com/restaurant/controller/CashierController.java`

Da co:

- `GET /cashier`
- `GET /cashier/categories`
- `GET /cashier/menu-items`
- `GET /cashier/menu-items/{menuItemId}`
- `POST /cashier/orders`
- `PATCH /cashier/orders/{orderId}/close`

### Service

File: `src/main/java/com/restaurant/service/CashierService.java`

Da co:

- Load cashier view.
- Load categories.
- Load menu items.
- Search menu item theo keyword.
- Filter menu item theo category.
- Load tables thong qua `TableService`.
- Load open orders status `BEING_COOKED`.
- Place order.
- Close order/payment.
- Mapper `Order -> OrderDTO`.
- Mapper `OrderItem -> OrderItemDTO`.
- Tinh `subTotal`, `tax`, `totalPrice`.
- Dong bo table status khi place/close order.

### Repository

Da co:

- `OrderRepository`
- `PaymentRepository`
- `CategoryRepository`
- `MenuItemRepository`
- `TableRepository`
- `UserRepository`

### Request DTO

Da co:

- `CreateOrderRequest`
- `CreateOrderItemRequest`
- `CloseOrderRequest`

### Response DTO

Da co:

- `CashierViewDTO`
- `OrderDTO`
- `OrderItemDTO`
- `CategoryDTO`
- `MenuItemDTO`
- `TableDTO`

### Security

File: `src/main/java/com/restaurant/config/SecurityConfig.java`

Da mo authenticated access cho:

- `GET /cashier/**`
- `POST /cashier/**`
- `PATCH /cashier/**`

Admin-only table management van nam o:

- `POST /tables`
- `PUT /tables/{tableId}`
- `DELETE /tables/{tableId}`
- `PATCH /tables/{tableId}/active`

Cashier/waiter duoc doi status ban qua:

- `PATCH /tables/{tableId}/status`
- `PATCH /tables/{tableId}/toggle-status`

## Flow Frontend De Goi API

### 1. Load Man Cashier

```http
GET /cashier
```

Render:

- Category tabs tu `categories`.
- Menu grid tu `menuItems`.
- Table dropdown tu `tables`.
- Order list tu `orders`.

### 2. Search/Filter Menu

```http
GET /cashier/menu-items?keyword=burger
GET /cashier/menu-items?categoryId=1&keyword=burger
```

### 3. Gio Hang Draft

Cart ben phai nen giu o frontend truoc khi bam `Place Order`.

Khi click menu item:

- Neu item chua co trong cart: them item voi `quantity = 1`.
- Neu item da co trong cart: tang quantity len 1.
- Nut minus giam quantity.
- Quantity ve 0 thi xoa item khoi cart.

### 4. Place Order

Frontend gui cart draft len:

```http
POST /cashier/orders
```

Sau khi thanh cong:

- Clear cart draft.
- Goi lai `GET /cashier` de refresh table status va order list.

### 5. Close Order

Khi user chon mot order dang mo va bam `Close Order`:

```http
PATCH /cashier/orders/{orderId}/close
```

Sau khi thanh cong:

- Goi lai `GET /cashier`.
- Order da close bien mat khoi `orders` vi khong con status `BEING_COOKED`.
- Ban dine-in quay ve `AVAILABLE`.

## Trang Thai Va Enum

`OrderType`:

- `DINE_IN`
- `TAKE_AWAY`

`OrderStatus`:

- `BEING_COOKED`
- `DELIVERED`

`PaymentMethod`:

- `CASH`
- `CREDIT_CARD`
- `QRIS`

`PaymentStatus`:

- `PENDING`
- `COMPLETED`
- `REFUNDED`

`TableStatus`:

- `AVAILABLE`
- `USED`

## Nhung Phan Chua Lam

- Sua item tren order da tao.
- Xoa item tren order da tao.
- Cancel order.
- Refund payment.
- Lich su order da close.
- Filter orders theo table/status/date.
- Tach tax rate ra config thay vi hard-code 11%.
- Mo rong `OrderStatus` neu can workflow bep ro hon, vi hien chi co `BEING_COOKED` va `DELIVERED`.

## Test

Da chay:

```powershell
.\mvnw.cmd test-compile
.\mvnw.cmd test
```

Ket qua:

- Compile pass.
- Test pass: 4 tests, 0 failures.

Ghi chu: project dang dung Spring Boot `4.0.5`, nen da nang `springdoc-openapi-starter-webmvc-ui` len `3.0.3` de `/api-docs` hoat dong voi Boot 4.
