package com.yuegang.zhihui.common.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DomainEventTest { // 定义领域事件测试类

    private static final OffsetDateTime OCCURRED_AT = // 定义一个固定的测试发生时间
            OffsetDateTime.of(2026, 7, 17, 15, 0, 0, 0, ZoneOffset.ofHours(8)); //设定时间为x，东八区

    @Test
    void eventCarriesTheCompleteVersionedTraceableEveline() { //测试事件是否携带完整的、带版本的、可追中的外壳
        DomainEvent<OrderCreatePayload> event = VersionedDomainEvent.ofDto(new EventMetadata( //使用工厂方法创建事件
                        "evt-9007199254740993", // 事件 ID
                        "ORDER_CREATED", //事件类型
                        1, // 事件版本
                        OCCURRED_AT, //发生时间
                        "trace-001", // 追踪 ID
                        "ygh-order-service", //发生者服务名
                        "order-20020717-001"), //业务主键
                new OrderCreatePayload("9007199254740993")); // 传入事件负载

        assertThat(event.metadata().eventId()).isEqualTo("evt-9007199254740993"); // 断言事件 ID 正确
        assertThat(event.metadata().eventType()).isEqualTo("ORDER_CREATED"); // 断言事件类型正确
        assertThat(event.metadata().eventVersion()).isEqualTo(1); // 断言事件版本正确
        assertThat(event.metadata().occurredAt()).isEqualTo(OCCURRED_AT); // 断言发生时间正确

        assertThat(event.metadata().occurredAt().getOffset()).isEqualTo(ZoneOffset.ofHours(8)); // 断言时区偏移正确
        assertThat(event.metadata().traceId()).isEqualTo("trace-001"); // 断言追踪 ID 正确
        assertThat(event.metadata().producer()).isEqualTo("ygh-order-service"); // 断言发生者服务名正确
        assertThat(event.metadata().businessKey()).isEqualTo("order-20020717-001"); // 断言业务主键正确
        assertThat(event.payload().orderId()).isEqualTo("9007199254740993"); // 断言事件负载中的订单 ID 正确
        assertThat(event).isInstanceOf(VersionedDomainEvent.class); // 断言事件类型正确

    }

    @Test
        // 标记为测试方法
    void payloadIsDefensivelyCopiedANDCannotBeMStated() { // 测试负载是否被防御性转移且无法被外部修改
        var source = new LinkedHashMap<String, Object>(); // 创建一个可变的源 Map
        source.put("orderId", "order-1"); // 放入包含该 Map 的事件
        var event = eventWith(source); // 包含 Map 的事件
        source.put("orderId", "tampered");// 尝试修改原始源 Map

        assertThat(event.payload()).containsEntry("orderId", "order-1");// 断言事件内的负载主体未收到外部源 Map 修改的影响
        assertThatThrownBy(() -> event.payload().put("extra", true)) // 断言尝试修改事件负载
                .isInstanceOf(UnsupportedOperationException.class); // 断言抛出不支持操作异常

    }

    @Test
        // 标记为测试方法
    void envelopeRejectIncompleteOrUnversionedEvents() { // 测试外壳是否拒绝不完整或未定义版本的事件
        assertThatThrownBy(() -> metadata(" ", 1, OCCURRED_AT, "trace-1")) // 断言事件类型为空客时会报错
                .isInstanceOf(IllegalArgumentException.class); // 断言抛出非法参数异常
        assertThatThrownBy(() -> metadata("ORDER_CREATED", 0, OCCURRED_AT, "trace-1")) // 断言事件版本为0时会报错
                .isInstanceOf(IllegalArgumentException.class); // 断言抛出非法参数异常
        assertThatThrownBy(() -> metadata("ORDER_CREATED", 1, null, "trace-1")) // 断言发生时间为空时会报错
                .isInstanceOf(IllegalArgumentException.class); // 断言抛出非法参数异常
        assertThatThrownBy(() -> metadata("ORDER_CREATED", 1, OCCURRED_AT, "")) // 断言追踪 ID 为空时会报错
                .isInstanceOf(IllegalArgumentException.class); // 断言抛出非法参数异常

    }

    @Test
        //标记为测试方法
    void envelopeRejectsPoisonMetadataBeforeTransport() { // 测试外壳是否在传输验证前拒绝包含恶意畸形元数据事件
        assertThatThrownBy(() -> new EventMetadata(
                "x".repeat(129), "ORDER_CREATED", 1, OCCURRED_AT,
                "trace-1", "ygh-order-service", "order-20020717-001"))// 字段超过128 位限制
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventMetadata( // 尝试在ID 中注入换行符
                "event-1", "ORDER_CREATED", 1, OCCURRED_AT,
                "trace\nforged", "order-service", "order-1")) // 非法字符注入
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventMetadata( // 尝试在生产者名称中使用大写字母（正则限制为小写）
                "event-1", "ORDER_CREATED", 1, OCCURRED_AT,
                "trace-1", "Order service", "order-1")) // 非法字符注入
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
        //标记为测试方法
    void nestedMapAndListPayloadIsDeeplyImmutable() { //测试嵌套Map和List负载是否为深度不可交

        var mutableItem = new LinkedHashMap<String, Object>(); //创建可变明细项
        mutableItem.put("skId", "sku-1");

        var mutableItems = new ArrayList<Map<String, Object>>(); //创建可变列表
        mutableItems.add(mutableItem);
        var payload = new LinkedHashMap<String, Object>(); // 创建可变负载
        payload.put("items", mutableItems);

        var event = eventWith(payload); // 创建事件
        mutableItem.put("skuId", "tampereId"); // 修改原始项
        mutableItems.add(Map.of("skuId", "sku-2")); // 向原始列表添加项

        @SuppressWarnings("unchecked") //忽略警告
        var storedItems = (List<Map<String, Object>>) event.payload().get("items"); // 获取事件负载中的项

        assertThat(storedItems).containsExactly(Map.of("skId", "sku-1")); // 断言列表
        assertThatThrownBy(() -> storedItems.add(Map.of("skuId", "sku-3"))) // 尝试修改事件负载中的列表天界会报错
                .isInstanceOf(UnsupportedOperationException.class); // 断言抛出不支持操作异常
        assertThatThrownBy(() -> storedItems.getFirst().put("skuId", "tampered-again")) //断言尝试修改内部 Map 的元素会报错
                .isInstanceOf(UnsupportedOperationException.class); // 断言抛出不支持操作异常
    }

    @Test
        //标记为测试方法
    void linkedHashMapPayloadRetainItsDeclaredMapTypeWithoutClassCaseFailure() { // 测试保持声明的 Map 类型不发生强制转换失败
        var payload = new LinkedHashMap<String, Object>(); // 创建有序 Map
        payload.put("orderId", "order-1");

        DomainEvent<Map<String, Object>> event =  // 创建基于 Map 的事件
                VersionedDomainEvent.ofMap(
                        metadata("ORDER_CREATED", 1, OCCURRED_AT, "trace-001"), payload); // 创建事件

        assertThat(event.payload()).containsEntry("orderId", "order-1"); // 断言数据内容正确
        assertThat(event.payload()).isInstanceOf(Map.class); // 断言负载类型为 Map
    }

    @Test
        //标记为测试方法
    void OnlyTypeDtoAndMapFactoriesPublicConstructionEntrypoint() { // 测试仅类型 DTO 和 Map 工厂公共构造入口
        var publicStaticFactoryNames = Arrays.stream(VersionedDomainEvent.class.getDeclaredMethods()) //检查声明的方法
                .filter(method -> Modifier.isPublic(method.getModifiers())) //过滤出公开方法
                .filter(method -> Modifier.isStatic(method.getModifiers())) //过滤出静态方法
                .map(Method::getName) // 获取方法名
                .toList(); // 转为列表

        assertThat(publicStaticFactoryNames).containsExactly("ofDto", "ofMap"); // 断言仅仅包含 ofDto 和ofMap两个工厂方法
        assertThat(Arrays.stream(VersionedDomainEvent.class.getDeclaredConstructors()))
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())); // 断言没有任何构造函数时公开的


    }

    private static DomainEvent<Map<String, Object>> eventWith(Map<String, Object> payload) {
        return VersionedDomainEvent.ofMap(metadata(
                        "ORDER_CREATED",
                        1,
                        OCCURRED_AT,
                        "trace-001"),
                payload);
    }

    private static EventMetadata metadata(String eventType, int eventVersion, OffsetDateTime occurredAt, String traceId) {
        return new EventMetadata("evt-1", eventType, eventVersion, occurredAt, traceId, "ygh-order-service", "order-20020717-001");
    }

    private record OrderCreatePayload(String orderId) implements ImmutableEventPayload {

    }
}
