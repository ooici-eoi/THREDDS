/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.mesh;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.MeshDatatype;
import ucar.nc2.dt.MeshCoordSystem;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.IndexIterator;
import ucar.ma2.Array;

import java.util.*;
import java.io.IOException;

/**
 *
 * @author Kyle
 */
public class MeshDataset implements ucar.nc2.dt.MeshDataset, ucar.nc2.ft.FeatureDataset {
  private NetcdfDataset ds;
  private DateRange dateRangeMax = null;
  private LatLonRect llbbMax = null;
  private ArrayList<GeoMesh> grids = new ArrayList<GeoMesh>();
  private Map<String, Meshset> gridsetHash = new HashMap<String, Meshset>();

  static public MeshDataset open(String location) throws java.io.IOException {
    return open(location, NetcdfDataset.getDefaultEnhanceMode());
  }

  static public MeshDataset open(String location, Set<NetcdfDataset.Enhance> enhanceMode) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(null, location, enhanceMode, -1, null, null);
    return new MeshDataset(ds);
  }

  public MeshDataset(NetcdfDataset ds) throws IOException {
    this(ds, null);
  }

  public MeshDataset(NetcdfDataset ds, Formatter parseInfo) throws IOException {
    this.ds = ds;
    // ds.enhance(EnumSet.of(NetcdfDataset.Enhance.CoordSystems));
    ds.enhance(NetcdfDataset.getDefaultEnhanceMode());

    // look for meshGrids
    if (parseInfo != null) parseInfo.format("MeshDataset look for MeshGrids\n");
    List<Variable> vars = ds.getVariables();
    for (Variable var : vars) {
      VariableEnhanced varDS = (VariableEnhanced) var;
      constructCoordinateSystems(ds, varDS, parseInfo);
    }
  }

  private void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v, Formatter parseInfo) {

    if (v instanceof StructureDS) {
      StructureDS s = (StructureDS) v;
      List<Variable> members = s.getVariables();
      for (Variable nested : members) {
        // LOOK flatten here ??
        constructCoordinateSystems(ds, (VariableEnhanced) nested, parseInfo);
      }
    } else {
      // Make CoordSys
      MeshCoordSys gcs = null;
      List<CoordinateSystem> csys = v.getCoordinateSystems();
      for (CoordinateSystem cs : csys) {
        MeshCoordSys gcsTry = MeshCoordSys.makeMeshCoordSys(parseInfo, cs, v);
        if (gcsTry != null) {
          gcs = gcsTry;
          if (gcsTry.isProductSet()) break;
        }
      }

      if (gcs != null)
        addGeoGrid((VariableDS) v, gcs, parseInfo);

    }

  }

  public void calcBounds() throws java.io.IOException {
    // not needed
  }

  ////////////////////////////
  // for ucar.nc2.ft.FeatureDataset
  public FeatureType getFeatureType() {
    return FeatureType.MESH;
  }

  public DateRange getDateRange() {
    if (dateRangeMax == null) makeRanges();
    return dateRangeMax;
  }

  public DateRange makeRanges() {
    return null;
  }

  public String getImplementationName() {
    return ds.getConventionUsed();
  }

  public String getDetailInfo() {
    Formatter buff = new Formatter();
    getDetailInfo(buff);
    return buff.toString();
  }
  public void getDetailInfo(Formatter buff) {
    getInfo(buff);
    buff.format("\n\n----------------------------------------------------\n");
    NetcdfDatasetInfo info = null;
    try {
      info = new NetcdfDatasetInfo( ds.getLocation());
      buff.format("%s", info.getParseInfo());
    } catch (IOException e) {
      buff.format("NetcdfDatasetInfo failed");
    } finally {
      if (info != null) try { info.close(); } catch (IOException ee) {} // do nothing
    }
    buff.format("\n\n----------------------------------------------------\n");
    buff.format("%s", ds.toString());
    buff.format("\n\n----------------------------------------------------\n");
  }
  /**
   * Show Grids and coordinate systems.
   * @param buf put info here
   */
  private void getInfo(Formatter buf) {
    buf.format("\nGeoReferencing Coordinate Axes\n");
    buf.format("Name__________________________Units_______________Type______Description\n");
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (axis.getAxisType() == null) continue;
      axis.getInfo(buf);
      buf.format("\n");
    }
  }

  //////////////////////////////////////////////////
  //  FileCacheable
  protected FileCache fileCache;
  public void setFileCache(FileCache fileCache) {
    this.fileCache = fileCache;
  }
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else {
      try {
        if (ds != null) ds.close();
      } finally {
        ds = null;
      }
    }
  }
  public NetcdfFile getNetcdfFile() {
    return ds;
  }
  public VariableSimpleIF getDataVariable(String shortName) {
    return ds.findTopVariable(shortName);
  }
  public List<VariableSimpleIF> getDataVariables() {
    return null;
  }
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ds.findGlobalAttributeIgnoreCase(name);
  }
  public List<Attribute> getGlobalAttributes() {
    return ds.getGlobalAttributes();
  }
  public LatLonRect getBoundingBox() {
    if (llbbMax == null) makeRanges();
    return llbbMax;
  }
  public Date getStartDate() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getStart().getDate();
  }
  public Date getEndDate() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getEnd().getDate();
  }
  public String getLocation() {
    return ds.getLocation();
  }
  public String getLocationURI() {
    return ds.getLocation();
  }
  public String getName() {
    return ds.getLocation();
  }
  public String getTitle() {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    return (title == null) ? getName() : title;
  }
  public String getDescription() {
    String desc = ds.findAttValueIgnoreCase(null, "description", null);
    if (desc == null)
      desc = ds.findAttValueIgnoreCase(null, "history", null);
    return (desc == null) ? getName() : desc;
  }
  public boolean sync() throws IOException {
    return (ds != null) ? ds.sync() : false;
  }
    /**
   * @return the list of GeoMesh objects contained in this dataset.
   */
  public List<MeshDatatype> getGrids() {
    return new ArrayList<MeshDatatype>(grids);
  }

  private void addGeoGrid(VariableDS varDS, MeshCoordSys mcs, Formatter parseInfo) {
    Meshset gridset;
    if (null == (gridset = gridsetHash.get(mcs.getName()))) {
      gridset = new Meshset(mcs);
      gridsetHash.put(mcs.getName(), gridset);
      if (parseInfo != null) parseInfo.format(" -make new MeshCoordSys= %s\n",mcs.getName());
      mcs.makeVerticalTransform(this, parseInfo); // delayed until now
    }

    GeoMesh geogrid = new GeoMesh(this, varDS, gridset.mcc);
    grids.add(geogrid);
    gridset.add(geogrid);
  }

  /**
   * Return MeshDatatype objects grouped by MeshCoordSys. All GridDatatype in a Meshset
   * have the same MeshCoordSystem.
   *
   * @return List of type ucar.nc2.dt.MeshDataset.Meshset
   */
  public List<ucar.nc2.dt.MeshDataset.Meshset> getMeshsets() {
    return new ArrayList<ucar.nc2.dt.MeshDataset.Meshset>(gridsetHash.values());
  }
  
  public MeshDatatype findMeshDatatype(String name) {
    return findMeshByName(name);
  }


  /**
   * find the named GeoMesh.
   *
   * @param name find this GeoMesh by name
   * @return the named GeoMesh, or null if not found
   */
  public GeoMesh findMeshByName(String name) {
    for (GeoMesh ggi : grids) {
      if (name.equals(ggi.getName()))
        return ggi;
    }
    return null;
  }

  /**
   * @return the underlying NetcdfDataset
   */
  public NetcdfDataset getNetcdfDataset() {
    return ds;
  }

  /**
   * This is a set of GeoMeshs with the same GeoCoordSys.
   */
  public class Meshset implements ucar.nc2.dt.MeshDataset.Meshset {

    private MeshCoordSys mcc;
    private List<MeshDatatype> grids = new ArrayList<MeshDatatype>();

    private Meshset(MeshCoordSys gcc) {
      this.mcc = gcc;
    }

    private void add(GeoMesh grid) {
      grids.add(grid);
    }

    /**
     * Get list of GeoGrid objects
     */
    public List<MeshDatatype> getGrids() {
      return grids;
    }

    /**
     * all GridDatatype point to this GridCoordSystem
     */
    public MeshCoordSystem getGeoCoordSystem() {
      return mcc;
    }

    /**
     * all GeoGrids point to this GeoCoordSysImpl.
     *
     * @deprecated use getGeoCoordSystem() if possible.
     */
    public MeshCoordSys getGeoCoordSys() {
      return mcc;
    }

  }
}
