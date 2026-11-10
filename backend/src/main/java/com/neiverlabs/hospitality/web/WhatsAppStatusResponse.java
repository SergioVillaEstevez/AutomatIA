package com.neiverlabs.hospitality.web;

public record WhatsAppStatusResponse(
    boolean configured,
    String detail
) {
}
