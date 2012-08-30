package edu.ucla.nesl.pact;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;

import edu.ucla.nesl.pact.library.Messages;

/**
 * A service that provides a route between two points.
 *
 * Loads a map of Westwood (open-street-maps) and uses GraphHopper for routing. See the companion
 * {@link LocationRouteActivity} for an usage example.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class GraphHopperService extends Service {

  private static final String TAG = "GraphHopperService";


  private NearbyPlaces mNearbyPlaces;
  private GraphHopper mGraphHopper;


  @Override
  public IBinder onBind(Intent intent) {
    return mMessenger.getBinder();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    try {
      mNearbyPlaces = new NearbyPlaces();
      mGraphHopper =  new GraphHopper();

      InputStreamReader reader =
          new InputStreamReader(getAssets().open("los-angeles.amenities.json"));
      mNearbyPlaces.loadFromJson(reader);
    } catch (IOException ex) {
      Log.e(TAG, "onCreate(): IOException");
    }

    try {
      mGraphHopper.loadFromOsm(this, "westwood.osm");
    } catch (IOException ex) {
      Log.e(TAG, "onCreate(): IOException");
    }
  }

  final Messenger mMessenger = new Messenger(new IncomingHandler());

  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case Messages.MSG_ASK_FOR_ROUTE:
          mGraphHopper.handleRouteMessage(msg);
          break;
        case Messages.MSG_ASK_FOR_NEARBY_PLACES:
          mNearbyPlaces.handleNearbyPlacesMessage(msg);
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }


}
