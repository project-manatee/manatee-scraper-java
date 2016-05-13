package com.manateams.scraper.districts.impl;

import com.manateams.scraper.districts.TEAMSUserType;

public class AustinISDParent implements TEAMSUserType {
    @Override
    public String teamsHost() {
        return "https://grades.austinisd.org";
    }

    @Override
    public boolean isParent() {
        return true;
    }
}
