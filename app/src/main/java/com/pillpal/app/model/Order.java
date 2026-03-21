package com.pillpal.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.firebase.Timestamp;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String orderId;
    private String status;
    private String date;
    private String total;
    private Timestamp pendingTimestamp;
    private Timestamp approvedTimestamp;
    private Timestamp paymentTimestamp;
    private Timestamp acceptedTimestamp;
    private Timestamp deliveredTimestamp;
}
