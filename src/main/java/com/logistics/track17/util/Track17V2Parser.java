package com.logistics.track17.util;

import com.logistics.track17.dto.Track17V2Response;
import com.logistics.track17.entity.TrackingEvent;
import com.logistics.track17.entity.TrackingNumber;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 17Track V2 数据解析工具类
 */
@Slf4j
public class Track17V2Parser {

    /**
     * 解析并更新运单信息
     */
    public static void parseAndUpdateTracking(Track17V2Response.AcceptedItem item, TrackingNumber trackingNumber) {
        if (item == null || item.getTrackInfo() == null) {
            return;
        }

        Track17V2Response.TrackInfo trackInfo = item.getTrackInfo();

        // 更新承运商ID
        if (item.getCarrier() != null) {
            trackingNumber.setCarrierId(item.getCarrier());
        }

        // 更新最新状态
        if (trackInfo.getLatestStatus() != null) {
            Track17V2Response.LatestStatus latestStatus = trackInfo.getLatestStatus();
            trackingNumber.setTrackStatus(latestStatus.getStatus());  // Delivered, InTransit等
            trackingNumber.setSubStatus(latestStatus.getSubStatus());
            trackingNumber.setSubStatusDescr(latestStatus.getSubStatusDescr());
        }

        // 更新时效数据
        if (trackInfo.getTimeMetrics() != null) {
            Track17V2Response.TimeMetrics metrics = trackInfo.getTimeMetrics();
            trackingNumber.setDaysOfTransit(metrics.getDaysOfTransit());
            trackingNumber.setDaysAfterLastUpdate(metrics.getDaysAfterLastUpdate());
        }

        // 更新最新事件
        if (trackInfo.getLatestEvent() != null) {
            Track17V2Response.TrackEvent latestEvent = trackInfo.getLatestEvent();
            trackingNumber.setLatestEventDesc(latestEvent.getDescription());
            trackingNumber.setLatestEventLocation(latestEvent.getLocation());

            // 解析ISO时间
            if (latestEvent.getTimeIso() != null) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(latestEvent.getTimeIso());
                    trackingNumber.setLatestEventTime(zdt.toLocalDateTime());
                } catch (DateTimeParseException e) {
                    log.warn("Failed to parse latest event time: {}", latestEvent.getTimeIso());
                }
            }
        }

        // 更新地址信息
        if (trackInfo.getShippingInfo() != null) {
            if (trackInfo.getShippingInfo().getShipperAddress() != null) {
                trackingNumber.setOriginCountry(trackInfo.getShippingInfo().getShipperAddress().getCountry());
            }
            if (trackInfo.getShippingInfo().getRecipientAddress() != null) {
                trackingNumber.setDestinationCountry(trackInfo.getShippingInfo().getRecipientAddress().getCountry());
            }
        }

        // 从里程碑获取揽收和签收时间
        if (trackInfo.getMilestone() != null) {
            for (Track17V2Response.Milestone milestone : trackInfo.getMilestone()) {
                if (milestone.getTimeIso() == null) continue;

                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(milestone.getTimeIso());
                    LocalDateTime time = zdt.toLocalDateTime();

                    if ("PickedUp".equals(milestone.getKeyStage())) {
                        trackingNumber.setPickupTime(time);
                    } else if ("Delivered".equals(milestone.getKeyStage())) {
                        trackingNumber.setDeliveredTime(time);
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Failed to parse milestone time: {}", milestone.getTimeIso());
                }
            }
        }

        // 更新同步时间
        trackingNumber.setLastSyncAt(LocalDateTime.now());
    }

    /**
     * 解析物流事件列表
     */
    public static List<TrackingEvent> parseEvents(Track17V2Response.AcceptedItem item, Long trackingId) {
        List<TrackingEvent> events = new ArrayList<>();

        if (item == null || item.getTrackInfo() == null || item.getTrackInfo().getTracking() == null) {
            return events;
        }

        Track17V2Response.Tracking tracking = item.getTrackInfo().getTracking();
        if (tracking.getProviders() == null) {
            return events;
        }

        // 遍历所有承运商的事件
        for (Track17V2Response.Provider provider : tracking.getProviders()) {
            if (provider.getEvents() == null || provider.getEvents().isEmpty()) {
                continue;
            }

            Integer providerKey = provider.getProvider() != null ? provider.getProvider().getKey() : null;
            String providerName = provider.getProvider() != null ? provider.getProvider().getName() : null;

            for (Track17V2Response.TrackEvent trackEvent : provider.getEvents()) {
                TrackingEvent event = new TrackingEvent();
                event.setTrackingId(trackingId);
                event.setEventDescription(trackEvent.getDescription());
                event.setEventLocation(trackEvent.getLocation());
                event.setStage(trackEvent.getStage());
                event.setSubStatus(trackEvent.getSubStatus());
                event.setTimeIso(trackEvent.getTimeIso());
                event.setProviderKey(providerKey);
                event.setProviderName(providerName);

                // 解析时间
                if (trackEvent.getTimeIso() != null) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(trackEvent.getTimeIso());
                        event.setEventTime(zdt.toLocalDateTime());
                    } catch (DateTimeParseException e) {
                        log.warn("Failed to parse event time: {}", trackEvent.getTimeIso());
                        event.setEventTime(LocalDateTime.now());
                    }
                } else {
                    event.setEventTime(LocalDateTime.now());
                }

                // 解析地址信息
                if (trackEvent.getAddress() != null) {
                    event.setCity(trackEvent.getAddress().getCity());
                    event.setPostalCode(trackEvent.getAddress().getPostalCode());
                }

                events.add(event);
            }
        }

        return events;
    }

    /**
     * 将状态文本转换为统一格式
     */
    public static String normalizeStatus(String status) {
        if (status == null) {
            return "InfoReceived";
        }

        switch (status) {
            case "Delivered":
                return "Delivered";
            case "InTransit":
            case "PickedUp":
            case "Departure":
            case "Arrival":
            case "OutForDelivery":
            case "AvailableForPickup":
                return "InTransit";
            case "Exception":
            case "Returning":
            case "Returned":
                return "Exception";
            default:
                return "InfoReceived";
        }
    }
}
