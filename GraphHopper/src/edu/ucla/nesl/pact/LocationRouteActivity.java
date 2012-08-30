package edu.ucla.nesl.pact;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import de.jetsli.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: Give a one line description.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class LocationRouteActivity extends Activity {

  private static final String TAG = "LocationRouteActivity";
  AtomicBoolean mConnected;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    findViewById(R.id.btnDefaultRoute).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        sendRequestRouteMessage();
      }
    });
    mConnected = new AtomicBoolean(false);
    Intent intent = new Intent(this, GraphHopperService.class);
    bindService(intent, mServiceConnection, BIND_AUTO_CREATE | BIND_DEBUG_UNBIND);
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
    // Boelter Hall.
    double fromLat = 34.069144;
    double fromLon = -118.443199;

    // Weyburn Terrace (graduate housing).
    double toLat = 34.063666;
    double toLon = -118.451397;

    sendRequestRouteMessage(fromLat, fromLon, toLat, toLon);

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
    Message msg = Message.obtain(null, GraphHopperService.MSG_ROUTE);
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
        case GraphHopperService.MSG_ROUTE:
          Bundle data = msg.getData();
          int[] nodes = data.getIntArray("nodes");
          double[] lats = data.getDoubleArray("latitudes");
          double[] lons = data.getDoubleArray("longitudes");

          StringBuilder builder = new StringBuilder();
          for (int ii = 0; ii < lats.length; ++ii) {
            builder.append(nodes[ii]);
            builder.append(": (");
            builder.append(lats[ii]);
            builder.append(", ");
            builder.append(lons[ii]);
            builder.append(")\n");
          }
          ((TextView) findViewById(R.id.txtRouteDetails)).setText(builder.toString());
          break;
        default:
          super.handleMessage(msg);
      }

    }
  });
}
