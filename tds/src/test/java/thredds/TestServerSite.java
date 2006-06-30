// $Id: TestServerSite.java,v 1.6 2006/03/25 00:09:25 caron Exp $
package thredds;

import com.meterware.httpunit.*;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;

import javax.servlet.http.HttpServletResponse;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.security.cert.Certificate;

/**
 * A description
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestServerSite extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestServerSite.class );
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestServerSite.class );

  private WebConversation wc;

  /** The TDS site to test. */
  private String host = "motherlode.ucar.edu:9080";
  /** The name of a user with tdsConfig role. */
  private String tdsConfigUser;
  private String tdsConfigWord;

  private int catGenAdminColumnCount = 6;
  private int dqcAdminColumnCount = 3;

  private String targetUrlTomcat;
  private String targetUrl;

  private int maxCrawlDepth = 2;

  public TestServerSite( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    wc = new WebConversation();

    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host);
    tdsConfigUser = env.getProperty( "thredds.tds.config.user");
    tdsConfigWord = env.getProperty( "thredds.tds.config.password");

    targetUrlTomcat = "http://" + host + "/";
    targetUrl = "http://" + host + "/thredds/";

    java.net.Authenticator.setDefault( new Authenticator()
    {
      public PasswordAuthentication getPasswordAuthentication()
      {
        return new PasswordAuthentication( tdsConfigUser, tdsConfigWord.toCharArray() );
      }
    } );

  }

  /** Test that top Tomcat page is OK. */
  public void testServerSiteTomcat()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrlTomcat, curLog );
    assertTrue( curLog.toString(), resp != null );

    assertTrue( curLog.toString(), checkResponseCodeOk( resp, curLog ) );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /**
   * Crawl /thredds/catalog.xml tree.
   */
  public void testServerSiteTopCatalog()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "catalog.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! crawlCatalogTree( wc, resp, curLog, 0, maxCrawlDepth ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /** Crawl /thredds/catalog.html tree. */
  public void testServerSiteTop()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl, curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );
    success &= crawlHtmlTree( wc, resp, curLog, 0, maxCrawlDepth );
    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /** Crawl TDS Docs pages. */
  public void testServerSiteDocsTop()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "docs/", curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );
    success &= crawlHtmlTree( wc, resp, curLog, 0, maxCrawlDepth );
    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteValidateTopCatalog()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "catalog?cmd=validate", curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteSubset()
  {
    assertTrue( "Need to implement subset test", false );
  }

  public void testServerSiteConvert0_6To1_0()
  {
    assertTrue( "Need to implement 0.6 to 1.0 conversion test", false );
  }

  public void testServerSiteDebug()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false);
      return;
    }

    StringBuffer curLog = new StringBuffer();

    // Test with tdsConfig authentication
    //wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    String urlString = targetUrl + "debug";
    URL url = null;
    try
    {
      url = new URL( urlString );
    }
    catch ( MalformedURLException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - malformed URL: " ).append( e.getMessage());
      assertTrue( curLog.toString(), false );
      return;
    }
    HttpURLConnection con = null;
    try
    {
      con = (HttpURLConnection) url.openConnection();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException opening connection: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    con.setInstanceFollowRedirects( true );
    int respCode;
    try
    {
      respCode = con.getResponseCode();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException getting response code: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( urlString + " - Response code = " + respCode );
    String urlString2 = con.getHeaderField( "Location" );
    System.out.println( "location header: " + urlString2 );
    URL url2;
    try
    {
      url2 = new URL( urlString2 );
    }
    catch ( MalformedURLException e )
    {
      curLog.append( "\n" ).append( urlString2 ).append( " - malformed URL: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    HttpURLConnection con2 = null;
    try
    {
      con2 = (HttpURLConnection) url2.openConnection();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException opening connection: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( "Check if HttpsURLConnection ..." );
    if ( con2 instanceof HttpsURLConnection )
    {
      System.out.println( "... is HttpsURLConnection" );
      HttpsURLConnection scon = (HttpsURLConnection) con2;
      Certificate[] certs = new Certificate[0];
      certs = scon.getLocalCertificates();
      for ( int i = 0; i < certs.length; i++ )
      {
        System.out.println( "Local Cert[" + i + "]: " + certs[i].toString() );
      }
      try
      {
        certs = scon.getServerCertificates();
      }
      catch ( SSLPeerUnverifiedException e )
      {
        curLog.append( "\n" ).append( urlString ).append( " - SSLPeerUnverifiedException getting certificates: " ).append( e.getMessage() );
        assertTrue( curLog.toString(), false );
        return;
      }

      for ( int i = 0; i < certs.length; i++ )
      {
        System.out.println( "Server Cert[" + i + "]: " + certs[i].toString() );
      }
    }
    else
      System.out.println( "... not HttpsURLConnection" );

    int respCode2;
    try
    {
      respCode2 = con2.getResponseCode();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString2 ).append( " - IOException getting response code: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( urlString + " - Response code = " + respCode2 );


    WebResponse resp = getResponseToAGetRequest( wc, targetUrlTomcat + "dqcServlet/redirect-test/302" , curLog );
    //WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "debug", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "THREDDS Debug", curLog );

    success &= checkLinkExistence( resp, "Show Logs", curLog );
    success &= checkLinkExistence( resp, "Show Build Version", curLog );
    //success &= checkLinkExistence( resp, "Reinitialize", curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteRoot()
  {
    assertTrue( "Need to implement /thredds/root/ test", false );
  }

  public void testServerSiteContent()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    // Test with tdsConfig authentication
    wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "content/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "Directory listing for /content/", curLog );

    success &= checkLinkExistence( resp, "catalog.xml", curLog );
    success &= checkLinkExistence( resp, "cataloggen/", curLog );
    success &= checkLinkExistence( resp, "dodsC/", curLog );
    success &= checkLinkExistence( resp, "dqcServlet/", curLog );
    success &= checkLinkExistence( resp, "extraCatalogs.txt", curLog );
    success &= checkLinkExistence( resp, "logs/", curLog );
    success &= checkLinkExistence( resp, "root/", curLog );
    success &= checkLinkExistence( resp, "wcs/", curLog );
    success &= checkLinkExistence( resp, "junk/", curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteCatGen()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    // Test with tdsConfig authentication
    wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "cataloggen/admin/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "Catalog Generator Servlet Config", curLog );
    WebTable[] tables;
    try
    {
      tables = resp.getTables();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( respUrlString).append( " - failed to parse: " ).append( e.getMessage());
      assertTrue( curLog.toString(),
                  false);
      return;
    }

    success &= checkTableCellText( resp, tables[0], 0, 0, "Task Name", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 1, "Configuration Document", curLog );

    int columnCount = tables[0].getColumnCount();
    if ( columnCount != catGenAdminColumnCount )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " # columns <" ).append( columnCount ).append( "> not as expected <" ).append( catGenAdminColumnCount ).append( ">" );
      success = false;
      return;
    }

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteDqcServletHtml()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "dqc/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "DQC Servlet - Available Datasets", curLog );

    WebTable[] tables;
    try
    {
      tables = resp.getTables();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - failed to parse: " ).append( e.getMessage() );
      assertTrue( curLog.toString(),
                  false );
      return;
    }

    success &= checkTableCellText( resp, tables[0], 0, 0, "Name", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 1, "Description", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 2, "DQC Document", curLog );

    int columnCount = tables[0].getColumnCount();
    if ( columnCount != dqcAdminColumnCount )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " # columns <" ).append( columnCount ).append( "> not as expected <" ).append( dqcAdminColumnCount ).append( ">" );
      success = false;
    }

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteDqcServletXml()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "dqc/catalog.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! checkInvCatalog( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  protected static boolean checkInvCatalog( WebResponse resp, StringBuffer curLog )
  {
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      return false;
    }

    if ( ! resp.getContentType().equals( "text/xml" ) )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - not XML." );
      return false;
    }

    // Get DOM for this response.
    Document catDoc;
    try
    {
      catDoc = resp.getDOM();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - parsing error: " ).append( e.getMessage() );
      return false;
    }

    return checkInvCatalog( catDoc, respUrlString, curLog );
  }

  protected static boolean checkInvCatalog( Document catDoc, String respUrlString, StringBuffer curLog )
  {
    // Get InvCatalogImp of this response.
    InvCatalogImpl cat = null;
    org.jdom.input.DOMBuilder builder = new org.jdom.input.DOMBuilder();
    try
    {
      cat = InvCatalogFactory.getDefaultFactory( false ).readXML( builder.build( catDoc), new URI( respUrlString ) );
    }
    catch ( URISyntaxException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - bad URI syntax: " ).append( e.getMessage() );
      return false;
    }

    // Check validity of catalog.
    if ( ! cat.check( curLog ) )
    {
      curLog.append( "\n").append( respUrlString ).append( " - not a valid catalog." );
      return false;
    }
    return true;
  }

  protected static boolean crawlCatalogTree( WebConversation wc, WebResponse resp, StringBuffer curLog, int curCrawlDepth, int maxCrawlDepth )
  {
    if ( curCrawlDepth + 1 > maxCrawlDepth ) return true;
    curCrawlDepth++;

    String respUrlString = resp.getURL().toString();

    // Check that given response OK.
    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      return false;
    }

    // Check that response content type is XML.
    if ( ! resp.getContentType().equals( "text/xml") )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - not XML." );
      return false;
    }

    // Parse the response and get DOM.
    Document doc;
    try
    {
      doc = resp.getDOM();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( respUrlString).append( " - could not parse:").append( e.getMessage() );
      return false;
    }

    // Check that this is a valid catalog.
    if ( ! checkInvCatalog( doc, respUrlString, curLog ) )
    {
      return false;
    }

    boolean success = true;
    NodeList catRefNodeList = doc.getElementsByTagName( "catalogRef");
    Node curNode;
    for ( int i = 0; i < catRefNodeList.getLength(); i++ )
    {
      curNode = catRefNodeList.item( i);
//      String curLink = curNode.getAttributes().getNamedItemNS( "http://www.w3.org/1999/xlink", "href").getNodeValue();
      String curLink = curNode.getAttributes().getNamedItem( "xlink:href").getNodeValue();
      try
      {
        curLink = new URL( resp.getURL(), curLink).toString();
      }
      catch ( MalformedURLException e )
      {
        curLog.append( "\n" ).append( curLink ).append( " - malformed URL:" ).append( e.getMessage() );
        success = false;
        continue;
      }

      // If the current link is under the current catalog URL,
      // continue crawling.
      if ( curLink.startsWith( respUrlString ))
      {
        WebResponse curResp = getResponseToAGetRequest( wc, curLink, curLog );
        if ( curResp == null )
        {
          success = false;
          continue;
        }
        success &= crawlCatalogTree( wc, curResp, curLog, curCrawlDepth, maxCrawlDepth );
      }
    }

    return( success );
  }

  protected static boolean crawlHtmlTree( WebConversation wc, WebResponse resp, StringBuffer curLog, int curCrawlDepth, int maxCrawlDepth )
  {
    if ( curCrawlDepth + 1 > maxCrawlDepth ) return true;
    curCrawlDepth++;

    String respUrlString = resp.getURL().toString();
    if ( ! resp.isHTML() && ! resp.getContentType().equals( "text/xml" ) )
    {
      curLog.append( "\n").append( respUrlString).append( " - not HTML or XML." );
      return( false );
    }

    // Get all links on this page.
    WebLink[] links;
    try
    {
      links = resp.getLinks();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( respUrlString).append( " - parsing error : ").append( e.getMessage() );
      return ( false );
    }

    boolean success = true;

    for ( int i = 0; i < links.length; i++ )
    {
      String curLinkUrlString;
      WebLink curLink = links[i];
      try
      {
        curLinkUrlString =  curLink.getRequest().getURL().toString();
      }
      catch ( MalformedURLException e )
      {
        curLog.append( "\n").append( respUrlString).append( " - malformed current link URL <").append( curLink.getURLString()).append( ">: ").append( e.getMessage() );
        success = false;
        continue;
      }

      if ( curLinkUrlString.startsWith( respUrlString))
      {
        WebResponse curResp = getResponseToAGetRequest( wc, curLinkUrlString, curLog);
        if ( curResp == null )
        {
          success = false;
          continue;
        }

        success &= checkResponseCodeOk( curResp, curLog );
        if ( curResp.isHTML() )
        {
          success &= crawlHtmlTree( wc, curResp, curLog, curCrawlDepth, maxCrawlDepth );
        }
      }
    }
    return( success);
  }

  protected static boolean checkResponseCodeOk( WebResponse resp, StringBuffer log )
  {
    String respUrlString = resp.getURL().toString();
    int respCode = resp.getResponseCode();
    if ( respCode != HttpServletResponse.SC_OK )
    {
      log.append( "\n").append( respUrlString).append( " - response code <").append( respCode).append( "> not as expected <OK - 200>");
      return false;
    }
    return true;
  }

  protected static WebResponse getResponseToAGetRequest( WebConversation wc, String reqUrl, StringBuffer curLog )
  {
    WebRequest req = new GetMethodWebRequest( reqUrl );
    WebResponse resp;
    try
    {
      resp = wc.getResponse( req );
    }
    catch ( IOException e )
    {
      curLog.append( "\n").append( reqUrl).append( " - failed to get response: ").append( e.getMessage() );
      return null;
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( reqUrl).append( " - failed to parse response: ").append( e.getMessage() );
      return null;
    }
    catch ( com.meterware.httpunit.HttpException e )
    {
      curLog.append( "\n" ).append( reqUrl ).append( " - HTTP error: " ).append( e.getMessage() );
      return null;
    }
    return resp;
  }

  protected static boolean checkTitle( WebResponse resp, String title, StringBuffer curLog )
  {
    String respUrlString = resp.getURL().toString();
    String pageTitle = null;
    try
    {
      pageTitle = resp.getTitle();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString )
              .append( " - parse error reading page title: " )
              .append( e.getMessage() );
      return false;
    }

    if ( ! pageTitle.equals( title ) )
    {
      curLog.append( "\n" ).append( respUrlString )
              .append( " - title <" ).append( pageTitle )
              .append( "> not as expected <" ).append( title )
              .append( ">." );
      return false;
    }
    return true;
  }

  protected static boolean checkLinkExistence( WebResponse resp, String linkText, StringBuffer curLog )
  {
    String respUrlString = resp.getURL().toString();
    WebLink link = null;
    try
    {
      link = resp.getLinkWith( linkText );
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - parse error checking for link <\"" ).append( linkText ).append( "\">: " ).append( e.getMessage() );
      return false;
    }
    if ( link == null )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - did not find link <\"" ).append( linkText ).append( "\">: " );
      return false;
    }
    return true;
  }

  protected boolean checkTableCellText( WebResponse resp, WebTable table, int headerRow, int headerCol, String headerText, StringBuffer curLog )
  {
    String respUrlString = resp.getURL().toString();
    String headerCellAsText = table.getCellAsText( headerRow, headerCol );
    if ( ! headerCellAsText.equals( headerText ) )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - table header in column 0 <" ).append( headerCellAsText ).append( "> not as expected <" ).append( headerText ).append( ">." );
      return false;
    }
    return true;
  }

}

/*
 * $Log: TestServerSite.java,v $
 * Revision 1.6  2006/03/25 00:09:25  caron
 * Forecast Model Run
 * Grib Sync()
 *
 * Revision 1.5  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.4  2006/01/17 20:58:51  edavis
 * Several small fixes to read/write of datasetScan element. A few documentation updates.
 *
 * Revision 1.3  2005/10/26 23:19:33  edavis
 * Updated TDS site tests.
 *
 * Revision 1.2  2005/08/22 19:39:13  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.1  2005/08/04 22:54:50  edavis
 * Rename TestMotherlode to TestServerSite and centralize modifications
 * needed to test other sites (though still not configurable).
 *
 * Revision 1.1  2005/07/27 17:18:38  edavis
 * Added some basic HttpUnit testing of motherlode:8088 server.
 *
 *
 */