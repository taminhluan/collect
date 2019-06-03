/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gfd.collect.android.location.activities;

import android.location.Location;

import org.robolectric.shadows.ShadowApplication;

public abstract class BaseGeoActivityTest {
    public void setUp() throws Exception {
        ShadowApplication.getInstance().grantPermissions("android.permission.ACCESS_FINE_LOCATION");
        ShadowApplication.getInstance().grantPermissions("android.permission.ACCESS_COARSE_LOCATION");
    }

    protected Location createLocation(String provider, double lat, double lon, double alt, float sd) {
        Location location = new Location(provider);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(alt);
        location.setAccuracy(sd);
        return location;
    }
}
