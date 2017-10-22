package mam.mam_project1.ar_localizer;

public class AugmentedPoint {
    private String pointName;
    private double pointLat;
    private double pointLon;

    public AugmentedPoint(String pointName, double newLatitude, double newLongitude) {
        this.pointName = pointName;
        this.pointLat = newLatitude;
        this.pointLon = newLongitude;
    }

    public String getPointName() {
        return pointName;
    }

    public double getPointLat() {
        return pointLat;
    }

    public double getPointLon() {
        return pointLon;
    }
}
