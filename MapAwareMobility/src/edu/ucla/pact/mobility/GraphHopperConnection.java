package edu.ucla.pact.mobility;

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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: Give a one line description.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class GraphHopperConnection {

  private static final String TAG = "GraphHopperConnection";


  public static final int MSG_ROUTE = 0;
  public static final int MSG_LOOKUP_LOCATION = 1;
  public static final int MSG_LOOKUP_NODES = 2;

  public static final int MSG_CONNECTED = 3;
  public static final int MSG_DISCONNECTED = 4;

  Context mContext;
  Messenger mService;
  Messenger mClient;

  public GraphHopperConnection(Context context, Messenger client) {
    mContext = context;
    mClient = client;
  }

  private AtomicBoolean mConnected;

  public void onCreate() {
    mConnected = new AtomicBoolean(false);
    Intent intent = new Intent("edu.ucla.nesl.pact.LocationRouteService");
    mContext.bindService(
        intent, mServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND);
  }

  public void onDestroy() {
    if (mConnected.get()) {
      mContext.unbindService(mServiceConnection);
    }
  }


  ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
      mService = new Messenger(binder);
      mConnected.set(true);
      try {
        mClient.send(Message.obtain(null, MSG_CONNECTED));
      } catch (RemoteException ex) {
        Log.e(TAG, "RemoteException!");
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mConnected.set(false);
      try {
        mClient.send(Message.obtain(null, MSG_DISCONNECTED));
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
    trySendMessage(MSG_ROUTE, data);
  }

  public void askForNodeId(double lat, double lon) {
    Bundle data = new Bundle();
    data.putDouble("lat", lat);
    data.putDouble("lon", lon);
    trySendMessage(MSG_LOOKUP_LOCATION, data);
  }

  public void askForLocations(ArrayList<Integer> nodes) {
    Bundle data = new Bundle();
    data.putIntegerArrayList("nodes", nodes);
    trySendMessage(MSG_LOOKUP_NODES, data);
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
