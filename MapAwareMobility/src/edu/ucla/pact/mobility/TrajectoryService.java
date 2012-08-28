package edu.ucla.pact.mobility;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import java.util.Vector;

/**
 * Figures out if user is *visiting* a place, and tracks trajectory from that point.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class TrajectoryService extends Service {

  private GraphHopperConnection mConn;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mConn = new GraphHopperConnection(this, mMessenger);
    mConn.onCreate();
  }

  @Override
  public void onDestroy() {
    if (mConn != null)
      mConn.onDestroy();
    super.onDestroy();

  }

  private void answerPathQuery(Messenger client) {
    Bundle data = new Bundle();
    data.putDoubleArray("latitudes", lats);
    data.putDoubleArray("longitudes", lon);
    data.putIntArray("nodes", nodes);
  }

  private Messenger mMessenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case GraphHopperConnection.MSG_LOOKUP_LOCATION:
          // Got node for location.
          break;
        case GraphHopperConnection.MSG_LOOKUP_NODES:
          // Got locations for nodes.
          break;
      }
      super.handleMessage(msg);

    }
  });

  private Messenger mPathServer = new Messenger(new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch(msg.what) {
        case MSG_PATH:
          answerPathQuery(msg.replyTo);
      }
      super.handleMessage(msg);

    }
  });

}
