package com.simlect.state;

public enum OrderStateEvent {
    PAY_SUCCESS,
    COUPON_RUSH_PAY_SUCCESS,
    USER_CANCEL,
    PAYMENT_TIMEOUT,
    SHIP,
    CONFIRM_RECEIPT,
    PARTIAL_REFUND,
    FULL_REFUND,
    DELETE
}
