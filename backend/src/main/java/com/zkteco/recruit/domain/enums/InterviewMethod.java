package com.zkteco.recruit.domain.enums;

public enum InterviewMethod {
    ONLINE("线上"),
    OFFLINE("线下");

    private final String label;

    InterviewMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
