package com.deploysync.model.profile;

public record EnvironmentProfile(
        String name,
        String masterEarPath,
        String weblogicHost,
        int    weblogicPort,
        String weblogicUsername,
        String weblogicAppname,
        String weblogicTarget) {

    public static EnvironmentProfile blank(String name) {
        return new EnvironmentProfile(name, "", "", 7001, "", "", "");
    }

    public EnvironmentProfile withName(String name) {
        return new EnvironmentProfile(name, masterEarPath, weblogicHost, weblogicPort,
                weblogicUsername, weblogicAppname, weblogicTarget);
    }

    public EnvironmentProfile withMasterEarPath(String masterEarPath) {
        return new EnvironmentProfile(name, masterEarPath, weblogicHost, weblogicPort,
                weblogicUsername, weblogicAppname, weblogicTarget);
    }

    public EnvironmentProfile withWeblogicFields(String weblogicHost, int weblogicPort, String weblogicUsername,
                                                 String weblogicAppname, String weblogicTarget) {
        return new EnvironmentProfile(name, masterEarPath, weblogicHost, weblogicPort,
                weblogicUsername, weblogicAppname, weblogicTarget);
    }
}
