package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 17Track V2 API 查询响应数据
 * 支持新版本的详细物流信息格式
 */
@Data
public class Track17V2Response {
    private Integer code;
    private Track17V2Data data;
    private String msg;

    @Data
    public static class Track17V2Data {
        private List<AcceptedItem> accepted;
        private List<RejectedItem> rejected;
    }

    @Data
    public static class AcceptedItem {
        private String number;
        private Integer carrier;
        private String tag;

        @JsonProperty("track_info")
        private TrackInfo trackInfo;
    }

    @Data
    public static class TrackInfo {
        @JsonProperty("shipping_info")
        private ShippingInfo shippingInfo;

        @JsonProperty("latest_status")
        private LatestStatus latestStatus;

        @JsonProperty("latest_event")
        private TrackEvent latestEvent;

        @JsonProperty("time_metrics")
        private TimeMetrics timeMetrics;

        private List<Milestone> milestone;

        private Tracking tracking;
    }

    @Data
    public static class ShippingInfo {
        @JsonProperty("shipper_address")
        private Address shipperAddress;

        @JsonProperty("recipient_address")
        private Address recipientAddress;
    }

    @Data
    public static class Address {
        private String country;
        private String state;
        private String city;
        private String street;
        @JsonProperty("postal_code")
        private String postalCode;
    }

    @Data
    public static class LatestStatus {
        private String status;           // Delivered, InTransit, InfoReceived, Exception等
        @JsonProperty("sub_status")
        private String subStatus;        // Delivered_Other, InTransit_PickedUp等
        @JsonProperty("sub_status_descr")
        private String subStatusDescr;
    }

    @Data
    public static class TimeMetrics {
        @JsonProperty("days_after_order")
        private Integer daysAfterOrder;

        @JsonProperty("days_of_transit")
        private Integer daysOfTransit;

        @JsonProperty("days_of_transit_done")
        private Integer daysOfTransitDone;

        @JsonProperty("days_after_last_update")
        private Integer daysAfterLastUpdate;
    }

    @Data
    public static class Milestone {
        @JsonProperty("key_stage")
        private String keyStage;         // InfoReceived, PickedUp, Departure, Arrival, Delivered等

        @JsonProperty("time_iso")
        private String timeIso;

        @JsonProperty("time_utc")
        private String timeUtc;
    }

    @Data
    public static class Tracking {
        @JsonProperty("providers_hash")
        private Long providersHash;

        private List<Provider> providers;
    }

    @Data
    public static class Provider {
        private ProviderInfo provider;

        @JsonProperty("latest_sync_status")
        private String latestSyncStatus;

        @JsonProperty("latest_sync_time")
        private String latestSyncTime;

        private List<TrackEvent> events;
    }

    @Data
    public static class ProviderInfo {
        private Integer key;
        private String name;
        private String alias;
        private String tel;
        private String homepage;
        private String country;
    }

    @Data
    public static class TrackEvent {
        @JsonProperty("time_iso")
        private String timeIso;

        @JsonProperty("time_utc")
        private String timeUtc;

        @JsonProperty("time_raw")
        private TimeRaw timeRaw;

        private String description;
        private String location;
        private String stage;

        @JsonProperty("sub_status")
        private String subStatus;

        private Address address;
    }

    @Data
    public static class TimeRaw {
        private String date;
        private String time;
        private String timezone;
    }

    @Data
    public static class RejectedItem {
        private String number;
        private Integer carrier;
        private RejectedError error;
    }

    @Data
    public static class RejectedError {
        private Integer code;
        private String message;
    }
}
