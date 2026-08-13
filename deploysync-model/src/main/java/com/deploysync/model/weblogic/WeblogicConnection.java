package com.deploysync.model.weblogic;

public record WeblogicConnection(String host, int port, String username, String password,
                                 String appName, String target) {
    public String baseUrl() {
        return "http://" + host + ":" + port + "/management/weblogic/latest";
    }
}
