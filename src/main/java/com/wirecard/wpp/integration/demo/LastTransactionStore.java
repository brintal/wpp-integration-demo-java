package com.wirecard.wpp.integration.demo;

import org.springframework.stereotype.Component;

@Component
public class LastTransactionStore {

    private String lastTransactionId;

    public String getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(String lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
    }
}
