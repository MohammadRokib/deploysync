package com.deploysync.view.support;

import org.springframework.stereotype.Component;

@Component
public class ActiveProfileHolder {
    private volatile String selectedProfileName = "";

    public String get() {
        return selectedProfileName;
    }

    public void set(String name) {
        this.selectedProfileName = (name == null) ? "" : name;
    }
}
