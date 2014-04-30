package ru.flexpay.eirc.payments_communication.entity;

import java.util.Locale;

/**
 * @author Pavel Sknar
 */
public class ResponseContent {
    private Long statusCode;
    private String statusMessage;

    public ResponseContent() {
    }

    public ResponseContent(ResponseStatus responseStatus) {
        this.setStatusCode(responseStatus.getId());
        this.setStatusMessage(responseStatus.getLabel(new Locale("ru")));
    }

    public Long getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Long statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
