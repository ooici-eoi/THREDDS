/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.mesh;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.MeshDatatype;
import ucar.nc2.util.NamedObject;
import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;

import java.util.*;
import java.io.IOException;

/**
 *
 * @author Kyle
 */
public class GeoMesh implements NamedObject, ucar.nc2.dt.MeshDatatype {
  private MeshDataset dataset;
  private MeshCoordSys mcs;
  private VariableDS vs;
  private int nDimOrgIndex = -1, zDimOrgIndex = -1, tDimOrgIndex = -1, eDimOrgIndex = -1, rtDimOrgIndex = -1;
  private int nDimNewIndex = -1, zDimNewIndex = -1, tDimNewIndex = -1, eDimNewIndex = -1, rtDimNewIndex = -1;
  private List<Dimension> mydims;


  /**
   * Constructor.
   *
   * @param dataset belongs to this dataset
   * @param dsvar   wraps this Variable
   * @param gcs     has this grid coordinate system
   */
  public GeoMesh(MeshDataset dataset, VariableDS dsvar, MeshCoordSys mcs) {
    this.dataset = dataset;
    this.vs = dsvar;
    this.mcs = mcs;

    CoordinateAxis naxis = mcs.getNodeAxis();
    if (naxis instanceof CoordinateAxis) {
      nDimOrgIndex = findDimension(mcs.getNodeAxis().getDimension(0));
    }

    if (mcs.getVerticalAxis() != null) zDimOrgIndex = findDimension(mcs.getVerticalAxis().getDimension(0));
    if (mcs.getTimeAxis() != null) {
      if (mcs.getTimeAxis1D() != null)
        tDimOrgIndex = findDimension(mcs.getTimeAxis1D().getDimension(0));
      else
        tDimOrgIndex = findDimension(mcs.getTimeAxis().getDimension(1));
    }
    if (mcs.getEnsembleAxis() != null) eDimOrgIndex = findDimension(mcs.getEnsembleAxis().getDimension(0));
    if (mcs.getRunTimeAxis() != null) rtDimOrgIndex = findDimension(mcs.getRunTimeAxis().getDimension(0));

    // construct canonical dimension list
    int count = 0;
    this.mydims = new ArrayList<Dimension>();
    if ((rtDimOrgIndex >= 0) && (rtDimOrgIndex != tDimOrgIndex)) {
      mydims.add(dsvar.getDimension(rtDimOrgIndex));
      rtDimNewIndex = count++;
    }
    if (eDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(eDimOrgIndex));
      eDimNewIndex = count++;
    }
    if (tDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(tDimOrgIndex));
      tDimNewIndex = count++;
    }
    if (zDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(zDimOrgIndex));
      zDimNewIndex = count++;
    }
    if (nDimOrgIndex >= 0) {
      mydims.add(dsvar.getDimension(nDimOrgIndex));
      nDimOrgIndex = count++;
    }
  }

  private int findDimension(Dimension want) {
    java.util.List dims = vs.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = (Dimension) dims.get(i);
      if (d.equals(want))
        return i;
    }
    return -1;
  }

  public VariableDS getVariable() {
    return vs;
  }
  /**
   * get the standardized description
   */
  public String getDescription() {
    return vs.getDescription();
  }
  /**
   * get the name of the geoGrid.
   */
  public String getName() {
    return vs.getName();
  }
  /**
   * get the escaped name of the geoGrid.
   */
  public String getNameEscaped() {
    return vs.getNameEscaped();
  }

  /**
   * nicely formatted information
   */
  public String getInfo() {
    StringBuilder buf = new StringBuilder(200);
    buf.setLength(0);
    buf.append(getName());
    Format.tab(buf, 30, true);
    buf.append(getUnitsString());
    Format.tab(buf, 60, true);
    buf.append(hasMissingData());
    Format.tab(buf, 66, true);
    buf.append(getDescription());
    return buf.toString();
  }
  /**
   * get the unit as a string
   */
  public String getUnitsString() {
    String units = vs.getUnitsString();
    return (units == null) ? "" : units;
  }
  /**
   * true if there may be missing data, see VariableDS.hasMissing()
   */
  public boolean hasMissingData() {
    return vs.hasMissing();
  }

  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN, according to isMissingData(val).
   *
   * @param values input array
   * @return input array, with missing values converted to NaNs.
   */
  public float[] setMissingToNaN(float[] values) {
    if (!vs.hasMissing()) return values;
    final int length = values.length;
    for (int i = 0; i < length; i++) {
      double value = values[i];
      if (vs.isMissing(value))
        values[i] = Float.NaN;
    }
    return values;
  }

  /**
   * Get the minimum and the maximum data value of the previously read Array,
   * skipping missing values as defined by isMissingData(double val).
   *
   * @param a Array to get min/max values
   * @return both min and max value.
   */
  public MAMath.MinMax getMinMaxSkipMissingData(Array a) {
    if (!hasMissingData())
      return MAMath.getMinMax(a);

    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (isMissingData(val))
        continue;
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MAMath.MinMax(min, max);
  }

  /**
   * if val is missing data, see VariableDS.isMissingData()
   */
  public boolean isMissingData(double val) {
    return vs.isMissing(val);
  }

  /**
   * Returns an ArrayList containing the dimensions used by this geoGrid.
   * The dimension are put into canonical order: (rt, e, t, z, y, x). Note that the z and t dimensions are optional.
   * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
   * gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.
   *
   * @return List with objects of type Dimension, in canonical order.
   */
  public java.util.List<Dimension> getDimensions() {
    return new ArrayList<Dimension>(mydims);
  }

  /**
   * get the ith dimension
   *
   * @param i : which dimension
   * @return ith Dimension
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= mydims.size())) return null;
    return mydims.get(i);
  }

  /**
   * get the ensemble Dimension, if it exists
   */
  public Dimension getEnsembleDimension() {
    return eDimNewIndex < 0 ? null : getDimension(eDimNewIndex);
  }

  /**
   * get the ensemble Dimension index in the geogrid (canonical order)
   */
  public int getEnsembleDimensionIndex() {
    return eDimNewIndex;
  }
  
  /**
   * get the z Dimension, if it exists
   */
  public Dimension getZDimension() {
    return zDimNewIndex < 0 ? null : getDimension(zDimNewIndex);
  }

  /**
   * get the z Dimension index in the geogrid (canonical order), or -1 if none
   */
  public int getZDimensionIndex() {
    return zDimNewIndex;
  }

  /**
   * get the run time Dimension, if it exists
   */
  public Dimension getRunTimeDimension() {
    return rtDimNewIndex < 0 ? null : getDimension(rtDimNewIndex);
  }

  /**
   * get the runtime Dimension index in the geogrid (canonical order)
   */
  public int getRunTimeDimensionIndex() {
    return rtDimNewIndex;
  }

  /**
   * get the time Dimension, if it exists
   */
  public Dimension getTimeDimension() {
    return tDimNewIndex < 0 ? null : getDimension(tDimNewIndex);
  }

  /**
   * get the time Dimension index in the geogrid (canonical order), or -1 if none
   */
  public int getTimeDimensionIndex() {
    return tDimNewIndex;
  }

  /**
   * Convenience function; lookup Attribute value by name. Must be String valued
   *
   * @param attName      name of the attribute
   * @param defaultValue if not found, use this as the default
   * @return Attribute string value, or default if not found.
   */
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return dataset.getNetcdfDataset().findAttValueIgnoreCase((Variable) vs, attName, defaultValue);
  }

  /**
   * Convenience function; lookup Attribute by name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    return vs.findAttributeIgnoreCase(name);
  }

  public List<Attribute> getAttributes() {
    return vs.getAttributes();
  }

  public int[] getShape() {
    int[] shape = new int[mydims.size()];
    for (int i = 0; i < mydims.size(); i++) {
      Dimension d = mydims.get(i);
      shape[i] = d.getLength();
    }
    return shape;
  }

  /**
   * get the rank
   */
  public int getRank() {
    return vs.getRank();
  }

  /**
   * get the data type
   */
  public DataType getDataType() {
    return vs.getDataType();
  }

  public int compareTo(MeshDatatype g) {
    return getName().compareTo(g.getName());
  }

}
