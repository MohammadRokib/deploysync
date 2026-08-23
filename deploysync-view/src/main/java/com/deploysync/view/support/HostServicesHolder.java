package com.deploysync.view.support;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

@Component
public class HostServicesHolder {
    private HostServices hostServices;

    public void attach(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void openUrl(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
        }
    }
}
