/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.ugrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.ugrid.geom.LatLonPoint2D;
import ucar.nc2.dt.ugrid.geom.LatLonPolygon2D;
import ucar.nc2.dt.ugrid.geom.LatLonRectangle2D;
import ucar.nc2.dt.ugrid.rtree.RTree;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 *
 * @author Kyle
 */
public class Mesh {

  private String name;
  private ArrayList<Variable> variables = new ArrayList<Variable>();
  private RTree rtree = new RTree();
  private ArrayList<Cell> cells = new ArrayList<Cell>();

  static public Mesh open(NetcdfDataset ds, VariableEnhanced v) {
    return new Mesh(ds,v);
  }

  Mesh(NetcdfDataset ds, VariableEnhanced v) {
    name = v.getName();
    if (v.findAttributeIgnoreCase("locations") != null) {
      for (String prefix : v.findAttributeIgnoreCase("locations").getStringValue().split(" ")) {
        constructConnectivity(ds, v, prefix);
        addVariables(ds);
      }
    } else {
      System.out.println("No 'locations' attribute on the Mesh.");
    }
  }

  private void addCoordinateSystems() {
    
  }

  private void addVariables(NetcdfDataset ds) {
    for (Variable v : ds.getVariables()) {
      if (v.findAttributeIgnoreCase("mesh") != null) {
        if (v.findAttributeIgnoreCase("mesh").getStringValue().toLowerCase().contains(name.toLowerCase())) {
          if (!variables.contains(v)) {
            variables.add(v);
          }
        }
      }
    }
  }

  private void constructConnectivity(NetcdfDataset ds, VariableEnhanced v, String prefix) {
    // This Mesh variable has no Coordinate system, because it may need more
    // than 1!  Should we change so variables can have more than 1?  I don't
    // think so, since it isn't this variable with the axis, it is others.
    // This is just a reference we can parse correctly.
    
    // prefix is "node" or "face" or "edge"
    Attribute att = v.findAttributeIgnoreCase(prefix + "_coordinates");
    Attribute conn = v.findAttributeIgnoreCase(prefix + "_connectivity");
    if (att != null && conn != null && !att.getStringValue().equals("") && !conn.getStringValue().equals("")) {
      // We may need to add these additional CoordinateSystems
      


      // We have located a coordinate system and a reference to the connectivity
      // array for that system.  Set the reference (prefix) so other variables
      // can be mapped to this coordinate system.
      constructCells(ds, att, ds.findVariable(conn.getStringValue()));
    } else {
      System.out.println("Could not find coordinate and connectivity " +
                         "information for this Mesh.");
    }
  }

  private void constructCells(NetcdfDataset ds, Attribute att, Variable connectivity) {
    for (CoordinateSystem coord : ds.getCoordinateSystems()) {
      if (coord.getName().toLowerCase().contains(att.getStringValue().toLowerCase())) {
        try {
          // Found the coordinate system
          double[] llats = (double[]) coord.getLatAxis().read().get1DJavaArray(double.class);
          double[] llons = (double[]) coord.getLonAxis().read().get1DJavaArray(double.class);
          Attribute cell_type = connectivity.findAttributeIgnoreCase("cell_type");
          if (cell_type != null && !cell_type.getStringValue().equals("")) {
            // for future use of the cell_type attribute

            int start_index;
            if (connectivity.findAttributeIgnoreCase("index_origin") != null) {
              start_index = connectivity.findAttributeIgnoreCase("index_origin").getNumericValue().intValue();
            } else {
              start_index = 0;
              System.out.println("Connectivity array " + connectivity.getName() + " assumed to start at index 0.");
            }
            if (connectivity.getDimensions().get(0).getLength() > connectivity.getDimensions().get(1).getLength()) {
              //  [N0] [1,2,3]
              //  [N1] [4,5,6]
              //  [N2] [2,5,6]
              //  [N3] [1,4,3]
              processConnectivityArray(connectivity, start_index, llats, llons);
            } else {
              //       N0  N1  N2  N3
              //  --------------------
              //       ̪   ̪   ̪   ̪
              //  [0]  1   4   2   1
              //  [1]  2   5   5   4
              //  [2]  3   6   6   3
              //       ̺   ̺   ̺   ̺
              processConnectivityList(connectivity, start_index, llats, llons);
            }
          } else {
            System.out.println("cell_type was not specified on the "
                       + "connectivity_array variable: " + cell_type.getName());
          }
        } catch (IOException e) {
          System.out.println(e);
        }
      }
    }
    //att.getStringValue()
  }

