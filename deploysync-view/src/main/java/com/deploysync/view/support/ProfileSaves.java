package com.deploysync.view.support;

import com.deploysync.model.profile.EnvironmentProfile;
import com.deploysync.model.profile.ProfileStore;

public class ProfileSaves {
    private ProfileSaves() {}

    public static boolean confirmAndSave(ProfileStore store, EnvironmentProfile profile) {
        if (store.find(profile.name()).isPresent() && !Popups.confirmOverwrite(profile.name())) {
            return false;
        }
        store.save(profile);
        return true;
    }
}
