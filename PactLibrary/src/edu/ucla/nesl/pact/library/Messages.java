package edu.ucla.nesl.pact.library;

/**
 * TODO: Give a one line description.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class Messages {

  public final static int MSG_GRAPH_HOPPER_CONNECTED = 0;
  public final static int MSG_GRAPH_HOPPER_DISCONNECTED = 1;

  public final static int MSG_ASK_FOR_ROUTE = 2;
  public final static int MSG_ROUTE = 3;

  public final static int MSG_QUERY_ROUTE_NODE_NEAR_LOCATOIN = 4;
  public final static int MSG_NODES = 5;


  public final static int MSG_QUERY_NEARBY_PLACES_FOR_LOCATION = 5;
  public final static int MSG_PLACES = 3;

  public static final String RADIUS_METERS_KEY = "radius_meters";
  public static final String LAT_KEY = "lat";
  public static final String LON_KEY = "lon";
  public static final String PLACE_TYPES_KEY = "place_types";
}