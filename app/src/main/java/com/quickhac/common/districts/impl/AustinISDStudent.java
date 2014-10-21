package com.quickhac.common.districts.impl;

import com.quickhac.common.districts.TEAMSUserType;

/**
 * Created by ehsan on 10/21/14.
 */
public class AustinISDStudent implements TEAMSUserType {

    @Override
    public String teamsHost() {
        return "my-teams.austinisd.org";
    }

    @Override
    public boolean isParent() {
        return false;
    }
}
