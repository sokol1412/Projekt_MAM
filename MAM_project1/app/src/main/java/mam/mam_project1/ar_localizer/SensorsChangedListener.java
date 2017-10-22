package mam.mam_project1.ar_localizer;

import android.location.Location;

public interface SensorsChangedListener {
    void onLocationChanged(Location currentLocation);
    void onAzimuthChanged(float azimuthFrom, float azimuthTo);
}
