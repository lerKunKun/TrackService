package com.logistics.track17.dto;

import lombok.Data;

import java.util.List;

/**
 * 17Track查询响应数据
 */
@Data
public class Track17QueryResponse {
    private Integer code;              // 响应码，0表示成功
    private Track17Data data;          // 响应数据
    private String msg;                // 响应消息

    @Data
    public static class Track17Data {
        private List<AcceptedItem> accepted;  // 成功接收的运单
        private List<RejectedItem> rejected;  // 被拒绝的运单

        @Data
        public static class AcceptedItem {
            private String number;             // 运单号
            private Integer carrier;           // 承运商ID
            private TrackInfo track;           // 物流信息

            @Data
            public static class TrackInfo {
                private String w1;             // 承运商名称
                private String w2;             // 目的国家
                private String w3;             // 状态代码
                private String w4;             // 状态文本
                private Integer z0;            // 物流状态 (0-待查询 1-查询中 2-运输途中 3-到达待取 4-成功签收 5-疑难件 6-退件签收)
                private Integer z1;            // 子状态
                private Integer z2;            // 运输天数
                private String z3;             // 预计送达时间
                private List<TrackEvent> z4;   // 物流轨迹

                @Data
                public static class TrackEvent {
                    private String a;          // 事件时间 (格式: 20240101120000)
                    private String z;          // 事件描述
                    private String c;          // 事件位置
                    private String d;          // 详细描述
                }
            }
        }

        @Data
        public static class RejectedItem {
            private String number;             // 运单号
            private Integer carrier;           // 承运商ID
            private Error error;               // 错误信息

            @Data
            public static class Error {
                private Integer code;          // 错误码
                private String message;        // 错误消息
            }
        }
    }
}
