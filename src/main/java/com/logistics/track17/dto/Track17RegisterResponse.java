package com.logistics.track17.dto;

import lombok.Data;

import java.util.List;

/**
 * 17Track注册响应数据
 */
@Data
public class Track17RegisterResponse {
    private Integer code;              // 响应码，0表示成功
    private Track17RegisterData data;  // 响应数据
    private String msg;                // 响应消息

    @Data
    public static class Track17RegisterData {
        private List<AcceptedItem> accepted;  // 成功接收的运单
        private List<RejectedItem> rejected;  // 被拒绝的运单

        @Data
        public static class AcceptedItem {
            private String number;             // 运单号
            private Integer carrier;           // 承运商ID (17Track自动识别的)
            private Integer origin;            // 始发国
            private String email;              // 可选邮箱
            private String lang;               // 语言
            private Object mec;                // 其他参数
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
