package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;
import thredds.wcs.v1_1_0.WcsException;
import thredds.wcs.v1_1_0.GetCapabilities;
import thredds.wcs.v1_1_0.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;

/**
 * Represent the incoming WCS 1.1.0 request.
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestParser
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsRequestParser.class );

  public static Request parseRequest( String version, HttpServletRequest req, HttpServletResponse res )
          throws WcsException
  {
    // These are handled in WcsServlet. Don't need to validate here.
//    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
//    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
//    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

    // General request info
    Request request; // The Request object to be built and returned.
    Request.Operation operation;
    GridDataset dataset = openDataset( req, res );

    // GetCapabilities request info
    List<GetCapabilities.Section> sections;
    GetCapabilities.ServiceId serviceId;
    GetCapabilities.ServiceProvider serviceProvider;

    // DescribeCoverage request info

    // GetCoverage request info

    // Determine the request operation.
    String requestParam = ServletUtil.getParameterIgnoreCase( req, "Request" );
    try
    {
      operation = Request.Operation.valueOf( requestParam );
    }
    catch ( IllegalArgumentException e )
    {
      throw new WcsException( WcsException.Code.OperationNotSupported, requestParam, "" );
    }

    // Handle "GetCapabilities" request.
    if ( operation.equals( Request.Operation.GetCapabilities ) )
    {
      String sectionsParam = ServletUtil.getParameterIgnoreCase( req, "Sections" );
      String updateSequenceParam = ServletUtil.getParameterIgnoreCase( req, "UpdateSequence" );
      String acceptFormatsParam = ServletUtil.getParameterIgnoreCase( req, "AccpetFormats" );

      if ( sectionsParam != null )
      {
        String[] sectionArray = sectionsParam.split( "," );
        sections = new ArrayList<GetCapabilities.Section>( sectionArray.length );
        for ( String curSection : sectionArray )
        {
          sections.add( GetCapabilities.Section.valueOf( curSection ) );
        }
      }
      else
        sections = Collections.emptyList();

      serviceId = null;
      serviceProvider = null;
//      serviceId = new GetCapabilities.ServiceId( ThreddsConfig.get( "WCS.serviceId.title", null ),
//                                                 ThreddsConfig.get( "WCS.serviceId.abstract", null),
//                                                 )
      request = new Request( operation, version, sections, serviceId, serviceProvider, dataset );

    }
    // Handle "DescribeCoverage" request.
    else if ( operation.equals( Request.Operation.DescribeCoverage ) )
    {
      // The parameter is "Identifier" but the KVP endocing of the parameter is "Identifiers".
      // So, deal with both.
      String identifiers = ServletUtil.getParameterIgnoreCase( req, "Identifiers" );
      if ( identifiers == null )
        identifiers = ServletUtil.getParameterIgnoreCase( req, "Identifier");

      request = new Request();
    }
    // Handle "GetCoverage" request.
    else if ( operation.equals( Request.Operation.GetCoverage ) )
    {
      String identifier = ServletUtil.getParameterIgnoreCase( req, "Identifier" );

      request = new Request();
    }
    else
      throw new WcsException( WcsException.Code.OperationNotSupported, requestParam, "" );

    return request;
  }

  private static GridDataset openDataset( HttpServletRequest req, HttpServletResponse res )
          throws WcsException
  {
//    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
//    boolean isRemote = ( datasetURL != null );
//    String datasetPath = isRemote ? datasetURL : req.getPathInfo();
//
//    // convert to a GridDataset
//    GridDataset gd = isRemote ? ucar.nc2.dt.grid.GridDataset.open( datasetPath ) : DatasetHandler.openGridDataset( req, res, datasetPath );
//    if ( gd == null ) return null;

    GridDataset dataset;
    String datasetPath = req.getPathInfo();
    try
    {
      dataset = DatasetHandler.openGridDataset( req, res, datasetPath );
    }
    catch ( IOException e )
    {
      log.warn( "WcsRequestParser(): Failed to open dataset <" + datasetPath + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.NoApplicableCode, null, "Failed to open dataset, \"" + datasetPath + "\"." );
    }
    if ( dataset == null )
    {
      log.debug( "WcsRequestParser(): Unknown dataset <" + datasetPath + ">." );
      throw new WcsException( WcsException.Code.NoApplicableCode, null, "Unknown dataset, \"" + datasetPath + "\"." );
    }
    return dataset;
  }
}
