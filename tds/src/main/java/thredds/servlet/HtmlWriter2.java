// $Id: HtmlWriter2.java,v 1.6 2006/06/14 22:26:28 edavis Exp $
package thredds.servlet;

import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvCatalogRef;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Format;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.nc2.dt.GridDatatype;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 24, 2006 3:18:50 PM
 */
public class HtmlWriter2
{
  static private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( HtmlWriter2.class );

  private static HtmlWriter2 singleton;

  private String contextPath;
  private String contextName;
  private String contextVersion;
  private String userCssPath; // relative to context path
  private String contextLogoPath;    // relative to context path
  private String instituteLogoPath;    // relative to context path
  private String docsPath;    // relative to context path

  private ucar.nc2.units.DateFormatter formatter = new ucar.nc2.units.DateFormatter();

  /*
   * <li>Context path: "/thredds"</li>
 * <li>Servlet name: "THREDDS Data Server"</li>
 * <li>Documentation location: "/thredds/docs/"</li>
 * <li>Version information: ThreddsDefault.version</li>
 * <li>Catalog reference URL: "/thredds/catalogServices?catalog="</li>

  */

  public static void init( String contextPath, String contextName, String contextVersion,
                           String docsPath, String userCssPath,
                           String contextLogoPath, String instituteLogoPath )
  {
    if ( singleton != null )
    {
      log.warn( "init(): this method has already been called; it should only be called once." );
      return;
      //throw new IllegalStateException( "HtmlWriter2.init() has already been called.");
    }
    singleton = new HtmlWriter2( contextPath, contextName, contextVersion,
                                 docsPath, userCssPath,
                                 contextLogoPath, instituteLogoPath );
  }

  public static HtmlWriter2 getInstance()
  {
    if ( singleton == null )
    {
      log.warn( "getInstance(): init() has not been called.");
      return null;
      //throw new IllegalStateException( "HtmlWriter2.init() has not been called." );
    }
    return singleton;
  }

  /** @noinspection UNUSED_SYMBOL*/
  private HtmlWriter2() {}

  private HtmlWriter2( String contextPath, String contextName, String contextVersion,
                       String docsPath, String userCssPath,
                       String contextLogoPath, String instituteLogoPath )
  {
    this.contextPath = contextPath;
    this.contextName = contextName;
    this.contextVersion = contextVersion;
    this.docsPath = docsPath;
    this.userCssPath = userCssPath;
    this.contextLogoPath = contextLogoPath;
    this.instituteLogoPath = instituteLogoPath;
  }

  public String getContextPath() { return contextPath; }
  public String getContextName() { return contextName; }
  public String getContextVersion() { return contextVersion; }
  public String getContextLogoPath() { return contextLogoPath; }
  //public String getUserCssPath() { return userCssPath; }
  //public String getInstituteLogoPath() { return instituteLogoPath; }
  public String getDocsPath() { return docsPath; }

//  public static final String UNIDATA_CSS
  public String getUserCSS()
  {
    return new StringBuffer()
            .append( "<link rel='stylesheet' href='")
            .append( this.contextPath)
            .append( "/").append( userCssPath).append("' type='text/css' />\"").toString();
  }


//  public static final String UNIDATA_HEAD
  public String getUserHead()
  {
    return new StringBuffer()
            .append( "<table width=\"100%\">\n")
            .append( "    <tr>\n" )
            .append( "        <td width=\"95\" height=\"95\" align=\"left\"><img src=\"").append( contextPath).append("/").append( instituteLogoPath ).append("\" width=\"95\" height=\"93\"> </td>\n")
            .append( "        <td width=\"701\" align=\"left\" valign=\"top\">\n")
            .append( "            <table width=\"303\">\n" )
            .append( "                <tr>\n" )
            .append( "                  <td width=\"295\" height=\"22\" align=\"left\" valign=\"top\"><h3><strong>").append( contextName).append("</strong></h3></td>\n" )
            .append( "                </tr>\n" )
            .append( "            </table>\n" )
            .append( "        </td>\n" )
            .append( "    </tr>\n" )
            .append( "</table>")
            .toString();
  }


//  private static final String TOMCAT_CSS
  private String getTomcatCSS()
  {
    return new StringBuffer()
            .append( "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " )
            .append( "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " )
            .append( "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " )
            .append( "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " )
            .append( "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " )
            .append( "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" )
            .append( "A {color : black;}" )
            .append( "A.name {color : black;}" )
            .append( "HR {color : #525D76;}")
            .toString();
  }

