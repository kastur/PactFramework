package edu.ucla.pact.mobility;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interactive map with shortest path route between start and end points.
 *
 * Calls the externally running GraphHopper routing service for the shortest path.
 * Displays map markers for the route along the path.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class MobilityMapActivity extends MapActivity {

  private static final String TAG = "MobilityMapActivity";
  private static final int MSG_ROUTE = 0;
  private final static GeoPoint DEFAULT_START_POINT =
      new GeoPoint(34069144, -118443199);  // Boelter Hall.
  private final static GeoPoint DEFAULT_END_POINT =
      new GeoPoint(34063666, -118451397);  // Weyburn terrace.

  private AtomicBoolean mConnected;
  private MapView mMapView;
  private ArrayOverlay mArrayOverlay;
  private Marker mStartMarker;
  private Marker mEndMarker;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    mMapView = (MapView) findViewById(R.id.map);

    mConnected = new AtomicBoolean(false);
    Intent intent = new Intent("edu.ucla.nesl.pact.LocationRouteService");
    bindService(intent, mServiceConnection, BIND_AUTO_CREATE | BIND_DEBUG_UNBIND);

    mMapView.setBuiltInZoomControls(true);
    List<Overlay> mapOverlays = mMapView.getOverlays();
    Drawable drawable = this.getResources().getDrawable(R.drawable.mm_20_green);
    mArrayOverlay = new ArrayOverlay(drawable);
    mapOverlays.add(mArrayOverlay);

    Drawable startDrawable = this.getResources().getDrawable(R.drawable.dd_start);
    mStartMarker = new Marker(startDrawable, DEFAULT_START_POINT, "START", "");
    mapOverlays.add(mStartMarker);

    Drawable endDrawable = this.getResources().getDrawable(R.drawable.dd_end);
    mEndMarker = new Marker(endDrawable, DEFAULT_END_POINT, "END", "");
    mapOverlays.add(mEndMarker);
  }

  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  Messenger mLocationRouter;
  ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
      mLocationRouter = new Messenger(binder);
      mConnected.set(true);
      sendRequestRouteMessage();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mConnected.set(false);
    }
  };

  private void sendRequestRouteMessage() {
    sendRequestRouteMessage(mStartMarker.getCenter().getLatitudeE6() / 1e6,
                            mStartMarker.getCenter().getLongitudeE6() / 1e6,
                            mEndMarker.getCenter().getLatitudeE6() / 1e6,
                            mEndMarker.getCenter().getLongitudeE6() / 1e6);
  }

  private void sendRequestRouteMessage(double fromLat, double fromLon, double toLat, double toLon) {
    if (!mConnected.get()) {
      Log.d(TAG, "Not yet connected to the LocationRouter service! Discarding command.");
    }

    Bundle data = new Bundle();
    data.putDouble("fromLat", fromLat);
    data.putDouble("fromLon", fromLon);
    data.putDouble("toLat", toLat);
    data.putDouble("toLon", toLon);
    Message msg = Message.obtain(null, MSG_ROUTE);
    msg.setData(data);
    msg.replyTo = mMessenger;

    try {
      mLocationRouter.send(msg);
    } catch (RemoteException ex) {
      Log.e(TAG, "RemoteException: trying to send request to location router.");
    }
  }

  Messenger mMessenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ROUTE:
          Bundle data = msg.getData();
          int[] nodes = data.getIntArray("nodes");
          double[] lats = data.getDoubleArray("latitudes");
          double[] lons = data.getDoubleArray("longitudes");

          mArrayOverlay.clear();
          for (int ii = 0; ii < lats.length; ++ii) {
            final String nodeId = Integer.toString(nodes[ii]);
            GeoPoint point = new GeoPoint((int) (lats[ii] * 1E6), (int) (lons[ii] * 1E6));
            OverlayItem item = new OverlayItem(point, nodeId, nodeId);
            mArrayOverlay.addOverlay(item);
          }

          mMapView.getController()
              .zoomToSpan(mArrayOverlay.getLatSpanE6(), mArrayOverlay.getLonSpanE6());
          mMapView.getController().setCenter(mArrayOverlay.getCenter());
          break;
        default:
          super.handleMessage(msg);
      }
    }
  });

  private class Marker extends ItemizedOverlay {

    private OverlayItem mItem;
    private int mSize;
    private String mStrA;
    private String mStrB;
    private Drawable mDrawable;

    private OverlayItem inDrag = null;
    private ImageView mDragImage = null;
    private boolean mDragging;


    public Marker(Drawable drawable, GeoPoint point, String a, String b) {
      super(boundCenterBottom(drawable));
      this.mDrawable = drawable;
      mStrA = a;
      mStrB = b;

      mDragging = false;
      mSize = 1;
      mDragImage = (ImageView) findViewById(R.id.drag);

      setPoint(point);
    }

    public void setPoint(GeoPoint point) {
      mItem = new OverlayItem(point, mStrA, mStrB);
      populate();
    }

    @Override
    public int size() {
      return mSize;
    }

    @Override
    protected OverlayItem createItem(int i) {
      return mItem;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {

      final int action = event.getAction();
      final int touchX = (int) event.getX();
      final int touchY = (int) event.getY();
      boolean eventHandled = false;

      switch (action) {
        case MotionEvent.ACTION_UP:
          if (!mDragging) {
            break;
          }
          // Drag finished.
          mDragging = false;
          mDragImage.setVisibility(View.GONE);
          GeoPoint geopoint = mMapView.getProjection().fromPixels(touchX, touchY);
          setPoint(geopoint);
          showMarker();
          sendRequestRouteMessage(); // updates the route.
          eventHandled = true;
          break;
        case MotionEvent.ACTION_DOWN:
          Point point = new Point(0, 0);
          mMapView.getProjection().toPixels(mItem.getPoint(), point);
          if (!hitTest(mItem, mDrawable, touchX - point.x, touchY - point.y)) {
            break;
          }
          hideMarker();
          mDragging = true;
        case MotionEvent.ACTION_MOVE:
          if (!mDragging) {
            break;
          }
          setDragImagePosition(touchX, touchY);
          mDragImage.setVisibility(View.VISIBLE);
          eventHandled = true;
          break;
      }

      return eventHandled || super.onTouchEvent(event, mapView);
    }

    private void showMarker() {
      mSize = 1;
      populate();
    }

    private void hideMarker() {
      mSize = 0;
      populate();
    }

    private void setDragImagePosition(int x, int y) {
      RelativeLayout.LayoutParams lp =
          (RelativeLayout.LayoutParams) mDragImage.getLayoutParams();
      lp.setMargins(x - (mDragImage.getWidth() / 2), y - mDragImage.getHeight(), 0, 0);
      mDragImage.setLayoutParams(lp);
    }
  }

  private class ArrayOverlay extends ItemizedOverlay {

    private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

    public ArrayOverlay(Drawable defaultMarker) {
      super(boundCenterBottom(defaultMarker));
    }


    @Override
    protected OverlayItem createItem(int i) {
      return mOverlays.get(i);
    }

    @Override
    public int size() {
      return mOverlays.size();
    }

    public void clear() {
      mOverlays.clear();
      populate();
    }

    public void addOverlay(OverlayItem overlay) {
      mOverlays.add(overlay);
      populate();
    }
  }

}
