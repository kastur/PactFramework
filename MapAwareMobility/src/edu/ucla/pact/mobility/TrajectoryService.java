package edu.ucla.pact.mobility;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

/**
 * Figures out if user is *visiting* a place, and tracks trajectory from that point.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class TrajectoryService extends Service {

  private GraphHopperConnection mConn;

  private static final int MSG_LOCATION_CHANGED = 0;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mConn = new GraphHopperConnection(this, mMessenger);
    mConn.onCreate();

    LocationManager locationManager = (LocationManager)getSystemService("native_location");
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
  }

  @Override
  public void onDestroy() {
    if (mConn != null)
      mConn.onDestroy();
    super.onDestroy();

  }
  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_LOCATION_CHANGED:
          Location location = (Location)msg.obj;
          updateTrajectory(location);
          break;
        case GraphHopperConnection.MSG_LOOKUP_LOCATION:
          // Got node for location.
          break;
        case GraphHopperConnection.MSG_LOOKUP_NODES:
          // Got locations for nodes.
          break;
      }
      super.handleMessage(msg);

    }
  };
  private Messenger mMessenger = new Messenger(mHandler);

  private LocationListener mLocationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {
      Message msg = Message.obtain(null, MSG_LOCATION_CHANGED);
      msg.obj = location;
      mHandler.sendMessage(msg);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
  };

  private void updateTrajectory(Location location) {

  }

}