  /**
   * Write a file directory.
   *
   * @param dir  directory
   * @param path the URL path reletive to the base
   */
  public void writeDirectory( HttpServletResponse res, File dir, String path )
          throws IOException
  {
    // error checking
    if ( dir == null )
    {
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      return;
    }

    if ( !dir.exists() || !dir.isDirectory() )
    {
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      return;
    }

    // Get directory as HTML
    String dirHtmlString = getDirectory( path, dir );

    res.setContentLength( dirHtmlString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // LOOK faster to use PrintStream instead of PrintWriter
    // Return an input stream to the underlying bytes
    // Prepare a writer
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( java.io.UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( dirHtmlString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, dirHtmlString.length() );
  }

  private String getDirectory( String path, File dir )
  {
    StringBuffer sb = new StringBuffer();

    // Render the page header
    sb.append( "<html>\r\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Directory listing for " ).append( path );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE>\r\n" );
    sb.append( "</head>\r\n" );
    sb.append( "<body>\r\n" );
    sb.append( "<h1>" );
    sb.append( "Directory listing for " ).append( path );

    // Render the link to our parent (if required)
    String parentDirectory = path;
    if ( parentDirectory.endsWith( "/" ) )
    {
      parentDirectory =
              parentDirectory.substring( 0, parentDirectory.length() - 1 );
    }
    int slash = parentDirectory.lastIndexOf( '/' );
    if ( slash >= 0 )
    {
      String parent = parentDirectory.substring( 0, slash );
      sb.append( " - <a href=\"" );
      if ( parent.equals( "" ) )
      {
        parent = "/";
      }
      sb.append( "../" ); // sb.append(encode(parent));
      //if (!parent.endsWith("/"))
      //  sb.append("/");
      sb.append( "\">" );
      sb.append( "<b>" );
      sb.append( "Up to " ).append( parent );
      sb.append( "</b>" );
      sb.append( "</a>" );
    }

    sb.append( "</h1>\r\n" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    // Render the column headings
    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Filename" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"center\"><font size=\"+1\"><strong>" );
    sb.append( "Size" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"right\"><font size=\"+1\"><strong>" );
    sb.append( "Last Modified" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Render the directory entries within this directory
    boolean shade = false;
    File[] children = dir.listFiles();
    List fileList = Arrays.asList( children );
    Collections.sort( fileList );
    for ( int i = 0; i < fileList.size(); i++ )
    {
      File child = (File) fileList.get( i );

      String childname = child.getName();
      if ( childname.equalsIgnoreCase( "WEB-INF" ) ||
           childname.equalsIgnoreCase( "META-INF" ) )
      {
        continue;
      }

      if ( child.isDirectory() ) childname = childname + "/";
      //if (!endsWithSlash) childname = path + "/" + childname; // client removes last path if no slash

      sb.append( "<tr" );
      if ( shade )
      {
        sb.append( " bgcolor=\"#eeeeee\"" );
      }
      sb.append( ">\r\n" );
      shade = !shade;

      sb.append( "<td align=\"left\">&nbsp;&nbsp;\r\n" );
      sb.append( "<a href=\"" );
      //sb.append( encode(contextPath));
      // resourceName = encode(path + resourceName);
      sb.append( childname );
      sb.append( "\"><tt>" );
      sb.append( childname );
      sb.append( "</tt></a></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );
      if ( child.isDirectory() )
      {
        sb.append( "&nbsp;" );
      }
      else
      {
        sb.append( renderSize( child.length() ) );
      }
      sb.append( "</tt></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );
      sb.append( formatter.toDateTimeString( new Date( child.lastModified() ) ) );
      sb.append( "</tt></td>\r\n" );

      sb.append( "</tr>\r\n" );
    }

    // Render the page footer
    sb.append( "</table>\r\n" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='").append(this.contextPath).append(this.docsPath).append("'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return sb.toString();
  }

  private String renderSize( long size )
  {

    long leftSide = size / 1024;
    long rightSide = ( size % 1024 ) / 103;   // Makes 1 digit
    if ( ( leftSide == 0 ) && ( rightSide == 0 ) && ( size > 0 ) )
    {
      rightSide = 1;
    }

    return ( "" + leftSide + "." + rightSide + " kb" );
  }

  public void writeCatalog( HttpServletResponse res, InvCatalogImpl cat, boolean isLocalCatalog )
          throws IOException
  {
    String catHtmlAsString = convertCatalogToHtml( cat, isLocalCatalog );

    res.setContentLength( catHtmlAsString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // Write it out
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( java.io.UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( catHtmlAsString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, catHtmlAsString.length() );
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param cat catalog to write
   */
  private String convertCatalogToHtml( InvCatalogImpl cat, boolean isLocalCatalog )
  {
    StringBuffer sb = new StringBuffer( 10000 );

    String catname = StringUtil.quoteHtmlContent( cat.getUriString() );

    // Render the page header
    sb.append( "<html>\r\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Catalog " ).append( catname );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE> " );
    sb.append( "</head>\r\n" );
    sb.append( "<body>" );
    sb.append( "<h1>" );
    sb.append( "Catalog " ).append( catname );
    sb.append( "</h1>" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    // Render the column headings
    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Dataset" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"center\"><font size=\"+1\"><strong>" );
    sb.append( "Size" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"right\"><font size=\"+1\"><strong>" );
    sb.append( "Last Modified" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Recursively render the datasets
    boolean shade = false;
    shade = doDatasets( cat, cat.getDatasets(), sb, shade, 0, isLocalCatalog );

    // Render the page footer
    sb.append( "</table>\r\n" );

    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='" ).append( contextPath ).append( "/").append(this.docsPath).append("'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return ( sb.toString() );
  }

  private boolean doDatasets( InvCatalogImpl cat, List datasets, StringBuffer sb, boolean shade, int level, boolean isLocalCatalog )
  {
    URI catURI = cat.getBaseURI();
    String catHtml;
    if ( !isLocalCatalog )
    {
      // Setup HREF url to link to HTML dataset page (more below).
      catHtml = contextPath + "/catalog.html?cmd=subset&catalog=" + cat.getUriString() + "&";
    }
    else
    { // replace xml with html
      catHtml = cat.getUriString();
      int pos = catHtml.lastIndexOf( '.' );
      if (pos < 0)
        catHtml = catHtml + "catalog.html?";
      else
        catHtml = catHtml.substring( 0, pos ) + ".html?";
    }

    for ( int i = 0; i < datasets.size(); i++ )
    {
      InvDatasetImpl ds = (InvDatasetImpl) datasets.get( i );
      String name = StringUtil.quoteHtmlContent( ds.getName() );

      sb.append( "<tr" );
      if ( shade )
      {
        sb.append( " bgcolor=\"#eeeeee\"" );
      }
      sb.append( ">\r\n" );
      shade = !shade;

      sb.append( "<td align=\"left\">" );
      for ( int j = 0; j <= level; j++ )
      {
        sb.append( "&nbsp;&nbsp;" );
      }
      sb.append( "\r\n" );

      if ( ds instanceof InvCatalogRef )
      {
        InvCatalogRef catref = (InvCatalogRef) ds;
        String href = catref.getXlinkHref();
        try {
          URI uri = new URI(href);
          if (uri.isAbsolute()) {
            href = contextPath + "/catalogServices?catalog=" + href;
          } else {
            int pos = href.lastIndexOf('.');
            href = href.substring(0, pos) + ".html";
          }

        } catch (URISyntaxException e) {
          log.error(href, e);
        }

        sb.append( "<a href=\"" );
        sb.append( StringUtil.quoteHtmlContent( href ) );
        sb.append( "\"><tt>" );
        sb.append( name );
        sb.append( "/</tt></a></td>\r\n" );
      }
      else if ( ds.getID() != null )
      {
        // Write link to HTML dataset page.
        sb.append( "<a href=\"" );
        // sb.append("catalog.html?cmd=subset&catalog=");
        sb.append( StringUtil.quoteHtmlContent( catHtml ) );
        sb.append( "dataset=" );
        sb.append( StringUtil.quoteHtmlContent( ds.getID() ) );
        sb.append( "\"><tt>" );
        sb.append( name );
        sb.append( "</tt></a></td>\r\n" );
      }
      else
      {
        sb.append( "<tt>" );
        sb.append( name );
        sb.append( "</tt></td>\r\n" );
      }

      sb.append( "<td align=\"right\"><tt>" );
      double size = ds.getDataSize();
      if ( ( size != 0.0 ) && !Double.isNaN( size ) )
      {
        sb.append( Format.formatByteSize( size ) );
      }
      else
      {
        sb.append( "&nbsp;" );
      }
      sb.append( "</tt></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );
      sb.append( formatter.toDateTimeString( new Date() ) );
      sb.append( "</tt></td>\r\n" );

      sb.append( "</tr>\r\n" );

      if ( !( ds instanceof InvCatalogRef ) )
      {
        shade = doDatasets( cat, ds.getDatasets(), sb, shade, level + 1, isLocalCatalog );
      }
    }

    return shade;
  }

  /**
   * Show CDM compliance (ccordinate systems, etc) of a NetcdfDataset.
   *
   * @param ds  dataset to write
   */
  public void showCDM( HttpServletResponse res , NetcdfDataset ds )
          throws IOException
  {
    String cdmAsString = getCDM( ds);

    res.setContentLength( cdmAsString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // Write it out
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( cdmAsString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, cdmAsString.length() );

  }

  private String getCDM( NetcdfDataset ds )
  {
    StringBuffer sb = new StringBuffer( 10000 );

    String name = StringUtil.quoteHtmlContent( ds.getLocation() );

    // Render the page header
    sb.append( "<html>\r\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Common Data Model" );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE> " );
    sb.append( "</head>\r\n" );
    sb.append( "<body>" );
    sb.append( "<h1>" );
    sb.append( "Dataset ").append( name );
    sb.append( "</h1>" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    //////// Axis
    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Axis" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Type" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Units" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Show the coordinate axes
    boolean shade = false;
    List axes = ds.getCoordinateAxes();
    for ( int i = 0; i < axes.size(); i++ )
    {
      CoordinateAxis axis = (CoordinateAxis) axes.get( i );
      showAxis( axis, sb, shade );
      shade = !shade;
    }

    ///////////// Grid
    GridDataset gds = new GridDataset( ds );

    // look for projections
    //List gridsets = gds.getGridSets();

    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "GeoGrid" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Description" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Units" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Show the grids
    shade = false;
    List grids = gds.getGrids();
    for ( int i = 0; i < grids.size(); i++ )
    {
      GridDatatype grid = (GridDatatype) grids.get( i );
      showGrid( grid, sb, shade );
      shade = !shade;
    }

    // Render the page footer
    sb.append( "</table>\r\n" );

    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='" ).append( contextPath ).append( "/" ).append( this.docsPath ).append( "'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return( sb.toString());
  }

  private void showAxis( CoordinateAxis axis, StringBuffer sb, boolean shade )
  {

    sb.append( "<tr" );
    if ( shade )
    {
      sb.append( " bgcolor=\"#eeeeee\"" );
    }
    sb.append( ">\r\n" );
    shade = !shade;

    sb.append( "<td align=\"left\">" );
    sb.append( "\r\n" );

    StringBuffer sbuff = new StringBuffer();
    axis.getNameAndDimensions( sbuff, false, true );
    String name = StringUtil.quoteHtmlContent( sbuff.toString() );
    sb.append( "&nbsp;" );
    sb.append( name );
    sb.append( "</tt></a></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    AxisType type = axis.getAxisType();
    String stype = ( type == null ) ? "" : StringUtil.quoteHtmlContent( type.toString() );
    sb.append( stype );
    sb.append( "</tt></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String units = axis.getUnitsString();
    String sunits = ( units == null ) ? "" : units;
    sb.append( sunits );
    sb.append( "</tt></td>\r\n" );

    sb.append( "</tr>\r\n" );
  }

  private void showGrid( GridDatatype grid, StringBuffer sb, boolean shade )
  {

    sb.append( "<tr" );
    if ( shade )
    {
      sb.append( " bgcolor=\"#eeeeee\"" );
    }
    sb.append( ">\r\n" );
    shade = !shade;

    sb.append( "<td align=\"left\">" );
    sb.append( "\r\n" );

    VariableEnhanced ve = grid.getVariable();
    StringBuffer sbuff = new StringBuffer();
    ve.getNameAndDimensions( sbuff, false, true );
    String name = StringUtil.quoteHtmlContent( sbuff.toString() );
    sb.append( "&nbsp;" );
    sb.append( name );
    sb.append( "</tt></a></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String desc = ve.getDescription();
    String sdesc = ( desc == null ) ? "" : StringUtil.quoteHtmlContent( desc );
    sb.append( sdesc );
    sb.append( "</tt></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String units = ve.getUnitsString();
    String sunits = ( units == null ) ? "" : units;
    sb.append( sunits );
    sb.append( "</tt></td>\r\n" );

    sb.append( "</tr>\r\n" );
  }

}
/*
 * $Log: HtmlWriter2.java,v $
 * Revision 1.6  2006/06/14 22:26:28  edavis
 * THREDDS Servlet Framework (TSF) changes:
 * 1) Allow developer to specify the logo files to be used by in HTML responses.
 * 2) Allow developer to specify the servlet path to be used for catalog requests.  
 * 3) Improve thread safety in DataRootHandler2.
 *
 * Revision 1.5  2006/04/20 22:25:23  caron
 * dods server: handle name escaping consistently
 * rename, reorganize servlets
 * update Paths doc
 *
 * Revision 1.4  2006/04/03 23:05:19  caron
 * add DLwriterServlet, StationObsCollectionServlet
 * rename various servlets, CatalogRootHandler -> DataRootHandler
 *
 * Revision 1.3  2006/03/30 23:22:11  edavis
 * Refactor THREDDS servlet framework, especially CatalogRootHandler and ServletUtil.
 *
 * Revision 1.2  2006/03/28 19:56:56  caron
 * remove DateUnit static methods - not thread safe
 * bugs in ForecasstModelRun interactions with external indexer
 *
 * Revision 1.1  2006/03/07 23:45:33  edavis
 * Remove hardwiring of "/thredds" as the context path in TDS framework.
 * Start refactoring URL mappings in TDS framework, use ExampleThreddsServlet as test servlet.
 *
 */