  private void processConnectivityList(Variable connectivity, int start_index, double[] llats, double[] llons) {
    
    LatLonPolygon2D poly;
    LatLonPoint2D point;
    Node node;
    Cell cell;
    int index;
    ArrayList<Node> nodes = new ArrayList<Node>();
    
    try {
      int[][] conn_data = (int[][])connectivity.read().copyToNDJavaArray();

      for (int i = 0; i < connectivity.getDimensions().get(1).getLength(); i++) {
        cell = new Cell();
        nodes.clear();
        poly = new LatLonPolygon2D.Double();
        for (int j = 0; j < connectivity.getDimensions().get(0).getLength(); j++) {
          index = conn_data[j][i] - start_index;
          point = new LatLonPoint2D.Double(llats[index], llons[index]);
          node = new Node();
          node.setDataIndex(index);
          node.setGeoPoint(point);
          nodes.add(node);
          poly.lineTo(point);
        }
        int addIndex = cells.size();
        cell.setPolygon(poly);
        cell.setNodes(nodes);
        cells.add(addIndex, cell);
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  private void processConnectivityArray(Variable connectivity, int start_index, double[] llats, double[] llons) {
    LatLonPolygon2D poly;
    LatLonPoint2D point;
    Node node;
    Cell cell;
    int index;
    ArrayList<Node> nodes = new ArrayList<Node>();
    
    try {
      int[][] conn_data = (int[][])connectivity.read().copyToNDJavaArray();

      for (int i = 0; i < connectivity.getDimensions().get(0).getLength(); i++) {
        cell = new Cell();
        nodes.clear();
        poly = new LatLonPolygon2D.Double();
        for (int j = 0; j < connectivity.getDimensions().get(1).getLength(); j++) {
          index = conn_data[i][j] - start_index;
          point = new LatLonPoint2D.Double(llats[index], llons[index]);
          node = new Node();
          node.setDataIndex(index);
          node.setGeoPoint(point);
          nodes.add(node);
          poly.lineTo(point);
        }
        int addIndex = cells.size();
        cell.setPolygon(poly);
        cell.setNodes(nodes);
        cells.add(addIndex, cell);
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  public void buildRTree() {
    for (int i = 0 ; i < cells.size() ; i++) {
      rtree.add(cells.get(i).getPolygon(), i);
    }
  }

  public String getName() {
    return name;
  }

  public ArrayList<Variable> getVariables() {
    return variables;
  }

  public int getSize() {
    return cells.size();
  }

  public int getTreeSize() {
    return rtree.size();
  }

  public int getNodeSize() {
    int i = 0;
    for (Cell c : cells) {
      i += c.getNodes().size();
    }
    return i;
  }
  public int getEdgeSize() {
    int i = 0;
    for (Cell c : cells) {
      i += c.getEdges().size();
    }
    return i;
  }
  public int getFaceSize() {
    int i = 0;
    for (Cell c : cells) {
      i += c.getFaces().size();
    }
    return i;
  }

  public LatLonRect getLatLonBoundingBox() {
    LatLonRectangle2D bounds = rtree.getBounds();
    return new LatLonRect((LatLonPoint)new LatLonPointImpl(bounds.getLatMin(), bounds.getLonMin()),(LatLonPoint)new LatLonPointImpl(bounds.getLatMax(), bounds.getLonMax()));
  }

  public Cell getCellFromLatLon(double lat, double lon) {
    LatLonPoint2D p = new LatLonPoint2D.Double(lat,lon);
    return cells.get(rtree.nearest(p));
  }

  public double extractPointData(Variable v, LatLonPoint point) throws IOException {

    // Translate to LatLonPoint2D
    final LatLonPoint2D p = new LatLonPoint2D.Double(point.getLatitude(), point.getLongitude());
    double z = -1;
    // Find the closest R-Tree node
    int q = rtree.nearest(p);
    // Get the cell assoicated with that R-Tree index
    Cell c = cells.get(q);

    // This should be stored in GeoLocation from parsing into
    // a coordinate system!
    String loc = v.findAttributeIgnoreCase("location").getStringValue();
    ArrayList<? extends Entity> e;

    if (loc.equals("node")) {
      e = c.getNodes();
    } else if (loc.equals("face")) {
      e = c.getFaces();
    } else if (loc.equals("edge")) {
      e = c.getEdges();
    } else {
      e = null;
    }

    if ((e != null) && (e.size() > 0)) {

      // Sort the collection of Entities by distance from the query point.
      // This should offer different ways to calculate the distance of
      // the closest point.

      // "e" is all of the Entities in the Cell that are on the variable's
      // location (Node OR Edge OR Face).

      Collections.sort(e, new Comparator() {
        public int compare(Object o1, Object o2) {
          Entity e1 = (Entity) o1;
          Entity e2 = (Entity) o2;
          if (e1.getGeoPoint().distance(p) == e2.getGeoPoint().distance(p)) {
            return 0;
          } else if (e1.getGeoPoint().distance(p) > e2.getGeoPoint().distance(p)) {
            return 1;
          } else {
            return -1;
          }
        }
      });

      // Get the closest Entities DataIndex into the NetCDF file.
      int in = e.get(0).getDataIndex();
      try {

        // Need to compute actual ranges here, not assume it is (time,z,entity)
        List<Range> r = new ArrayList<Range>();
        // Time (first)
        r.add(new Range(0,0));
        // Sigma (first)
        r.add(new Range(0,0));
        // Data (DataIndex from Cell)
        r.add(new Range(in,in));

        float[] ret1D = (float[]) v.read(r).copyTo1DJavaArray();
        z = ret1D[0];
      } catch (InvalidRangeException ex) {
        System.out.println(ex);
      }
    }
    return z;
  }
}
