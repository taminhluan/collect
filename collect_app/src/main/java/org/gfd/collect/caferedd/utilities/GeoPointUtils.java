package org.gfd.collect.caferedd.utilities;

public class GeoPointUtils {

    private GeoPointUtils() {

    }

    /**
     * Corrects location provider names so "gps" displays as "GPS" in user-facing messaging.
     */
    public static String capitalizeGps(String locationProvider) {
        return "gps".equals(locationProvider) ? "GPS" : locationProvider;
    }
}
