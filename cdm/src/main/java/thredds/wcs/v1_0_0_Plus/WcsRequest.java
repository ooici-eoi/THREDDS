package thredds.wcs.v1_0_0_Plus;

import java.util.*;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Represent the incoming WCS 1.0.0+ request.
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsRequest
{
  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs" );
  protected static final Namespace gmlNS = Namespace.getNamespace( "gml", "http://www.opengis.net/gml" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  // General request info
  private Operation operation;
  private String version;

  // Dataset
  private String datasetPath;
  private GridDataset dataset;
  private HashMap<String,GridDataset.Gridset> availableCoverages;

  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  public enum RequestEncoding
  {
    GET_KVP, POST_XML, POST_SOAP
  }

  public enum Format
  {
    NONE, GeoTIFF, GeoTIFF_Float, NetCDF3
  }

  WcsRequest( Operation operation, String version, String datasetPath, GridDataset dataset )
  {
    this.operation = operation;
    this.version = version;
    this.datasetPath = datasetPath;
    this.dataset = dataset;
    this.availableCoverages = new HashMap<String,GridDataset.Gridset>();

    // The following coverage list drops the WCS 1.0 restriction on the
    // rangeSet in favor of WCS 1.1 rangeSet.
    // NOTE: For WCS 1.0 replace the for loop with:
    //     for ( GridDatatype curGridDatatype : this.dataset.getGrids() ) {...}
    for ( GridDataset.Gridset curGridSet : this.dataset.getGridsets())
    {
      GridCoordSystem gcs = curGridSet.getGeoCoordSystem();
      if ( !gcs.isRegularSpatial() )
        continue;
      this.availableCoverages.put( gcs.getName(), curGridSet );
    }

    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  public Operation getOperation() { return operation; }
  public String getVersion() { return version; }
  public String getDatasetPath() { return datasetPath; }
  public GridDataset getDataset() { return dataset; }

  public boolean isAvailableCoverageName( String name )
  {
    return availableCoverages.containsKey( name);
  }

  public GridDataset.Gridset getAvailableCoverage( String name )
  {
    return availableCoverages.get( name);
  }
  public Collection<GridDataset.Gridset> getAvailableCoverageCollection()
  {
    return Collections.unmodifiableCollection( availableCoverages.values());
  }

  protected Element genCoverageOfferingBriefElem( String elemName, String covName, String covLabel,
                                              GridCoordSystem gridCoordSys )
  {

    // <CoverageOfferingBrief>
    Element briefElem = new Element( elemName, wcsNS );

    // <CoverageOfferingBrief>/gml:metaDataProperty [0..*]
    // <CoverageOfferingBrief>/gml:description [0..1]
    // <CoverageOfferingBrief>/gml:name [0..*]
    // <CoverageOfferingBrief>/metadataLink [0..*]

    // <CoverageOfferingBrief>/description [0..1]
    // <CoverageOfferingBrief>/name [1]
    // <CoverageOfferingBrief>/label [1]
    briefElem.addContent( new Element( "name", wcsNS ).addContent( covName ) );
    briefElem.addContent( new Element( "label", wcsNS ).addContent( covLabel ) );

    // <CoverageOfferingBrief>/lonLatEnvelope [1]
    briefElem.addContent( genLonLatEnvelope( gridCoordSys ) );

    // ToDo Add keywords capabilities.
    // <CoverageOfferingBrief>/keywords [0..*]  /keywords [1..*] and /type [0..1]

    return briefElem;
  }

  private Element genLonLatEnvelope( GridCoordSystem gcs )
  {
    // <CoverageOfferingBrief>/lonLatEnvelope
    Element lonLatEnvelopeElem = new Element( "lonLatEnvelope", wcsNS );
    lonLatEnvelopeElem.setAttribute( "srsName", "urn:ogc:def:crs:OGC:1.3:CRS84" );

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:pos
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( llpt.getLongitude() + " " + llpt.getLatitude() ) );
    double lon = llpt.getLongitude() + llbb.getWidth();
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( lon + " " + urpt.getLatitude() ) );
// ToDo Add vertical
//    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
//    if ( vertAxis != null )
//    {
//      // ToDo Deal with conversion to meters. Yikes!!
//      // See verAxis.getUnitsString()
//      lonLatEnvelopeElem.addContent(
//              new Element( "pos", gmlNS).addContent(
//                      vertAxis.getCoordValue( 0) + " " +
//                      vertAxis.getCoordValue( ((int)vertAxis.getSize()) - 1)));
//    }
// ToDo Add vertical

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getStart().toDateTimeStringISO()) );
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getEnd().toDateTimeStringISO()) );
    }

    return lonLatEnvelopeElem;
  }

}
