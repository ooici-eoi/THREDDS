// $Id:WRFConvention.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.util.StringUtil;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.io.IOException;
import java.util.*;

/**
 * WRF netcdf output files. 
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class WRFConvention extends CoordSysBuilder {

  static private java.text.SimpleDateFormat dateFormat;
  static {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /** return true if we think this is a WRF file. */
  public static boolean isMine( NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("MAP_PROJ")) &&
           (null != ncfile.findDimension("south_north"));
  }

  private double originX = 0.0, originY = 0.0;
  private ProjectionCT projCT = null;

  /** create a NetcdfDataset out of this NetcdfFile, adding coordinates etc. */
  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    this.conventionName = "WRF";

    // kludge in fixing the units
    List vlist = ds.getVariables();
    for (int i=0; i<vlist.size(); i++) {
      Variable v = (Variable) vlist.get(i);
      Attribute att = v.findAttributeIgnoreCase( "units");
      if (att != null) {
        String units = att.getStringValue();
        v.addAttribute( new Attribute( "units", normalize( units))); // removes the old
      }
    }

        // make projection transform
    Attribute att = ds.findGlobalAttribute("MAP_PROJ");
    int projType = att.getNumericValue().intValue();
    double lat1 = findAttributeDouble( ds, "TRUELAT1");
    double lat2 = findAttributeDouble( ds, "TRUELAT2");
    double centralLat = findAttributeDouble( ds, "CEN_LAT");
    double centralLon = findAttributeDouble( ds, "CEN_LON");

    double standardLon = findAttributeDouble( ds, "STAND_LON");
    double moadLat = findAttributeDouble( ds, "MOAD_CEN_LAT");

    double lon0 = (Double.isNaN(standardLon)) ? centralLon : standardLon;
    double lat0 = (Double.isNaN(moadLat)) ? centralLat : moadLat;

    ProjectionImpl proj = null;
    switch (projType) {
      case 1:
        proj = new LambertConformal(lat0, lon0, lat1, lat2);
        projCT = new ProjectionCT("Lambert", "FGDC", proj);
        // System.out.println(" using LC "+proj.paramsToString());
        break;
      case 2:
        proj = new Stereographic(centralLat, centralLon, 1.0);
        projCT = new ProjectionCT("Stereographic", "FGDC", proj);
        break;
      case 3:
        proj = new TransverseMercator(centralLat, centralLon, 1.0);
        projCT = new ProjectionCT("TransverseMercator", "FGDC", proj);
        break;
      default:
        parseInfo.append("ERROR: unknown projection type = "+projType);
        break;
    }

    if (standardLon != centralLon) {
      LatLonPointImpl lpt0 = new LatLonPointImpl( lat0,  lon0);
      LatLonPointImpl lpt1 = new LatLonPointImpl( centralLat,  centralLon);
      ProjectionPoint ppt0 = proj.latLonToProj(lpt0, new ProjectionPointImpl());
      ProjectionPoint ppt1 = proj.latLonToProj(lpt1, new ProjectionPointImpl());
      //System.out.println("ppt0="+ppt0+" lpt0= "+lpt0);
      //System.out.println("ppt1="+ppt1+" lpt1= "+lpt1);
      originX = ppt1.getX() - ppt0.getX();
      originY = ppt1.getY() - ppt0.getY();
    }

    // make axes
    ds.addCoordinateAxis( makeXCoordAxis( ds, "x", ds.findDimension("west_east")));
    ds.addCoordinateAxis( makeXCoordAxis( ds, "x_stag", ds.findDimension("west_east_stag")));
    ds.addCoordinateAxis( makeYCoordAxis( ds, "y", ds.findDimension("south_north")));
    ds.addCoordinateAxis( makeYCoordAxis( ds, "y_stag", ds.findDimension("south_north_stag")));
    ds.addCoordinateAxis( makeZCoordAxis( ds, "z", ds.findDimension("bottom_top")));
    ds.addCoordinateAxis( makeZCoordAxis( ds, "z_stag", ds.findDimension("bottom_top_stag")));

    // time coordinate variations
    CoordinateAxis taxis = makeTimeCoordAxis( ds, "time", ds.findDimension("Time"));
    if (taxis == null)
      taxis = makeTimeCoordAxis( ds, "time", ds.findDimension("Times"));
    if (taxis != null)
      ds.addCoordinateAxis( taxis);

    ds.addCoordinateAxis( makeSoilDepthCoordAxis( ds, "ZS"));

    if (projCT != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, projCT);
        v.addAttribute( new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
        ds.addVariable(null, v);
    }

    // make vertical coord transform variable
    //VariableDS ctv = makeVerticalCoordTransformVariable();
    //if (ctv != null)
     // ds.addVariable( null, ctv);

    ds.finish();
  }

  // pretty much WRF specific
  private String normalize( String units) {
    if (units.equals("fraction")) units="";
    else if (units.equals("dimensionless")) units="";
    else if (units.equals("NA")) units="";
    else if (units.equals("-")) units="";
    else {
      units = StringUtil.substitute( units, "**", "^");
      units = StringUtil.remove( units, '}');
      units = StringUtil.remove( units, '{');
    }
    return units;
  }
   /////////////////////////////////////////////////////////////////////////

  protected void makeCoordinateTransforms( NetcdfDataset ds) {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName());
      vp.isCoordinateTransform = true;
      vp.ct = projCT;
    }
    super.makeCoordinateTransforms( ds);
  }

  protected AxisType getAxisType( NetcdfDataset ds, VariableEnhanced ve) {
    Variable v = (Variable) ve;
    String vname = v.getName();

   if (vname.equalsIgnoreCase("x") || vname.equalsIgnoreCase("x_stag"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("y") || vname.equalsIgnoreCase("y_stag"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("z") || vname.equalsIgnoreCase("z_stag"))
      return AxisType.GeoZ;

    if (vname.equalsIgnoreCase("Z"))
       return AxisType.Height;

    if (vname.equalsIgnoreCase("time") || vname.equalsIgnoreCase("times"))
      return AxisType.Time;

    String unit = ve.getUnitsString();
    if (unit != null) {
      if ( SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if ( SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }
    

    return null;
  }

  /**  Does increasing values of Z go vertical  up?
    * @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger,
    * else return "down" */
  public String getZisPositive( CoordinateAxis v) {
  	return "down"; //eta coords decrease upward
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private CoordinateAxis makeXCoordAxis( NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dx = findAttributeDouble( ds, "DX") / 1000.0;
    int nx = dim.getLength();
    double startx = originX -dx * nx / 2; // - dx/2; // ya just gotta know
    //System.out.println(" originX= "+originX+" startx= "+startx);

    CoordinateAxis v = new CoordinateAxis1D( ds, null, axisName, DataType.DOUBLE, dim.getName(), "km", "synthesized GeoX coordinate from DX attribute");
    ds.setValues( v, nx, startx, dx);
    v.addAttribute( new Attribute(_Coordinate.AxisType, "GeoX"));
    if (!axisName.equals( dim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    //ADD: is staggered grid being dealt with?
    return v;
  }

  private CoordinateAxis makeYCoordAxis( NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dy = findAttributeDouble( ds, "DY") / 1000.0;
    int ny = dim.getLength();
    double starty = originY - dy * ny / 2; // - dy/2; // ya just gotta know
    //System.out.println(" originY= "+originY+" starty= "+starty);

    CoordinateAxis v = new CoordinateAxis1D( ds, null, axisName, DataType.DOUBLE, dim.getName(), "km", "synthesized GeoY coordinate from DY attribute");
    ds.setValues( v, ny, starty, dy);
    v.addAttribute( new Attribute(_Coordinate.AxisType, "GeoY"));
    if (!axisName.equals( dim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));
    //ADD: is staggered grid being dealt with?
    return v;
  }

  private CoordinateAxis makeZCoordAxis( NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    CoordinateAxis v = new CoordinateAxis1D( ds, null, axisName, DataType.SHORT, dim.getName(),"", "eta values");
    v.addAttribute( new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals( dim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    //use eta values from file variables: ZNU, ZNW
    //But they are a function of time though the values are the same in the sample file
    //NOTE: Use first time sample assuming all are the same!
    //ADD: Is this a safe assumption???
    Variable etaVar;
    if (axisName.endsWith("stag")) etaVar = ds.findVariable("ZNW");
    else etaVar = ds.findVariable("ZNU");
    if (etaVar == null) return makeFakeCoordAxis(ds, axisName, dim);
    int n = etaVar.getShape()[1];//number of eta levels
    int[] origin = new int[] {0,0};
    int[] shape = new int[] {1,n};
    try {
      Array array = etaVar.read(origin, shape);//read first time slice
      ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
      IndexIterator it = array.getIndexIterator();
      int count = 0;
      while (it.hasNext()) {
      	double d = it.getDoubleNext();
      	newArray.set(count++, d);
      }
      v.setCachedData(newArray, true);
    } catch (Exception e) {
      e.printStackTrace();
    }//ADD: error?

    return v;
  }

  private CoordinateAxis makeFakeCoordAxis( NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    CoordinateAxis v = new CoordinateAxis1D( ds, null, axisName, DataType.SHORT, dim.getName(), "", "synthesized coordinate: only an index");
    v.addAttribute( new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals( dim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    ds.setValues( v, dim.getLength(), 0, 1);
    return v;
  }

  private CoordinateAxis makeTimeCoordAxis( NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    int nt = dim.getLength();
    Variable timeV = ds.findVariable("Times");
    if (timeV == null) return null;

    Array timeData;
    try {
      timeData = timeV.read();
    } catch (IOException ioe) {
      return null;
    }

    ArrayDouble.D1 values = new ArrayDouble.D1( nt);
    int count = 0;

    if (timeData instanceof ArrayChar) {
      ArrayChar.StringIterator iter = ((ArrayChar) timeData).getStringIterator();
      while (iter.hasNext()) {
        String dateS = iter.next();
        try {
          Date d = dateFormat.parse(dateS);
          values.set(count++, (double) d.getTime() / 1000);
        } catch (java.text.ParseException e) {
          parseInfo.append("ERROR: cant parse Time string = <" + dateS+
              "> err="+e.getMessage()+"\n");

          // one more try
          String startAtt = ds.findAttValueIgnoreCase(null,"START_DATE",null);
          if ((nt == 1) && (null != startAtt)) {
            try {
              Date d = dateFormat.parse(startAtt);
              values.set(0, (double) d.getTime() / 1000);
            } catch (java.text.ParseException e2) {
              parseInfo.append("ERROR: cant parse global attribute START_DATE = <" + startAtt+
                  "> err="+e2.getMessage()+"\n");
            }
          }
        }
      }
    } else {
      IndexIterator iter = timeData.getIndexIterator();
      while (iter.hasNext()) {
        String dateS = (String) iter.next();
        try {
          Date d = dateFormat.parse(dateS);
          values.set(count++, (double) d.getTime() / 1000);
        } catch (java.text.ParseException e) {
          parseInfo.append("ERROR: cant parse Time string = " + dateS);
        }
      }

    }

    CoordinateAxis v = new CoordinateAxis1D( ds, null, axisName, DataType.DOUBLE, dim.getName(),
      "secs since 1970-01-01 00:00:00", "synthesized time coordinate from Times(time)");
    v.addAttribute( new Attribute(_Coordinate.AxisType, "Time"));
    if (!axisName.equals( dim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    v.setCachedData( values, true);
    return v;
  }

  private VariableDS makeSoilDepthCoordAxis( NetcdfDataset ds, String coordVarName) {
    Variable coordVar = ds.findVariable(coordVarName);
    if (null == coordVar)
      return null;

    Dimension soilDim = null;
    List dims = coordVar.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = (Dimension) dims.get(i);
      if (d.getName().startsWith("soil_layers"))
        soilDim = d;
    }
    if (null == soilDim)
      return null;

    if (coordVar.getRank() == 1) {
      coordVar.addAttribute( new Attribute(_Coordinate.AxisType, "GeoZ"));
      if (!coordVarName.equals( soilDim.getName()) )
        coordVar.addAttribute( new Attribute(_Coordinate.AliasForDimension, soilDim.getName()));
      return (VariableDS) coordVar;
    }

    String units = ds.findAttValueIgnoreCase(coordVar, "units", "");

    CoordinateAxis v = new CoordinateAxis1D( ds, null, "soilDepth", DataType.SHORT, soilDim.getName(), units, "soil depth");
    v.addAttribute( new Attribute(_Coordinate.AxisType, "GeoZ"));
    v.addAttribute( new Attribute("units", "units"));
    if (!v.getShortName().equals( soilDim.getName()) )
      v.addAttribute( new Attribute(_Coordinate.AliasForDimension, soilDim.getName()));

    //read first time slice
    int n = coordVar.getShape()[1];
    int[] origin = new int[] {0,0};
    int[] shape = new int[] {1,n};
    try {
      Array array = coordVar.read(origin, shape);
      ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
      IndexIterator it = array.getIndexIterator();
      int count = 0;
      while (it.hasNext()) {
      	double d = it.getDoubleNext();
      	newArray.set(count++, d);
      }
      v.setCachedData(newArray, true);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return v;
  }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    if (att == null) return Double.NaN;
    return att.getNumericValue().doubleValue();
  }

  /**
   * Assign CoordinateTransform objects to Coordinate Systems.
   */
  protected void assignCoordinateTransforms(NetcdfDataset ncDataset) {
    super.assignCoordinateTransforms(ncDataset);

    // any cs whose got a vertical coordinate with no units
    List csys = ncDataset.getCoordinateSystems();
    for (int i = 0; i < csys.size(); i++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(i);
      if (cs.getZaxis() != null) {
        String units = cs.getZaxis().getUnitsString();
        if ((units == null) || (units.trim().length() == 0)) {
          VerticalCT vct = makeWRFEtaVerticalCoordinateTransform(ncDataset, cs);
          if (vct != null)
            cs.addCoordinateTransform(vct);
          parseInfo.append("***Added WRFEta verticalCoordinateTransform to " + cs.getName() + "\n");
        }
      }
    }
  }

  private VerticalCT makeWRFEtaVerticalCoordinateTransform(NetcdfDataset ds, CoordinateSystem cs) {
    if ((null == ds.findVariable("PH")) || (null == ds.findVariable("PHB")) ||
        (null == ds.findVariable("P")) || (null == ds.findVariable("PB")))
      return null;

    WRFEtaTransformBuilder builder = new WRFEtaTransformBuilder(cs);
    return (VerticalCT) builder.makeCoordinateTransform(ds, null);
  }

  /* private boolean isStaggered(CoordinateAxis axis) {
  	if (axis == null) return false;
  	String name = axis.getName();
  	if (name == null) return false;
  	if (name.endsWith("stag")) return true;
  	return false;
  }

  private class WRFEtaBuilder extends AbstractCoordTransBuilder {
    private CoordinateSystem cs;

    WRFEtaBuilder(CoordinateSystem cs) {
      this.cs = cs;
    }

    public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable v) {
      VerticalCT.Type type = VerticalCT.Type.WRFEta;
      VerticalCT ct = new VerticalCT(type.toString(), conventionName, type, this);

      ct.addParameter(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
      ct.addParameter(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
      ct.addParameter(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
      ct.addParameter(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
      ct.addParameter(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
      ct.addParameter(new Parameter(WRFEta.BasePressureVariable, "PB"));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredX, ""+isStaggered(cs.getXaxis())));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredY, ""+isStaggered(cs.getYaxis())));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredZ, ""+isStaggered(cs.getZaxis())));
      ct.addParameter(new Parameter("eta", ""+cs.getZaxis().getName()));

      parseInfo.append(" added vertical coordinate transform = "+type+"\n");
      return ct;
    }

    public String getTransformName() {
      return "WRF_Eta";
    }

    public TransformType getTransformType() {
      return TransformType.Vertical;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new WRFEta(ds, timeDim, vCT);
    }

  } */

}