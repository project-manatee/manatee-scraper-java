package com.manateams.scraper.districts.impl;

import com.manateams.scraper.districts.TEAMSUserType;

/**
 * Created by ehsan on 10/21/14.
 */
public class AustinISDStudent implements TEAMSUserType {

    @Override
    public String teamsHost() {
        return "https://grades.austinisd.org";
    }

    @Override
    public boolean isParent() {
        return false;
    }
}
