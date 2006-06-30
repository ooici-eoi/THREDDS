// $Id: SingleTrajectoryObsDataset.java,v 1.14 2006/06/06 16:07:15 caron Exp $
package ucar.nc2.dt.trajectory;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.VariableSimpleAdapter;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Superclass for for implementations of TrajectoryObsDataset using a
 * NetcdfFile underneath that contains a single trajectory. The file
 * must have a single coordinate variable for time. The time dimension
 * may be UNLIMITED (if time is not UNLIMITED, there must be no UNLIMITED
 * dimension). The file must also have a latitude variable, a longitude
 * variable, and an elevation variable each on the time dimension only.
 * The data variables also must be on the time dimension but they can
 * also have other dimensions.
 * For instance:
 * <pre>
 * time( time)  - convertable to -> double
 * lat( time)   - convertable to -> double
 * lon( time)   - convertable to -> double
 * elev( time)  - convertable to -> double
 * var1( time[, dim#]*)
 * ...
 * varN( time[, dim#]*)
 * </pre>
 *
 * @author edavis
 * @since 5 May 2005 10:12 -0600
 */
abstract class SingleTrajectoryObsDataset
        extends TypedDatasetImpl
        implements TrajectoryObsDataset
{
  protected String trajectoryId; // the Id String for the trajectory.
  protected int trajectoryNumPoint; // the num of points in the trajectory.
  protected HashMap trajectoryVarsMap; // a Map of all the TypedDataVariables in the trajectory keyed by their names.

  protected Dimension timeDim;
  protected Variable timeVar;
  protected Structure recordVar;
  protected Variable latVar;
  protected Variable lonVar;
  protected Variable elevVar;

  protected String timeVarUnitsString;

  protected double elevVarUnitsConversionFactor;

  protected TrajectoryObsDatatype trajectory;

  public SingleTrajectoryObsDataset( NetcdfFile ncfile)
  {
    super( ncfile);
  }

  /** Setup needed for all SingleTrajectoryObsDatatypes. Can only be called once.
   *
   * Units of time varible must be udunits time units.
   * Units of latitude variable must be convertible to "degrees_north" by udunits.
   * Units of longitude variable must be convertible to "degrees_east" by udunits.
   * Units of altitude variable must be convertible to "meters" by udunits.
   *
   * @throws IllegalArgumentException if units of time, latitude, longitude, or altitude variables are not as required.
   * @throws IllegalStateException if this method has already been called.
   */
  public void setTrajectoryInfo( Config trajConfig )
          throws IOException
  {
    if ( timeDim != null )
      throw new IllegalStateException( "The setTrajectoryInfo() method can only be called once.");

    this.trajectoryId = trajConfig.getTrajectoryId();
    this.timeDim = trajConfig.getTimeDim();
    this.timeVar = trajConfig.getTimeVar();
    this.latVar = trajConfig.getLatVar();
    this.lonVar = trajConfig.getLonVar();
    this.elevVar = trajConfig.getElevVar();

    trajectoryNumPoint = this.timeDim.getLength();
    timeVarUnitsString = this.timeVar.findAttribute( "units" ).getStringValue();

    // Check that time, lat, lon, elev units are acceptable.
    if ( DateUnit.getStandardDate( timeVarUnitsString ) == null )
    {
      throw new IllegalArgumentException( "Units of time variable <" + timeVarUnitsString + "> not a date unit." );
    }
    String latVarUnitsString = this.latVar.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( latVarUnitsString, "degrees_north" ) )
    {
      throw new IllegalArgumentException( "Units of lat var <" + latVarUnitsString + "> not compatible with \"degrees_north\"." );
    }
    String lonVarUnitsString = this.lonVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( lonVarUnitsString, "degrees_east" ) )
    {
      throw new IllegalArgumentException( "Units of lon var <" + lonVarUnitsString + "> not compatible with \"degrees_east\"." );
    }
    String elevVarUnitsString = this.elevVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( elevVarUnitsString, "meters" ) )
    {
      throw new IllegalArgumentException( "Units of elev var <" + elevVarUnitsString + "> not compatible with \"meters\"." );
    }

    try
    {
      elevVarUnitsConversionFactor = getMetersConversionFactor( elevVarUnitsString);
    }
    catch ( Exception e )
    {
      throw new IllegalArgumentException( "Exception on getMetersConversionFactor() for the units of elev var <" + elevVarUnitsString + ">." );
    }

    if ( this.ncfile.hasUnlimitedDimension() && this.ncfile.getUnlimitedDimension().equals( timeDim ) )
    {
      if ( this.ncfile.addRecordStructure() )
        this.recordVar = (Structure) this.ncfile.getRootGroup().findVariable( "record");
      else
        this.recordVar = new StructurePseudo( this.ncfile, null, "record", timeDim ); 
    } else {
      this.recordVar = new StructurePseudo( this.ncfile, null, "record", timeDim);
    }

    // @todo HACK, HACK, HACK - remove once addRecordStructure() deals with ncd attribute changes.
    Variable elevVarInRecVar = this.recordVar.findVariable( this.elevVar.getName());
    if ( ! elevVarUnitsString.equals( elevVarInRecVar.findAttribute( "units").getStringValue()))
    {
      elevVarInRecVar.addAttribute( new Attribute( "units", elevVarUnitsString));
    }

    trajectoryVarsMap = new HashMap();
    //for ( Iterator it = this.recordVar.getVariables().iterator(); it.hasNext(); )
    for ( Iterator it = this.ncfile.getRootGroup().getVariables().iterator(); it.hasNext(); )
    {
      Variable curVar = (Variable) it.next();
      if ( curVar.getRank() > 0 &&
           !curVar.equals( this.timeVar) &&
           ! curVar.equals( this.latVar) &&
           ! curVar.equals( this.lonVar) &&
           ! curVar.equals( this.elevVar) &&
           ( this.recordVar == null ? true : ! curVar.equals( this.recordVar)))
      {
        MyTypedDataVariable typedVar = new MyTypedDataVariable( new VariableDS( null, curVar, true ) );
        dataVariables.add( typedVar);
        trajectoryVarsMap.put( typedVar.getName(), typedVar);
      }
    }

    trajectory = new SingleTrajectory( this.trajectoryId, trajectoryNumPoint,
                                       this.timeVar, timeVarUnitsString,
                                       this.latVar, this.lonVar, this.elevVar,
                                       dataVariables, trajectoryVarsMap);

    startDate = trajectory.getTime( 0);
    endDate = trajectory.getTime( trajectoryNumPoint - 1);

    ( (SingleTrajectory) trajectory).setStartDate( startDate );
    ( (SingleTrajectory) trajectory).setEndDate( endDate );
  }

  protected static SimpleUnit meterUnit = SimpleUnit.factory( "meters" );

  protected static double getMetersConversionFactor( String unitsString ) throws Exception
  {
    SimpleUnit unit = SimpleUnit.factoryWithExceptions( unitsString );
    return unit.convertTo( 1.0, meterUnit );
  }

  protected void setStartDate() {}

  protected void setEndDate() {}

  protected void setBoundingBox() {}

  public List getTrajectoryIds()
  {
    List l = new ArrayList();
    l.add( trajectoryId);
    return( l);
  }

  public List getTrajectories() // throws IOException;
  {
    List l = new ArrayList();
    l.add( trajectory);
    return( l);
  }

  public TrajectoryObsDatatype getTrajectory( String trajectoryId ) // throws IOException;
  {
    if ( ! trajectoryId.equals( this.trajectoryId)) return( null);
    return( trajectory);
  }

  public String getDetailInfo()
  {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append( "TrajectoryObsDataset\n" );
    sbuff.append( "  adapter   = " + getClass().getName() + "\n" );
    sbuff.append( "  trajectories:" + "\n" );
    for ( Iterator it = this.getTrajectoryIds().iterator(); it.hasNext(); )
    {
      sbuff.append( "      " + (String) it.next() + "\n" );
    }
    sbuff.append( super.getDetailInfo() );

    return sbuff.toString();
  }

  public boolean syncExtend()
  {
    if ( ! this.ncfile.hasUnlimitedDimension()) return false;
    try
    {
      if ( ! this.ncfile.syncExtend() ) return false;
    }
    catch ( IOException e )
    {
      return false;
    }

    // Update number of points in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
    int newNumPoints = this.timeDim.getLength();
    if ( this.trajectoryNumPoint >= newNumPoints )
      return false;
    this.trajectoryNumPoint = newNumPoints;
    ( (SingleTrajectory) this.trajectory).setNumPoints( this.trajectoryNumPoint);

    // Update end date in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
    try
    {
      endDate = trajectory.getTime( trajectoryNumPoint - 1 );
    }
    catch ( IOException e )
    {
      return false;
    }
    ( (SingleTrajectory) trajectory ).setEndDate( endDate );

    return true;
  }

  public static class Config
  {
    protected String trajectoryId; // the Id String for the trajectory.

    protected Dimension timeDim;
    protected Variable timeVar;
    protected Variable latVar;
    protected Variable lonVar;
    protected Variable elevVar;

    public Config() {}

    public Config( String trajectoryId, Dimension timeDim, Variable timeVar,
                   Variable latVar, Variable lonVar, Variable elevVar )
    {
      this.trajectoryId = trajectoryId;
      this.timeDim = timeDim;
      this.timeVar = timeVar;
      this.latVar = latVar;
      this.lonVar = lonVar;
      this.elevVar = elevVar;
    }

    public void setTrajectoryId( String trajectoryId )
    {
      this.trajectoryId = trajectoryId;
    }

    public void setTimeDim( Dimension timeDim )
    {
      this.timeDim = timeDim;
    }

    public void setTimeVar( Variable timeVar )
    {
      this.timeVar = timeVar;
    }

    public void setLatVar( Variable latVar )
    {
      this.latVar = latVar;
    }

    public void setLonVar( Variable lonVar )
    {
      this.lonVar = lonVar;
    }

    public void setElevVar( Variable elevVar )
    {
      this.elevVar = elevVar;
    }

    public String getTrajectoryId()
    {
      return trajectoryId;
    }

    public Dimension getTimeDim()
    {
      return timeDim;
    }

    public Variable getTimeVar()
    {
      return timeVar;
    }

    public Variable getLatVar()
    {
      return latVar;
    }

    public Variable getLonVar()
    {
      return lonVar;
    }

    public Variable getElevVar()
    {
      return elevVar;
    }
  }

  private class SingleTrajectory implements TrajectoryObsDatatype
  {

    private String id;
    private String description;
    private int numPoints;
    private Date startDate;
    private Date endDate;
    private String timeVarUnitsString;
    private Variable timeVar;
    private Variable latVar;
    private Variable lonVar;
    private Variable elevVar;
    private List variables;
    private HashMap variablesMap;

    //private Structure struct;


    private SingleTrajectory( String id, int numPoints,
                              Variable timeVar, String timeVarUnitsString,
                              Variable latVar, Variable lonVar, Variable elevVar,
                              List variables, HashMap variablesMap )
    {
      this.description = null;
      this.id = id;
      this.numPoints = numPoints;
      this.timeVarUnitsString = timeVarUnitsString;
      this.timeVar = timeVar;
      this.variables = variables;
      this.variablesMap = variablesMap;
      this.latVar = latVar;
      this.lonVar = lonVar;
      this.elevVar = elevVar;

      //this.struct = new Structure( ncfile, ncfile.getRootGroup(), null, "struct for building StructureDatas");
    }

    protected void setNumPoints( int numPoints )
    {
      this.numPoints = numPoints;
    }
    protected void setStartDate( Date startDate )
    {
      if ( this.startDate != null ) throw new IllegalStateException( "Can only call setStartDate() once." );
      this.startDate = startDate;
    }
    protected void setEndDate( Date endDate )
    {
      //if ( this.endDate != null ) throw new IllegalStateException( "Can only call setEndDate() once.");
      this.endDate = endDate;
    }

    public String getId()
    {
      return( id);
    }

    public String getDescription()
    {
      return( description);
    }

    public int getNumberPoints()
    {
      return( numPoints);
    }

    public List getDataVariables()
    {
      return ( variables );
    }

    public VariableSimpleIF getDataVariable( String name )
    {
      return ( (VariableSimpleIF) variablesMap.get( name ) );
    }

    public PointObsDatatype getPointObsData( int point ) throws IOException
    {
      return( new MyPointObsDatatype( point));
    }

    public Date getStartDate()
    {
      return( startDate);
    }

    public Date getEndDate()
    {
      return( endDate);
    }

    public LatLonRect getBoundingBox()
    {
      return null;
    }

    public Date getTime( int point ) throws IOException
    {
      return ( DateUnit.getStandardDate( getTimeValue( point ) + " " + timeVarUnitsString ) );
    }

    public EarthLocation getLocation( int point ) throws IOException
    {
      return ( new MyEarthLocation( point ) );
    }

    public String getTimeUnitsIdentifier()
    {
      return( timeVarUnitsString);
    }

    public double getTimeValue( int point ) throws IOException
    {
      Array array = null;
      try
      {
        array = getTime( this.getPointRange( point ) );
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + ( this.getNumberPoints() - 1 ) + ">: " + e.getMessage() );
        iae.initCause( e );
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else if ( array instanceof ArrayInt )
      {
        return ( array.getInt( array.getIndex() ) );
      }
      else
      {
        throw new IOException( "Time variable not float, double, or integer <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are degrees_north
    public double getLatitude( int point ) throws IOException // required, units degrees_north
    {
      Array array = null;
      try
      {
        array = getLatitude( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else
      {
        throw new IOException( "Latitude variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are degrees_east
    public double getLongitude( int point ) throws IOException // required, units degrees_east
    {
      Array array = null;
      try
      {
        array = getLongitude( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else
      {
        throw new IOException( "Longitude variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are meters
    public double getElevation( int point ) throws IOException // optional; units meters;  missing = NaN.
    {
      Array array = null;
      try
      {
        array = getElevation( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return array.getDouble( array.getIndex());
      }
      else if ( array instanceof ArrayFloat)
      {
        return array.getFloat( array.getIndex());
      }
      else
      {
        throw new IOException( "Elevation variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    public StructureData getData( int point ) throws IOException, InvalidRangeException
    {
      return SingleTrajectoryObsDataset.this.recordVar.readStructure( point );
    }

    public Array getData( int point, String parameterName ) throws IOException
    {
      try
      {
        return( getData( this.getPointRange( point), parameterName));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
    }

    public Range getFullRange()
    {
      Range range = null;
      try
      {
        range = new Range(0, this.getNumberPoints()-1);
      }
      catch ( InvalidRangeException e )
      {
        IllegalStateException ise = new IllegalStateException( "Full trajectory range invalid <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        ise.initCause( e);
        throw( ise);
      }
      return range;
    }

    public Range getPointRange( int point ) throws InvalidRangeException
    {
      if ( point >= this.getNumberPoints()) throw new InvalidRangeException( "Point <" + point + "> not in acceptible range <0, " + (this.getNumberPoints()-1) + ">.");
      return( new Range(point, point));
    }

    public Range getRange( int start, int end, int stride ) throws InvalidRangeException
    {
      if ( end >= this.getNumberPoints()) throw new InvalidRangeException( "End point <" + end + "> not in acceptible range <0, " + (this.getNumberPoints()-1) + ">.");
      return( new Range( start, end, stride));
    }

    public Array getTime( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      return( timeVar.read( section));
    }

    // @todo Make sure units are degrees_north
    public Array getLatitude( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      return( latVar.read( section));
    }

    // @todo Make sure units are degrees_east
    public Array getLongitude( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      return( lonVar.read( section));
    }

    // @todo Make sure units are meters
    public Array getElevation( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      Array a = elevVar.read( section);
      if ( elevVarUnitsConversionFactor == 1.0) return( a);
      for ( IndexIterator it = a.getIndexIterator(); it.hasNext(); )
      {
        if ( elevVar.getDataType() == DataType.DOUBLE )
        {
          double val = it.getDoubleNext();
          it.setDoubleCurrent( val * elevVarUnitsConversionFactor );
        }
        else if ( elevVar.getDataType() == DataType.FLOAT )
        {
          float val = it.getFloatNext();
          it.setFloatCurrent( (float) ( val * elevVarUnitsConversionFactor ) );
        }
        else if ( elevVar.getDataType() == DataType.INT )
        {
          int val = it.getIntNext();
          it.setIntCurrent( (int) ( val * elevVarUnitsConversionFactor ) );
        }
        else if ( elevVar.getDataType() == DataType.LONG )
        {
          long val = it.getLongNext();
          it.setLongCurrent( (long) ( val * elevVarUnitsConversionFactor ) );
        }
        else
        {
          throw new IllegalStateException( "Elevation variable type <" + elevVar.getDataType().toString() + "> not double, float, int, or long." );
        }
      }
      return( a);
    }

    public Array getData( Range range, String parameterName ) throws IOException, InvalidRangeException
    {
      Variable variable = ncfile.getRootGroup().findVariable( parameterName );
      int varRank = variable.getRank();
      int [] varShape = variable.getShape();
      List section = new ArrayList( varRank);
      section.add( range);
      for ( int i = 1; i < varRank; i++)
      {
        section.add( new Range( 0, varShape[i]-1 ) );
      }
      Array array = variable.read( section);
      if ( array.getShape()[0] == 1 )
      {
        return ( array.reduce( 0 ) );
      }
      else
      {
        return ( array );
      }
      //return( array.getShape()[0] == 1 ? array.reduce( 0 ) : array);
    }

    public DataIterator getDataIterator( int bufferSize ) throws IOException
    {
      return new PointDatatypeIterator( recordVar, bufferSize );
    }

    private class PointDatatypeIterator extends DatatypeIterator
    {
      protected Object makeDatatypeWithData( int recnum, StructureData sdata )
      {
        return new MyPointObsDatatype( recnum, sdata );
      }

      PointDatatypeIterator( Structure struct, int bufferSize )
      {
        super( struct, bufferSize );
      }
    }


    // PointObsDatatype implementation used by SingleTrajectory.
    private class MyPointObsDatatype implements PointObsDatatype
    {
      private int point;
      private StructureData sdata;
      private double time;
      private EarthLocation earthLoc;

      private MyPointObsDatatype( int point) throws IOException
      {
        this.point = point;
        this.time = SingleTrajectory.this.getTimeValue( point);
        this.earthLoc = SingleTrajectory.this.getLocation( point);
      }

      private MyPointObsDatatype( int point, StructureData sdata )
      {
        this.point = point;
        this.sdata = sdata;
        this.time = sdata.getScalarDouble( SingleTrajectory.this.timeVar.getName());
        this.earthLoc = new MyEarthLocation( sdata);
      }

      public double getNominalTime()
      {
        return ( this.time );
      }

      public double getObservationTime()
      {
        return ( this.time );
      }

      public Date getNominalTimeAsDate() {
        String dateStr = getNominalTime() + " " + timeVarUnitsString;
        return DateUnit.getStandardDate( dateStr );
      }

      public Date getObservationTimeAsDate() {
        String dateStr = getObservationTime() + " " + timeVarUnitsString;
        return DateUnit.getStandardDate( dateStr );
      }

      public EarthLocation getLocation()
      {
        return( this.earthLoc);
      }

      public StructureData getData() throws IOException
      {
        if ( sdata != null ) return sdata;
        try {
          return( SingleTrajectory.this.getData( point));
        } catch (InvalidRangeException e) {
          throw new IllegalStateException( e.getMessage());
        }
      }
    }

    // EarthLocation implementation used by SingleTrajectory.
    private class MyEarthLocation implements EarthLocation
    {
      private double latitude;
      private double longitude;
      private double elevation;

      private MyEarthLocation( int point) throws IOException
      {
        this.latitude = SingleTrajectory.this.getLatitude( point);
        this.longitude = SingleTrajectory.this.getLongitude( point);
        this.elevation = SingleTrajectory.this.getElevation( point);
      }

      private MyEarthLocation( StructureData sdata )
      {
        this.latitude = sdata.getScalarDouble( SingleTrajectory.this.latVar.getName() );
        this.longitude = sdata.getScalarDouble( SingleTrajectory.this.lonVar.getName() );
        this.elevation = sdata.getScalarDouble( SingleTrajectory.this.elevVar.getName() );
        if ( elevVarUnitsConversionFactor != 1.0 ) this.elevation *= elevVarUnitsConversionFactor;
      }

      public double getLatitude()
      {
        return( this.latitude);
      }

      public double getLongitude()
      {
        return( this.longitude);
      }

      public double getAltitude()
      {
        return( this.elevation);
      }
    }
  }

  private class MyTypedDataVariable extends VariableSimpleAdapter
  {
    private int rank;
    private int[] shape;
    private MyTypedDataVariable( VariableDS v )
    {
      super( v );

      // Calculate the rank and shape of the variable, removing trajectory dimesion.
      rank = super.getRank() - 1;
      int[] varShape = super.getShape();
      shape = new int[ varShape.length - 1];
      int trajDimIndex = v.findDimensionIndex( SingleTrajectoryObsDataset.this.timeDim.getName());
      for ( int i = 0, j = 0; i < varShape.length; i++)
      {
        if ( i == trajDimIndex) continue;
        shape[ j++] = varShape[ i];
      }
    }

    public int getRank()
    {
      return( rank);
    }

    public int[] getShape()
    {
      return( shape);
    }
  }
}

/*
 * $Log: SingleTrajectoryObsDataset.java,v $
 * Revision 1.14  2006/06/06 16:07:15  caron
 * *** empty log message ***
 *
 * Revision 1.13  2006/05/08 02:47:33  caron
 * cleanup code for 1.5 compile
 * modest performance improvements
 * dapper reading, deal with coordinate axes as structure members
 * improve DL writing
 * TDS unit testing
 *
 * Revision 1.12  2006/02/09 22:57:12  edavis
 * Add syncExtend() method to TrajectoryObsDataset.
 *
 * Revision 1.11  2005/07/25 16:12:10  caron
 * cleanup unit testing
 * NetcdfDataset does not modify wrapped NetcdfFile, allowing it to be acquired
 *
 * Revision 1.10  2005/05/23 23:36:22  edavis
 * Add checking and converting of units for the latitude, longitude,
 * and elevation variables in MultiTrajectoryObsDataset.
 *
 * Revision 1.9  2005/05/23 22:47:00  edavis
 * Handle changing elevation data units (done in ncDataset
 * and record structure, needed some changes from John, too).
 *
 * Revision 1.8  2005/05/23 20:18:35  caron
 * refactor for scale/offset/missing
 *
 * Revision 1.7  2005/05/23 17:02:22  edavis
 * Deal with converting elevation data into "meters".
 *
 * Revision 1.6  2005/05/20 19:44:27  edavis
 * Add getDataIterator() to TrajectoryObsDatatype and implement
 * in SingleTrajectoryObsDataset (returns null in
 * MultiTrajectoryObsDataset).
 *
 * Revision 1.5  2005/05/16 16:47:52  edavis
 * A few improvements to SingleTrajectoryObsDataset and start using
 * it in RafTrajectoryObsDataset. Add MultiTrajectoryObsDataset
 * (based on SingleTrajectoryObsDataset) and use in
 * Float10TrajectoryObsDataset.
 *
 * Revision 1.4  2005/05/11 19:58:11  caron
 * add VariableSimpleIF, remove TypedDataVariable
 *
 * Revision 1.3  2005/05/11 00:10:03  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.2  2005/05/06 22:17:41  edavis
 * Add use of TypedDatasetAdapter as subclass.
 *
 * Revision 1.1  2005/05/06 21:04:18  edavis
 * Add this superclass for implementations of TrajectoryObsDataset.
 *
 * Revision 1.6  2005/05/05 16:08:13  edavis
 * Add TrajectoryObsDatatype.getDataVariables() methods.
 *
 * Revision 1.5  2005/05/04 17:18:45  caron
 * *** empty log message ***
 *
 * Revision 1.4  2005/05/01 19:16:03  caron
 * move station to point package
 * add implementations for common interfaces
 * refactor station adapters
 *
 * Revision 1.3  2005/03/18 00:29:07  edavis
 * Finish trajectory implementations with the new TrajectoryObsDatatype
 * and TrajectoryObsDataset interfaces and update tests.
 *
 * Revision 1.2  2005/03/15 23:20:53  caron
 * new radial dataset interface
 * change getElevation() to getAltitude()
 *
 * Revision 1.1  2005/03/10 21:34:17  edavis
 * Redo trajectory implementations with new TrajectoryObsDatatype and
 * TrajectoryObsDataset interfaces.
 *
 * Revision 1.1  2005/03/01 22:02:23  edavis
 * Two more implementations of the TrajectoryDataset interface.
 *
 */