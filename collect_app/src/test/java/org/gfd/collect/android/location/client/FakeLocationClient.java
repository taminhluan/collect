package org.gfd.collect.android.location.client;

import android.location.Location;

import com.google.android.gms.location.LocationListener;

public class FakeLocationClient implements LocationClient {
    private boolean failOnStart;
    private LocationClientListener clientListener;
    private LocationListener locationListener;
    private boolean running;
    private boolean locationAvailable = true;
    private Priority priority = Priority.PRIORITY_HIGH_ACCURACY;
    private Location lastLocation;

    // Instrumentation for testing.

    public void setLocationAvailable(boolean available) {
        locationAvailable = available;
    }

    public void setFailOnStart(boolean fail) {
        failOnStart = fail;
    }

    public void receiveFix(Location location) {
        lastLocation = location;
        if (locationListener != null) {
            locationListener.onLocationChanged(location);
        }
    }

    public boolean isRunning() {
        return running;
    }

    // Implementation of the LocationClient interface.

    public void start() {
        running = true;
        if (clientListener != null) {
            if (failOnStart) {
                clientListener.onClientStartFailure();
            } else {
                clientListener.onClientStart();
            }
        }
    }

    public void stop() {
        running = false;
        if (clientListener != null) {
            clientListener.onClientStop();
        }
    }

    public boolean isLocationAvailable() {
        return locationAvailable;
    }

    public void requestLocationUpdates(LocationListener locationListener) {
        this.locationListener = locationListener;
    }

    public void stopLocationUpdates() {
        this.locationListener = null;
    }

    public void setListener(LocationClientListener clientListener) {
        this.clientListener = clientListener;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public boolean isMonitoringLocation() {
        return locationListener != null;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean canSetUpdateIntervals() {
        return false;
    }

    public void setUpdateIntervals(long updateInterval, long fastestUpdateInterval) { }

    public void resetUpdateIntervals() { }
}
