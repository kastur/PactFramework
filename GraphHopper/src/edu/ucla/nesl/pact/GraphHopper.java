package edu.ucla.nesl.pact;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import de.jetsli.graph.reader.OSMReader;
import de.jetsli.graph.routing.DijkstraBidirection;
import de.jetsli.graph.routing.Path;
import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.storage.Location2IDFullIndex;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.ucla.nesl.pact.library.Messages;

/**
 * TODO: Give a one line description.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class GraphHopper {
  private static final String TAG = "GraphHopper";
  private AtomicBoolean mLoaded;
  private Graph mGraph;
  private Location2IDFullIndex mLocIndex;

  public GraphHopper() {
    mLoaded = new AtomicBoolean(false);
  }

  public void loadFromOsm(Context context, String fileName) throws IOException {
    final String storageDirectory = Environment.getDataDirectory().getPath() + "/graph_storage";
    new File(storageDirectory).mkdir();

    final int storageSize = 5000000;
    OSMReader reader = new OSMReader(storageDirectory, storageSize);

    {
      InputStream inputStream = context.getAssets().open("westwood.osm");
      try {
        reader.preprocessAcceptHighwaysOnly(inputStream);
      } catch (IOException ex) {
        System.err.println("Error!");
      } catch (XmlPullParserException ex) {
        System.err.println("Error!");
      }
    }

    {
      InputStream inputStream = context.getAssets().open("westwood.osm");
      reader.writeOsm2Graph(inputStream);
    }

    mGraph = reader.getGraph();
    mLocIndex = new Location2IDFullIndex(mGraph);
    Log.d(TAG, "----------SUCCESS! Loaded map.");

    mLoaded.set(true);
  }

  public void handleRouteMessage(final Message msg) {
    if (!mLoaded.get()) {
      Log.e(TAG, "Map is not loaded yet! Sending empty route!");
      handleMessageWhenNotLoaded(msg);
      return;
    }

    Bundle data = msg.getData();
    double fromLat = data.getDouble("fromLat", 0.0);
    double fromLon = data.getDouble("fromLon", 0.0);
    double toLat = data.getDouble("toLat", 0.0);
    double toLon = data.getDouble("toLon", 0.0);

    int fromId = mLocIndex.findID(fromLat, fromLon);
    int toId = mLocIndex.findID(toLat, toLon);

    DijkstraBidirection router = new DijkstraBidirection(mGraph);
    Path path = router.calcPath(fromId, toId);

    double latitudes[] = new double[path.locations()];
    double longitudes[] = new double[path.locations()];
    int nodes[] = new int[path.locations()];
    for (int ii = 0; ii < path.locations(); ++ii) {
      final int node_id = path.location(ii);
      nodes[ii] = node_id;
      latitudes[ii] = mGraph.getLatitude(node_id);
      longitudes[ii] = mGraph.getLongitude(node_id);
    }
    sendRouteMessage(msg.replyTo, nodes, latitudes, longitudes);
  }

  private void handleMessageWhenNotLoaded(final Message msg) {
    Bundle data = msg.getData();
    double fromLat = data.getDouble("fromLat", 0.0);
    double fromLon = data.getDouble("fromLon", 0.0);
    double toLat = data.getDouble("toLat", 0.0);
    double toLon = data.getDouble("toLon", 0.0);

    double latitudes[] = new double[]{fromLat, toLat};
    double longitudes[] = new double[]{fromLon, toLon};
    int nodes[] = new int[]{-1, -1};

    Bundle bundle = new Bundle();
    bundle.putIntArray("nodes", nodes);
    bundle.putDoubleArray("latitudes", latitudes);
    bundle.putDoubleArray("longitudes", longitudes);
    sendRouteMessage(msg.replyTo, nodes, latitudes, longitudes);
  }

  private void sendRouteMessage(
      Messenger replyTo, int[] nodes, double[] latitudes, double[] longitudes) {
    Bundle bundle = new Bundle();
    bundle.putIntArray("nodes", nodes);
    bundle.putDoubleArray("latitudes", latitudes);
    bundle.putDoubleArray("longitudes", longitudes);
    Message responseMsg = Message.obtain(null, Messages.MSG_ROUTE);
    responseMsg.setData(bundle);
    try {
      replyTo.send(responseMsg);
    } catch (RemoteException ex) {
      Log.e(TAG, "handleMessage (MSG_ROUTE): RemoteException when trying to respond.");
    }
  }



}
