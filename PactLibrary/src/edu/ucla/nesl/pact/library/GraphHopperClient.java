package edu.ucla.nesl.pact.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maintains service binding to the background GraphHopper service.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class GraphHopperClient {

  private static final String TAG = "GraphHopperClient";
  private static final String GRAPH_HOPPER_SERVICE = "edu.ucla.nesl.pact.LocationRouteService";

  Context mContext;
  Messenger mService;
  Messenger mClient;

  public GraphHopperClient(Context context, Messenger client) {
    mContext = context;
    mClient = client;
  }

  private AtomicBoolean mConnected;

  public void onCreate() {
    mConnected = new AtomicBoolean(false);
    Intent intent = new Intent(GRAPH_HOPPER_SERVICE);
    mContext.bindService(
        intent, mServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND);
  }

  public void onDestroy() {
    if (mConnected.get()) {
      mContext.unbindService(mServiceConnection);
    }
    // TODO: Destroy the GraphHopper and NearbyPlaces instances properly.
  }


  ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
      mService = new Messenger(binder);
      mConnected.set(true);
      try {
        mClient.send(Message.obtain(null, Messages.MSG_GRAPH_HOPPER_CONNECTED));
      } catch (RemoteException ex) {
        Log.e(TAG, "RemoteException!");
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mConnected.set(false);
      try {
        mClient.send(Message.obtain(null, Messages.MSG_GRAPH_HOPPER_DISCONNECTED));
      } catch (RemoteException ex) {
        Log.e(TAG, "RemoteException!");
      }
    }
  };

  public void askForRoute(double fromLat, double fromLon, double toLat, double toLon) {
    if (!mConnected.get()) {
      Log.d(TAG, "Not yet connected to the GraphHopper service! Discarding command.");
    }

    Bundle data = new Bundle();
    data.putDouble("fromLat", fromLat);
    data.putDouble("fromLon", fromLon);
    data.putDouble("toLat", toLat);
    data.putDouble("toLon", toLon);
    trySendMessage(Messages.MSG_ASK_FOR_ROUTE, data);
  }

  public void queryRouteNodeNearLocation(double lat, double lon) {
    Bundle data = new Bundle();
    data.putDouble(Messages.LAT_KEY, lat);
    data.putDouble(Messages.LON_KEY, lon);
    trySendMessage(Messages.MSG_QUERY_ROUTE_NODE_NEAR_LOCATOIN, data);
  }

  public void queryNearbyPlacesNearLocation(double lat, double lon, double radius) {
    Bundle data = new Bundle();
    data.putDouble(Messages.LAT_KEY, lat);
    data.putDouble(Messages.LON_KEY, lon);
    data.putDouble(Messages.RADIUS_METERS_KEY, radius);
    trySendMessage(Messages.MSG_QUERY_NEARBY_PLACES_FOR_LOCATION, data);
  }

  private void trySendMessage(int what, Bundle data) {
    Message msg = Message.obtain(null, what);
    msg.setData(data);
    msg.replyTo = mClient;
    try {
      mService.send(msg);
    } catch (RemoteException ex) {
      Log.e(TAG, "RemoteException: trying to send request to location router.");
    }
  }
}
