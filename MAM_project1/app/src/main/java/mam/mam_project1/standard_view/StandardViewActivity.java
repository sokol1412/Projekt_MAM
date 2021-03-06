package mam.mam_project1.standard_view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoDisplayActivity;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import mam.mam_project1.R;
import mam.mam_project1.ar_localizer.AugmentedPoint;
import mam.mam_project1.ar_localizer.CurrentAzimuth;
import mam.mam_project1.ar_localizer.CurrentLocation;
import mam.mam_project1.ar_localizer.SensorsChangedListener;
import mam.mam_project1.ar_recognition.RecognitionViewActivity;
import mam.mam_project1.shaker.Shaker;
import mam.mam_project1.vr_view.CardboardViewActivity;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;


public class StandardViewActivity extends VideoDisplayActivity
        implements View.OnTouchListener, SensorsChangedListener {

    private int status = 0;
    private Shaker shaker;
    private static Camera camera = null;

    private List<AugmentedPoint> augmentedPoints;

    private static double AZIMUTH_ACCURACY = 5;
    private double realAzimuth = 0;
    private double mMyLatitude = 0;
    private double mMyLongitude = 0;
    private Toast ARToast = null;

    private CurrentAzimuth currentAzimuth;
    private CurrentLocation currentLocation;

    // size of the minimum square which user must select in order to track object
    private final static int MINIMUM_SQUARE_SIZE = 20;

    //constant for defining the time duration between the click that can be considered as double-tap
    private final static int MAX_DURATION = 200;

    private Point2D_I32 firstClick = new Point2D_I32();
    private Point2D_I32 secondClick = new Point2D_I32();

    //clickTime needed for detecting doubletap event
    private long clickTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shaker = new Shaker(getApplicationContext());

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout controls = (LinearLayout) inflater.inflate(R.layout.standard_view, null);
        controls.setBackgroundColor(Color.BLACK);

        LinearLayout parent = getViewContent();
        parent.addView(controls);

        FrameLayout iv = getViewPreview();
        iv.setOnTouchListener(this);

        startObjectTracking();
        setupListeners();
        setAugmentedPoints();
        ARToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    public void changeToVRActivity(View view) {
        Intent switcher = new Intent(this, CardboardViewActivity.class);
        startActivity(switcher);
    }

    public void changeToRecognitionViewActivity(View view) {
        Intent switcher = new Intent(this, RecognitionViewActivity.class);
        switcher.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(switcher);
    }

    @Override
    protected void onResume() {
        currentAzimuth.start();
        currentLocation.start();
        super.onResume();
        startObjectTracking();
    }

    @Override
    protected void onStop() {
        currentAzimuth.stop();
        currentLocation.stop();
        super.onStop();
    }

    private void setupListeners() {
        currentLocation = new CurrentLocation(this);
        currentLocation.buildGoogleApiClient(this);
        currentLocation.start();

        currentAzimuth = new CurrentAzimuth(this, this);
        currentAzimuth.start();
    }


    private void setAugmentedPoints() {
        augmentedPoints = new ArrayList<AugmentedPoint>();
        augmentedPoints.add(new AugmentedPoint(
                "London",
                51.509865,
                -0.118092));
        augmentedPoints.add(new AugmentedPoint(
                "Warsaw",
                52.237049,
                21.017532));
        augmentedPoints.add(new AugmentedPoint(
                "Helsinki",
                60.192059,
                24.945831));
        augmentedPoints.add(new AugmentedPoint(
                "Madrid",
                40.416775,
                -3.703790));
        augmentedPoints.add(new AugmentedPoint(
                "Ottawa",
                45.425533,
                -75.692482));
    }

    public double calculateTeoreticalAzimuth(double pointLat, double pointLon) {
        //this function returns theorethical azimuth calculated from device current location to some POI location
        double dX = pointLat - mMyLatitude;
        double dY = pointLon - mMyLongitude;

        double phiAngle;
        double tanPhi;

        tanPhi = Math.abs(dY / dX);
        phiAngle = Math.atan(tanPhi);
        phiAngle = Math.toDegrees(phiAngle);

        // specify quaters (from 1 to 4)
        if (dX > 0 && dY > 0) {
            return phiAngle;
        } else if (dX < 0 && dY > 0) {
            return 180 - phiAngle;
        } else if (dX < 0 && dY < 0) {
            return 180 + phiAngle;
        } else if (dX > 0 && dY < 0) {
            return 360 - phiAngle;
        }

        return phiAngle;
    }

    private List<Double> calculateAzimuthAccuracy(double azimuth) {
        double minAngle = azimuth - AZIMUTH_ACCURACY;
        double maxAngle = azimuth + AZIMUTH_ACCURACY;
        List<Double> minMax = new ArrayList<Double>();

        if (minAngle < 0)
            minAngle += 360;

        if (maxAngle >= 360)
            maxAngle -= 360;

        minMax.clear();
        minMax.add(minAngle);
        minMax.add(maxAngle);

        return minMax;
    }

    private boolean isBetween(double minAngle, double maxAngle, double azimuth) {
        if (minAngle > maxAngle) {
            if (isBetween(0, maxAngle, azimuth) && isBetween(minAngle, 360, azimuth))
                return true;
        } else {
            if (azimuth > minAngle && azimuth < maxAngle)
                return true;
        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        mMyLatitude = location.getLatitude();
        mMyLongitude = location.getLongitude();
    }

    @Override
    public void onAzimuthChanged(float azimuthChangedFrom, float azimuthChangedTo) {
        if (mMyLatitude == 0 || mMyLongitude == 0)
            return;
        realAzimuth = azimuthChangedTo;

        for (AugmentedPoint a : augmentedPoints) {
            double pointAzimuth = calculateTeoreticalAzimuth(a.getPointLat(), a.getPointLon());
            double minAngle = calculateAzimuthAccuracy(pointAzimuth).get(0);
            double maxAngle = calculateAzimuthAccuracy(pointAzimuth).get(1);
            float[] calculatedDistance = new float[3];
            Location.distanceBetween(mMyLatitude, mMyLongitude, a.getPointLat(), a.getPointLon(), calculatedDistance);

            if (isBetween(minAngle, maxAngle, realAzimuth)) {
                ARToast.setText("City: " + a.getPointName() + "\nDistance: " + Math.round(calculatedDistance[0] * 0.001) + " km");
                ARToast.show();
            }
        }
    }

    private static int getClosestSize(List<Camera.Size> sizes, int width, int height) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size s = sizes.get(i);

            int dx = s.width - width;
            int dy = s.height - height;

            int score = dx * dx + dy * dy;
            if (score < bestScore) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }

    @Override
    protected Camera openConfigureCamera(Camera.CameraInfo cameraInfo) {
        camera = Camera.open();
        Camera.Parameters param = camera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(getClosestSize(sizes, 320, 240));
        param.setPreviewSize(s.width, s.height);
        camera.setParameters(param);

        return camera;
    }

    private void startObjectTracking() {
        ImageType imageType = ImageType.single(ImageUInt8.class);
        //circular tracker - good and robust, but unable to recover track when focus is lost
        TrackerObjectQuad tracker = FactoryTrackerObjectQuad.circulant(null, ImageUInt8.class);

        setProcessing(new TrackingProcessing(tracker, imageType));
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (status == 0) {
            if (MotionEvent.ACTION_DOWN == motionEvent.getActionMasked()) {
                firstClick.set((int) motionEvent.getX(), (int) motionEvent.getY());
                secondClick.set((int) motionEvent.getX(), (int) motionEvent.getY());
                status = 1;
            }
        } else if (status == 1) {
            if (MotionEvent.ACTION_MOVE == motionEvent.getActionMasked()) {
                secondClick.set((int) motionEvent.getX(), (int) motionEvent.getY());
            } else if (MotionEvent.ACTION_UP == motionEvent.getActionMasked()) {
                secondClick.set((int) motionEvent.getX(), (int) motionEvent.getY());
                status = 2;
            }
        } else if (status == 3) {
            if (MotionEvent.ACTION_UP == motionEvent.getActionMasked()) {
                clickTime = System.currentTimeMillis();
            } else if (MotionEvent.ACTION_DOWN == motionEvent.getActionMasked()) {
                if (System.currentTimeMillis() - clickTime <= MAX_DURATION) {
                    status = 0;
                }
            }
        }

        return true;
    }

    protected class TrackingProcessing<T extends ImageBase> extends VideoImageProcessing<MultiSpectral<ImageUInt8>> {

        T input;
        ImageType<T> inputType;

        TrackerObjectQuad tracker;
        boolean visible;

        Quadrilateral_F64 location = new Quadrilateral_F64();

        Paint objectMarkingPaint = new Paint();
        Paint trackingPaint = new Paint();

        protected TrackingProcessing(TrackerObjectQuad tracker, ImageType<T> inputType) {
            super(ImageType.ms(3, ImageUInt8.class));
            this.inputType = inputType;

            if (inputType.getFamily() == ImageType.Family.SINGLE_BAND) {
                input = inputType.createImage(1, 1);
            }

            status = 0;
            this.tracker = tracker;

            objectMarkingPaint.setColor(Color.argb(0xFF / 2, 0xFF, 0, 0));

            trackingPaint.setColor(Color.BLUE);
            trackingPaint.setStrokeWidth(3f);
            trackingPaint.setStyle(Paint.Style.STROKE);

        }

        @Override
        protected void process(MultiSpectral<ImageUInt8> input, Bitmap output, byte[] storage) {
            updateTracker(input);
            visualize(input, output, storage);
        }

        private void updateTracker(MultiSpectral<ImageUInt8> color) {
            if (inputType.getFamily() == ImageType.Family.SINGLE_BAND) {
                input.reshape(color.width, color.height);
                ConvertImage.average(color, (ImageUInt8) input);
            } else {
                input = (T) color;
            }

            if (status == 2) {
                imageToOutput(firstClick.x, firstClick.y, location.a);
                imageToOutput(secondClick.x, secondClick.y, location.c);

                makeInBounds(location.a);
                makeInBounds(location.c);

                if (checkMovement(location.a, location.c)) {
                    location.b.set(location.c.x, location.a.y);
                    location.d.set(location.a.x, location.c.y);

                    tracker.initialize(input, location);
                    visible = true;
                    status = 3;
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StandardViewActivity.this, "Select larger area", Toast.LENGTH_SHORT).show();
                        }
                    });
                    status = 0;
                }
            } else if (status == 3) {
                visible = tracker.process(input, location);
            }
        }

        private void visualize(MultiSpectral<ImageUInt8> color, Bitmap output, byte[] storage) {
            ConvertBitmap.multiToBitmap(color, output, storage);
            Canvas canvas = new Canvas(output);

            if (status == 1) {
                Point2D_F64 a = new Point2D_F64();
                Point2D_F64 b = new Point2D_F64();

                imageToOutput(firstClick.x, firstClick.y, a);
                imageToOutput(secondClick.x, secondClick.y, b);

                canvas.drawRect((int) a.x, (int) a.y, (int) b.x, (int) b.y, objectMarkingPaint);
            } else if (status >= 2) {
                if (visible) {
                    Quadrilateral_F64 q = location;
                    if (shaker.shakeEventOccurred) {
                        trackingPaint.setColor(shaker.randomColor);
                        shaker.shakeEventOccurred = false;
                    }
                    canvas.drawRect((int) q.a.x, (int) q.a.y, (int) q.c.x, (int) q.c.y, trackingPaint);
                }
            }
        }

        private void makeInBounds(Point2D_F64 p) {
            if (p.x < 0) p.x = 0;
            else if (p.x >= input.width)
                p.x = input.width - 1;

            if (p.y < 0) p.y = 0;
            else if (p.y >= input.height)
                p.y = input.height - 1;

        }

        private boolean checkMovement(Point2D_F64 a, Point2D_F64 b) {
            if (Math.abs(a.x - b.x) < MINIMUM_SQUARE_SIZE)
                return false;
            if (Math.abs(a.y - b.y) < MINIMUM_SQUARE_SIZE)
                return false;

            return true;
        }
    }
}
