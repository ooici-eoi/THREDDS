// $Id: ForecastModelRun.java 70 2006-07-13 15:16:05Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dt.grid;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.*;

import ucar.ma2.InvalidRangeException;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.conv._Coordinate;

import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.NetcdfFile;
import ucar.nc2.IOServiceProvider;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.iosp.grib.GribServiceProvider;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

/**
 * This reads and writes XML files to summarize the inventory for a single ForecastModelRun.
 * The underlying dataset is a GridDataset.
 *
 * Tracks unique TimeCoords (aka "valid times" aka "forecast times" aka "offset hours"), and tracks the list of
 *   variables (aka grids) that use that TimeCoord.
 *
 * Tracks unique VertCoords; grids have a reference to one if they are 3D.
 *
 * <pre>
 * Data Structures
 *  List VertCoord
 *    double[] values
 *
 *  List TimeCoord
 *    double[] offsetHour
 *    List Grid
 *      VertCoord (optional)
 *      List Misssing
 * </pre>
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class ForecastModelRunInventory {
  public static final int OPEN_NORMAL = 1; // try to open XML, if fail, open dataset and write XML
  public static final int OPEN_FORCE_NEW = 2;  // only open dataset and write XML new
  public static final int OPEN_XML_ONLY = 3; // only open XML, if not exist, return

  private String name;
  private ArrayList times = new ArrayList(); // list of TimeCoord
  private ArrayList vaxes = new ArrayList(); // list of VertCoord
  private Date runDate; // date of the run
  private String runTime; // string representation of the date of the run
  private GridDataset gds; // underlying dataset - may be null if read from XML
  private LatLonRect bb;

  private boolean debugMissing = false;

  private ForecastModelRunInventory() { }

  private ForecastModelRunInventory(String ncfileLocation) throws IOException {
    this( ucar.nc2.dataset.grid.GridDataset.open( ncfileLocation), null);
  }

  private ForecastModelRunInventory(ucar.nc2.dt.GridDataset gds, Date runDate) {

    this.gds = gds;
    name = gds.getTitle();

    NetcdfFile ncfile = gds.getNetcdfFile();
    if (runDate == null) {
      runTime = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelRunDate, null);
      if (runTime == null)
        throw new IllegalArgumentException("File must have "+_Coordinate.ModelRunDate+" attribute ");
      this.runDate = DateUnit.getStandardOrISO(runTime);
      if (this.runDate == null)
        throw new IllegalArgumentException( _Coordinate.ModelRunDate+" must be ISO date string "+runTime);
    } else {
      this.runDate = runDate;
      DateFormatter df = new DateFormatter();
      this.runTime = df.toDateTimeStringISO(runDate);
    }
    getIosp();

    // add each variable
    List vars = gds.getGrids();
    for (int i = 0; i < vars.size(); i++) {
      GridDatatype gg = (GridDatatype) vars.get(i);
      GridCoordSystem gcs = gg.getGridCoordSystem();
      Grid grid = new Grid(gg.getName());
      addMissing((Variable) gg.getVariable(), gcs, grid);

      // LOOK: Note this assumes a dense coordinate system
      CoordinateAxis1D axis = gcs.getTimeAxis1D();
      if (axis != null) {
        TimeCoord tc = getTimeCoordinate(axis);
        tc.vars.add( grid);
        grid.parent = tc;
      }

      CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      if (vaxis != null) {
        grid.vc = getVertCoordinate(vaxis);
      }

      LatLonRect rect = gcs.getLatLonBoundingBox();
      if (null == bb)
        bb = rect;
      else if (!bb.equals(rect))
        bb.extend(rect);
    }
  }

  public void setName(String name) { this.name = name; }
  public String getName() { return name; }

  /** Get the date of the ForecastModelRun */
  public Date getRunDate() { return runDate; }

  /** Get string representation of the date of the ForecastModelRun */
  public String getRunDateString() { return runTime; }

  /**
   * Get a list of unique TimeCoords, which contain the list of variables that all use that TimeCoord.
   * @return list of TimeCoord
   */
  public List getTimeCoords() { return times; }

  /**
   * Get a list of unique VertCoords.
   * @return list of VertCoord
   */
  public List getVertCoords() { return vaxes; }

  public LatLonRect getBB() { return bb; }

  /**
   * Release and close the dataset, and allow CG.
   * @throws IOException
   */
  public void releaseDataset() throws IOException {
    if (gds == null)
      return;

    gds.close();
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      tc.axis = null;  // allow GC
    }

    for (int i = 0; i < vaxes.size(); i++) {
      VertCoord vc = (VertCoord) vaxes.get(i);
      vc.axis = null;  // allow GC
    }

  }

 public Grid findGrid( String name) {
   for (int i = 0; i < times.size(); i++) {
     TimeCoord tc = (TimeCoord) times.get(i);
     List grids = tc.getGrids();
     for (int j = 0; j < grids.size(); j++) {
       Grid g = (Grid) grids.get(j);
       if (g.name.equals(name))
       return g;
     }
   }
   return null;
 }

  //////////////////////////////////////////////////////////

  // Grib files are collections of 2D horizontal arrays.
  // LOOK: breaking encapsolation !!!
  private void getIosp() {
    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();
    NetcdfFile ncfile = ncd.getReferencedFile();
    while (ncfile instanceof NetcdfDataset) {
       ncd = (NetcdfDataset) ncfile;
       ncfile = ncd.getReferencedFile();
    }
    if (ncfile == null) return;
    IOServiceProvider iosp = ncfile.getIosp();
    if (iosp == null) return;
    if (!(iosp instanceof GribServiceProvider)) return;
    gribIosp = (GribServiceProvider) iosp;
  }
  private GribServiceProvider gribIosp;

  private void addMissing( Variable v, GridCoordSystem gcs, Grid grid) {
    if (gribIosp == null) return;
    if (gcs.getVerticalAxis() == null) return;
    int ntimes = (int) gcs.getTimeAxis().getSize();
    int nverts = (int) gcs.getVerticalAxis().getSize();
    int total = ntimes * nverts;

    ArrayList missing = new ArrayList();
    for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
      for (int vertIndex = 0; vertIndex < nverts; vertIndex++)
      try {
        if (gribIosp.isMissingXY(v, timeIndex, vertIndex))
          missing.add(new Missing(timeIndex, vertIndex));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }
    if (missing.size() > 0) {
      grid.missing = missing;
      if (debugMissing) System.out.println("Missing "+gds.getTitle()+" "+v.getName()+" # ="+missing.size()+"/"+total);
    } else
      if (debugMissing) System.out.println(" None missing for "+gds.getTitle()+" "+v.getName()+" total = "+total);
  }

  /////////////////////////////////////////////////////////////////////////

  private TimeCoord getTimeCoordinate(CoordinateAxis1D axis) {
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      if ((tc.axis != null) && (tc.axis == axis))
        return tc;
    }

    TimeCoord want = new TimeCoord(runDate, axis);
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      if ((tc.equalsData(want)))
        return tc;
    }

    // its a new one
    times.add(want);
    want.setId( Integer.toString(tc_seqno));
    tc_seqno++;
    return want;
  }

  private int tc_seqno = 0;

  /**
   *  Represents a list of valid times.
   *  Tracks a list of variables that all have the same list of valid times.
   */
  public static class TimeCoord implements FmrcCoordSys.TimeCoord {
    private CoordinateAxis1D axis; // is null when read from XML
    private ArrayList vars = new ArrayList();  // list of Grid
    private String id; // unique id
    private double[] offset; // hours since runTime

    TimeCoord() { }

    TimeCoord( int num, TimeCoord from) {
      this.id = Integer.toString(num);
      this.offset = from.offset;
    }

    TimeCoord(Date runDate, CoordinateAxis1D axis) {
      this.axis = axis;

      DateUnit unit = (DateUnit) DateUnit.factory(axis.getUnitsString());

      int n = (int) axis.getSize();
      offset = new double[n];
      for (int i = 0; i < axis.getSize(); i++) {
        Date d = unit.makeDate( axis.getCoordValue(i));
        offset[i] = getOffsetInHours( runDate, d);
      }
    }

    /** The list of Grid that use this TimeCoord */
    public List getGrids() { return vars; }

    /** A unique id for this TimeCoord */
    public String getId() { return id; }
    /** Set the unique id for this TimeCoord */
    public void setId(String id) { this.id = id; }

    public String getName() { return id.equals("0") ? "time" : "time"+id; }

    /** The list of valid times, in units of hours since the run time */
    public double[] getOffsetHours() { return offset; }
    public void setOffsetHours(double[] offset) { this.offset = offset; }

    /** Instances that have the same offsetHours are equal */
    public boolean equalsData(TimeCoord tother) {
      if (offset.length != tother.offset.length)
        return false;
      for (int i = 0; i < offset.length ; i++) {
        if (!ucar.nc2.util.Misc.closeEnough( offset[i], tother.offset[i]))
          return false;
      }
      return true;
    }

    int findIndex (double offsetHour) {
      for (int i = 0; i<offset.length; i++)
        if (offset[i] == offsetHour)
          return i;
      return -1;
    }

    /* Overrride hashcode to correspond to equals()
    public int hashCode() {
      if (hashcode != 0) return hashcode;

      int result = 17;
      for (int i = 0; i < offset.length ; i++) {
        long temp = Double.doubleToLongBits( offset[i]);
        result = 37*result + (int) (temp ^ (temp >>>32));
      }
      hashcode = result;
      return hashcode;
    }
    private int hashcode = 0; */
  }

  //////////////////////////////////////////////////////

  /**
   * A Grid variable has a name, timeCoord and optionally a Vertical Coordinate, and list of Missing.
   * The inventory is represented as:
   *   1) if 2D, the timeCoord represents the inventory
   *   2) if 3D, inventory = timeCoord * vertCoord - Missing
   */
  public static class Grid implements Comparable {
    String name; // , sname;
    TimeCoord parent = null;
    VertCoord vc = null; // optional
    ArrayList missing; // array of Missing

    Grid( String name) {
      this.name = name;
    }

    public int compareTo(Object o) {
      Grid other = (Grid) o;
      return name.compareTo(other.name);
    }

    public int countInventory() {
      return countTotal() - countMissing();
    }

    public int countTotal() {
      int ntimes = parent.getOffsetHours().length;
      return ntimes * getVertCoordLength();
    }

    public int countMissing() {
      return (missing == null) ? 0 : missing.size();
    }

    int getVertCoordLength() {
      return (vc == null) ? 1 : vc.getValues1().length;
    }

    public int countInventory(double hourOffset) {
      int timeIndex = parent.findIndex( hourOffset);
      if (timeIndex < 0)
        return 0;

            // otherwise, count the Missing with this time index
      if (missing == null)
        return getVertCoordLength();

      int count = 0;
      for (int i = 0; i < missing.size(); i++) {
        Missing m = (Missing) missing.get(i);
        if (m.timeIndex == timeIndex)
        count++;
      }
      return getVertCoordLength() - count;
    }

    /**
     * Get inventory as an array of vert coords, at a particular time coord = hourOffset
     * @param hourOffset : may or may not be in the list of time coords
     * @return array of vert coords. NaN = missing; -0.0 = surface.
     */
    public double[] getVertCoords(double hourOffset) {

      int timeIndex = parent.findIndex( hourOffset);
      if (timeIndex < 0)
        return new double[0]; // if not in list of time coordinates, then entire inventory is missing

      if (vc == null) {
        double[] result = new double[1]; // if 2D return -0.0
        result[0] = -0.0;
        return result;
      }

      double[] result = (double[]) vc.getValues1().clone();
      if (null != missing) {
        for (int i = 0; i < missing.size(); i++) {
          Missing m = (Missing) missing.get(i);
          if (m.timeIndex == timeIndex)
            result[m.vertIndex] = Double.NaN;
        }
      }
      return result;
    }
  }

  public static class Missing {
    int timeIndex, vertIndex;
    Missing(int timeIndex, int vertIndex) {
      this.timeIndex = timeIndex;
      this.vertIndex = vertIndex;
    }
  }

  //////////////////////////////////////////////////////

  private VertCoord getVertCoordinate(String vert_id) {
    if (vert_id == null)
      return null;
    for (int i = 0; i < vaxes.size(); i++) {
      VertCoord vc = (VertCoord) vaxes.get(i);
      if ((vc.id.equals(vert_id)))
        return vc;
    }
    return null;
  }

  private VertCoord getVertCoordinate(CoordinateAxis1D axis) {
    for (int i = 0; i < vaxes.size(); i++) {
      VertCoord vc = (VertCoord) vaxes.get(i);
      if ((vc.axis != null) && (vc.axis == axis)) return vc;
    }

    VertCoord want = new VertCoord(axis);
    for (int i = 0; i < vaxes.size(); i++) {
      VertCoord vc = (VertCoord) vaxes.get(i);
      if ((vc.equalsData(want))) return vc;
    }

    // its a new one
    vaxes.add(want);
    want.setId( Integer.toString(vc_seqno));
    vc_seqno++;
    return want;
  }

  private int vc_seqno = 0;


  /**
   *  Represents a vertical coordinate.
   *  Tracks a list of variables that all have the same list of valid times.
   */
  public static class VertCoord implements FmrcCoordSys.VertCoord, Comparable {
    CoordinateAxis1D axis; // is null when read from XML
    private String name, units;
    private String id; // unique id
    double[] values1, values2;

    VertCoord() { }

    VertCoord(CoordinateAxis1D axis) {
      this.axis = axis;
      this.name = axis.getName();
      this.units = axis.getUnitsString();

      int n = (int) axis.getSize();
      if (axis.isLayer()) {
        values1 = axis.getBound1();
        values2 = axis.getBound2();
      } else {
        values1 = new double[n];
        for (int i = 0; i < axis.getSize(); i++)
          values1[i] = axis.getCoordValue(i);
      }
    }

    // copy constructor
    VertCoord(VertCoord vc) {
      this.name = vc.getName();
      this.units = vc.getUnits();
      this.id = vc.getId();
      this.values1 = (double[]) vc.getValues1().clone();
      this.values2 = (vc.getValues2() == null) ? null : (double[]) vc.getValues2().clone();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    public double[] getValues1() { return values1; }
    public void setValues1(double[] values) { this.values1 = values; }

    public double[] getValues2() { return values2; }
    public void setValues2(double[] values) { this.values2 = values; }

    public int getSize() { return values1.length; }

    public boolean equalsData(VertCoord other) {
      if (values1.length != other.values1.length)
        return false;

      for (int i = 0; i < values1.length ; i++) {
        if ( !ucar.nc2.util.Misc.closeEnough(values1[i], other.values1[i]))
          return false;
      }

      if ((values2 == null) && (other.values2 == null))
        return true;

      if ((values2 == null) || (other.values2 == null))
        return false;

      if (values2.length != other.values2.length)
        return false;
      for (int i = 0; i < values2.length ; i++) {
        if ( !ucar.nc2.util.Misc.closeEnough(values2[i], other.values2[i]))
          return false;
      }

      return true;
    }

    public int compareTo(Object o) {
      VertCoord other = (VertCoord) o;
      return name.compareTo( other.name);
    }
  }

   static public double getOffsetInHours(Date origin, Date date) {
      double secs = date.getTime() / 1000;
      double origin_secs = origin.getTime() / 1000;
      double diff = secs - origin_secs;

      return diff / 3600.0;
    }

  //////////////////////////////////////////////////////////////

  /**
   * Write the XML representation to a local file.
   * @param filename wite to this local file
   * @throws IOException
   */
  public void writeXML(String filename) throws IOException {
    OutputStream out = new BufferedOutputStream( new FileOutputStream( filename));
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(writeDocument(), out);
    out.close();
  }

  /**
   * Write the XML representaion to an OutputStream.
   * @param out write to this OutputStream
   * @throws IOException
   */
  public void writeXML(OutputStream out) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(writeDocument(), out);
  }

  /**
   * Write the XML representation to a String.
   */
  public String writeXML( )  {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    return fmt.outputString( writeDocument());
  }

  /** Create the XML representation */
  public Document writeDocument() {
    Element rootElem = new Element("forecastModelRun");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("name", getName());
    rootElem.setAttribute("runTime", runTime);

        // list all the vertical coords
    Collections.sort( vaxes);
    for (int i = 0; i < vaxes.size(); i++) {
      VertCoord vc = (VertCoord) vaxes.get(i);

      Element vcElem = new Element("vertCoord");
      rootElem.addContent(vcElem);
      vcElem.setAttribute("id", vc.id);
      vcElem.setAttribute("name", vc.name);
      if (vc.units != null)
        vcElem.setAttribute("units", vc.units);

      StringBuffer sbuff = new StringBuffer();
      for (int j = 0; j < vc.values1.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append( Double.toString(vc.values1[j]));
        if (vc.values2 != null) {
          sbuff.append(",");
          sbuff.append( Double.toString(vc.values2[j]));
        }
      }
      vcElem.addContent(sbuff.toString());
    }

    // list all the offset hours
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);

      Element offsetElem = new Element("offsetHours");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("id", tc.id);

      StringBuffer sbuff = new StringBuffer();
      for (int j = 0; j < tc.offset.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append( Double.toString(tc.offset[j]));
      }
      offsetElem.addContent(sbuff.toString());

      Collections.sort( tc.vars);
      for (int j=0; j<tc.vars.size(); j++) {
        Grid grid = (Grid) tc.vars.get(j);
        Element varElem = new Element("variable");
        offsetElem.addContent(varElem);
        varElem.setAttribute("name", grid.name);
        if (grid.vc != null)
          varElem.setAttribute("vert_id", grid.vc.id);

        if ((grid.missing != null) && (grid.missing.size() > 0)) {
          Element missingElem = new Element("missing");
          varElem.addContent(missingElem);
          sbuff.setLength(0);
          for (int k = 0; k < grid.missing.size(); k++) {
            Missing m = (Missing) grid.missing.get(k);
            if (k > 0) sbuff.append(" ");
            sbuff.append(m.timeIndex);
            sbuff.append(",");
            sbuff.append(m.vertIndex);
          }
          missingElem.addContent(sbuff.toString());
        }
      }

      // add lat/lon bounding box
      if (bb != null) {
        Element bbElem = new Element("horizBB");
        rootElem.addContent(bbElem);
        LatLonPoint llpt = bb.getLowerLeftPoint();
        LatLonPoint urpt = bb.getUpperRightPoint();
        bbElem.setAttribute("west", ucar.unidata.util.Format.dfrac( llpt.getLongitude(), 3));
        bbElem.setAttribute("east", ucar.unidata.util.Format.dfrac( urpt.getLongitude(), 3));
        bbElem.setAttribute("south", ucar.unidata.util.Format.dfrac( llpt.getLatitude(), 3));
        bbElem.setAttribute("north", ucar.unidata.util.Format.dfrac( urpt.getLatitude(), 3));
      }
    }

    return doc;
  }

  /**
   * Construct a ForecastModelRun from its XML representation
   * @param xmlLocation location of xml - assumed to be a local file.
   * @return ForecastModelRun
   * @throws IOException
   */
  public static ForecastModelRunInventory readXML(String xmlLocation) throws IOException {
    if (debug) System.out.println(" read from XML "+xmlLocation);

    InputStream is = new BufferedInputStream( new FileInputStream( xmlLocation));
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element rootElem = doc.getRootElement();
    ForecastModelRunInventory fmr = new ForecastModelRunInventory();
    fmr.runTime = rootElem.getAttributeValue("runTime");

    DateFormatter formatter = new  DateFormatter();
    fmr.runDate = formatter.getISODate(fmr.runTime);

    java.util.List vList = rootElem.getChildren("vertCoord");
    for (int i=0; i< vList.size(); i++) {
      Element vertElem = (Element) vList.get(i);
      VertCoord vc = new VertCoord();
      fmr.vaxes.add( vc);
      vc.id = vertElem.getAttributeValue("id");
      vc.name = vertElem.getAttributeValue("name");
      vc.units = vertElem.getAttributeValue("units");

      // parse the values
      String values = vertElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      vc.values1 = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        int pos = toke.indexOf(',');
        if (pos < 0)
          vc.values1[count] = Double.parseDouble( toke);
        else {
          if (vc.values2 == null)
            vc.values2 = new double[n];
          String val1 = toke.substring(0,pos);
          String val2 = toke.substring(pos+1);
          vc.values1[count] = Double.parseDouble( val1);
          vc.values2[count] = Double.parseDouble( val2);
        }
        count++;
      }
    }

    java.util.List tList = rootElem.getChildren("offsetHours");
    for (int i=0; i< tList.size(); i++) {
      Element timeElem = (Element) tList.get(i);
      TimeCoord tc = new TimeCoord();
      fmr.times.add( tc);
      tc.id = timeElem.getAttributeValue("id");

      // parse the values
      String values = timeElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      tc.offset = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        tc.offset[count++] = Double.parseDouble( stoke.nextToken());
      }

      //get the variable names
      java.util.List varList = timeElem.getChildren("variable");
      for (int j=0; j< varList.size(); j++) {
        Element vElem = (Element) varList.get(j);
        Grid grid = new Grid( vElem.getAttributeValue("name"));
        grid.vc = fmr.getVertCoordinate( vElem.getAttributeValue("vert_id"));
        tc.vars.add( grid);
        grid.parent = tc;

        java.util.List mList = vElem.getChildren("missing");
        for (int k=0; k< mList.size(); k++) {
          Element mElem = (Element) mList.get(k);
          grid.missing = new ArrayList();

          // parse the values
          values = mElem.getText();
          stoke = new StringTokenizer(values, " ,");
          while (stoke.hasMoreTokens()) {
            int timeIdx = Integer.parseInt(stoke.nextToken());
            int vertIdx = Integer.parseInt(stoke.nextToken());
            grid.missing.add( new Missing(timeIdx, vertIdx)) ;
          }
        }
      }
    }

          // add lat/lon bounding box
    Element bbElem = rootElem.getChild("horizBB");
      if (bbElem != null) {
        double west = Double.parseDouble( bbElem.getAttributeValue("west"));
        double east = Double.parseDouble( bbElem.getAttributeValue("east"));
        double north = Double.parseDouble( bbElem.getAttributeValue("north"));
        double south = Double.parseDouble( bbElem.getAttributeValue("south"));
        fmr.bb = new LatLonRect( new LatLonPointImpl(south, west), new LatLonPointImpl(north, east));
      }

    return fmr;
  }


  /**
   * Open a GridDataset and construct a ForecastModelRun.
   * The information is serialized into am XML file at ncfileLocation.fmrInv.xml, and used if it exists.
   *
   * @param cache use this cache (may be null)
   * @param ncfileLocation  location of the grid dataset.
   * @param mode one of OPEN_NORMAL, OPEN_FORCE_NEW, OPEN_XML_ONLY constants
   * @param isFile if its a file: new File( ncfileLocation) makes sense, so we can check if its changed
   * @return ForecastModelRun
   * @throws IOException
   */
  public static ForecastModelRunInventory open(ucar.nc2.util.DiskCache2 cache, String ncfileLocation, int mode, boolean isFile) throws IOException {
    boolean force = (mode == OPEN_FORCE_NEW);
    boolean xml_only = (mode == OPEN_XML_ONLY);

    String summaryFileLocation = ncfileLocation + ".fmrInv.xml";
    File summaryFile;
    if (null != cache) {
      summaryFile = cache.getCacheFile(summaryFileLocation);
      summaryFileLocation = summaryFile.getPath();
    } else {
      summaryFile = new File( summaryFileLocation);
    }

    if (!force) {
      if (summaryFile.exists()) {

        if (isFile) { // see if its changed
          File ncdFile = new File( ncfileLocation);
          if (!ncdFile.exists())
            throw new IllegalArgumentException("File must exist = "+ncfileLocation);

          if (xml_only || (summaryFile.lastModified() >= ncdFile.lastModified())) {
            return readXML(summaryFileLocation);
          }
        } else {  // just use it

          return readXML(summaryFileLocation);
        }
      }

      if (xml_only) return null;
    }

    // otherwise, make it
    if (debug) System.out.println(" read from dataset "+ncfileLocation+" write to XML "+summaryFileLocation);
    ForecastModelRunInventory fmr = new ForecastModelRunInventory(ncfileLocation);
    fmr.writeXML(summaryFileLocation);
    fmr.releaseDataset();

    if (showXML)
      thredds.util.IO.copyFile(summaryFileLocation, System.out);

    return fmr;
  }

  public static ForecastModelRunInventory open(ucar.nc2.dt.GridDataset gds, Date runDate) {
    return new ForecastModelRunInventory(gds, runDate);
  }


  private static boolean debug = true, showXML = false;
  public static void main(String args[]) throws Exception {
    //String def = "C:/data/grib/nam/c20s/NAM_CONUS_20km_surface_20060316_1800.grib1";
    // String def = "C:/data/radarMosaic/RADAR_10km_mosaic_20060807_2220.grib1";
    String def = "R:/testdata/motherlode/grid/NAM_CONUS_80km_20060728_1200.grib1";
    String datasetName =  (args.length < 1) ? def : args[0];
    // ucar.nc2.util.DiskCache2 cache = new ucar.nc2.util.DiskCache2("C:/data/grib", false, -1, -1);
    // cache.setCachePathPolicy(DiskCache2.CACHEPATH_POLICY_NESTED_TRUNCATE, "RUC");
    open(null, datasetName, OPEN_FORCE_NEW, true);
  }
}
