package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.Order;
import com.logistics.track17.entity.OrderItem;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.mapper.OrderItemMapper;
import com.logistics.track17.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务
 */
@Slf4j
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ObjectMapper objectMapper;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper, ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 从webhook保存订单
     * 实现幂等性：如果订单已存在则更新
     */
    @Transactional(rollbackFor = Exception.class)
    public Order saveOrderFromWebhook(Shop shop, JsonNode orderData) {
        try {
            // 解析订单基本信息
            Long shopifyOrderId = orderData.get("id").asLong();

            // 检查订单是否已存在（幂等性）
            Order existingOrder = orderMapper.selectByShopifyOrderId(shop.getId(), shopifyOrderId);

            Order order = existingOrder != null ? existingOrder : new Order();
            order.setShopId(shop.getId());
            order.setShopifyOrderId(shopifyOrderId);
            order.setOrderNumber(
                    orderData.has("order_number") ? String.valueOf(orderData.get("order_number").asLong()) : null);
            order.setOrderName(orderData.has("name") ? orderData.get("name").asText() : null);

            // 客户信息
            if (orderData.has("customer") && !orderData.get("customer").isNull()) {
                JsonNode customer = orderData.get("customer");
                order.setCustomerEmail(customer.has("email") ? customer.get("email").asText() : null);
                String firstName = customer.has("first_name") ? customer.get("first_name").asText() : "";
                String lastName = customer.has("last_name") ? customer.get("last_name").asText() : "";
                order.setCustomerName((firstName + " " + lastName).trim());
                order.setCustomerPhone(customer.has("phone") ? customer.get("phone").asText() : null);
            } else {
                order.setCustomerEmail(orderData.has("email") ? orderData.get("email").asText() : null);
            }

            // 金额
            order.setTotalPrice(orderData.has("total_price") ? new BigDecimal(orderData.get("total_price").asText())
                    : BigDecimal.ZERO);
            order.setCurrency(orderData.has("currency") ? orderData.get("currency").asText() : "USD");

            // 状态
            order.setFinancialStatus(
                    orderData.has("financial_status") ? orderData.get("financial_status").asText() : null);
            order.setFulfillmentStatus(
                    orderData.has("fulfillment_status") && !orderData.get("fulfillment_status").isNull()
                            ? orderData.get("fulfillment_status").asText()
                            : null);

            // 收货地址
            if (orderData.has("shipping_address") && !orderData.get("shipping_address").isNull()) {
                JsonNode address = orderData.get("shipping_address");
                String firstName = address.has("first_name") ? address.get("first_name").asText() : "";
                String lastName = address.has("last_name") ? address.get("last_name").asText() : "";
                order.setShippingAddressName((firstName + " " + lastName).trim());
                order.setShippingAddressAddress1(address.has("address1") ? address.get("address1").asText() : null);
                order.setShippingAddressCity(address.has("city") ? address.get("city").asText() : null);
                order.setShippingAddressProvince(address.has("province") ? address.get("province").asText() : null);
                order.setShippingAddressCountry(address.has("country") ? address.get("country").asText() : null);
                order.setShippingAddressZip(address.has("zip") ? address.get("zip").asText() : null);
                order.setShippingAddressPhone(address.has("phone") ? address.get("phone").asText() : null);
            }

            // 时间
            if (orderData.has("created_at")) {
                order.setCreatedAt(parseShopifyDateTime(orderData.get("created_at").asText()));
            }
            if (orderData.has("updated_at")) {
                order.setUpdatedAt(parseShopifyDateTime(orderData.get("updated_at").asText()));
            }
            order.setSyncedAt(LocalDateTime.now());

            // 保存原始JSON
            order.setRawData(objectMapper.writeValueAsString(orderData));

            // 保存或更新订单
            if (existingOrder == null) {
                orderMapper.insert(order);
                log.info("Created new order: {} for shop: {}", order.getOrderNumber(), shop.getShopName());
            } else {
                orderMapper.update(order);
                log.info("Updated existing order: {} for shop: {}", order.getOrderNumber(), shop.getShopName());
            }

            // 保存订单商品
            if (orderData.has("line_items") && orderData.get("line_items").isArray()) {
                saveOrderItems(order.getId(), orderData.get("line_items"));
            }

            return order;

        } catch (Exception e) {
            log.error("Error saving order from webhook", e);
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        }
    }

    /**
     * 保存订单商品
     */
    private void saveOrderItems(Long orderId, JsonNode lineItems) {
        List<OrderItem> items = new ArrayList<>();

        for (JsonNode lineItem : lineItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setShopifyLineItemId(lineItem.has("id") ? lineItem.get("id").asLong() : null);
            item.setSku(lineItem.has("sku") ? lineItem.get("sku").asText() : null);
            item.setTitle(lineItem.has("title") ? lineItem.get("title").asText() : "Unknown");
            item.setVariantTitle(lineItem.has("variant_title") && !lineItem.get("variant_title").isNull()
                    ? lineItem.get("variant_title").asText()
                    : null);
            item.setQuantity(lineItem.has("quantity") ? lineItem.get("quantity").asInt() : 1);
            item.setPrice(lineItem.has("price") ? new BigDecimal(lineItem.get("price").asText()) : BigDecimal.ZERO);

            items.add(item);
        }

        if (!items.isEmpty()) {
            orderItemMapper.batchInsert(items);
            log.info("Saved {} order items for order ID: {}", items.size(), orderId);
        }
    }

    /**
     * 解析Shopify时间格式
     */
    private LocalDateTime parseShopifyDateTime(String dateTimeStr) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 根据ID查询订单
     */
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    /**
     * 查询订单列表
     */
    public List<Order> getOrderList(Long shopId, Integer page, Integer pageSize) {
        page = page == null || page < 1 ? 1 : page;
        pageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int offset = (page - 1) * pageSize;

        return orderMapper.selectList(shopId, offset, pageSize);
    }

    /**
     * 统计订单数量
     */
    public Long countOrders(Long shopId) {
        return orderMapper.count(shopId);
    }
}
