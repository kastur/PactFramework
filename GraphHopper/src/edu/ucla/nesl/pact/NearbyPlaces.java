package edu.ucla.nesl.pact;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import de.jetsli.graph.trees.QuadTree;
import de.jetsli.graph.trees.QuadTreeSimple;
import de.jetsli.graph.util.CoordTrig;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.ucla.nesl.pact.library.Messages;

public class NearbyPlaces {

  private static final String TAG = "NearbyPlaces";
  private static final int NUMBER_OF_POPULAR_PLACES_TO_LOAD = 3;
  private QuadTree<String> mIndex;
  private AtomicBoolean mLoaded;

  public NearbyPlaces() {
    mLoaded = new AtomicBoolean(false);
  }

  public void loadFromJson(Reader reader) {
    mIndex = new QuadTreeSimple<String>();
    final Reader final_reader = reader;
    Thread loader = new Thread("PlacesManagerLoader") {
      @Override
      public void run() {
        Log.d(TAG, "----------STARTED TO LOAD PLACES");
        JsonElement root = new JsonParser().parse(final_reader);
        int count = 0;
        for (Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
          if (count > NUMBER_OF_POPULAR_PLACES_TO_LOAD)
            break;
          final String amenity = entry.getKey();
          for (JsonElement o : entry.getValue().getAsJsonArray()) {
            JsonArray list = o.getAsJsonArray();
            final double lon = list.get(0).getAsDouble();
            final double lat = list.get(1).getAsDouble();

            // If we want to, we can also put the name of the place into the quad-tree.
            // String name = list.get(2).getAsString();
            mIndex.add(lat, lon, amenity);
          }
          Log.d(TAG, "------LOADED: #" + count + " : " + amenity);
          count++;
        }
        mLoaded.set(true);
        Log.d(TAG, "----------FINISHED LOADING PLACES");
      }
    };

    // Start indexing places in the background.
    loader.start();
  }

  public void clear() {
    mIndex.clear();
  }

  public void handleNearbyPlacesMessage(Message msg) {
    Bundle data = msg.getData();

    Collection<CoordTrig<String>> nodes = getNearbyPlaces(
        data.getDouble(Messages.LAT_KEY, 0.0),
        data.getDouble(Messages.LON_KEY, 0.0),
        data.getDouble(Messages.RADIUS_METERS_KEY, 0.0));

    HashSet<String> hashSet = new HashSet<String>();
    for (CoordTrig<String> n : nodes) {
      hashSet.add(n.getValue());
    }

    ArrayList<String> places = new ArrayList<String>(hashSet);

    Bundle responseData = new Bundle();
    responseData.putStringArrayList(Messages.PLACE_TYPES_KEY, places);

    Message responseMsg = Message.obtain(null, Messages.MSG_PLACES);
    responseMsg.setData(responseData);

    try {
      msg.replyTo.send(responseMsg);
    } catch (RemoteException ex) {
      ex.printStackTrace();
    }

  }

  private Collection<CoordTrig<String>> getNearbyPlaces(double lat, double lon, double radius_meters) {
    if (!mLoaded.get()) {
      return new ArrayList<CoordTrig<String>>();
    } else {
      // Convert radius (in meters) to kilometers.
      return mIndex.getNodes(lat, lon, radius_meters / 1000.0);
    }
  }
}
