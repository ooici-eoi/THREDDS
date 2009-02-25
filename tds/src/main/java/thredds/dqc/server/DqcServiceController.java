package thredds.dqc.server;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.server.views.InvCatalogXmlView;
import thredds.servlet.UsageLog;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.HtmlWriter;
import thredds.catalog.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DqcServiceController extends AbstractController
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;

  private boolean allow = false;

  private String dqcConfigDirName;
  private String dqcConfigFileName;
  private File dqcConfigDir;
  private File dqcConfigFile;

  private DqcServletConfig dqcConfig;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  public void setDqcConfigDirName( String dqcConfigDirName )
  {
    this.dqcConfigDirName = dqcConfigDirName;
  }

  public void setDqcConfigFileName( String dqcConfigFileName )
  {
    this.dqcConfigFileName = dqcConfigFileName;
  }

  public void init()
  {
    log.info( "init(): " + UsageLog.setupNonRequestContext() );

    // Make sure DqcService is enabled.
    this.allow = ThreddsConfig.getBoolean( "DqcService.allow", false );
    if ( ! this.allow )
    {
      log.info( "init(): DqcServlet not enabled in threddsConfig.xml." );
      log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    // Check that have TdsContext.
    if ( this.tdsContext == null )
    {
      this.allow = false;
      log.error( "init(): Disabling DqcService - null TdsContext." );
      return;
    }

    // Locate or create DqcConfig directory.
    this.dqcConfigDir = this.tdsContext.getConfigFileSource().getFile( this.dqcConfigDirName );
    if ( this.dqcConfigDir == null )
    {
      this.dqcConfigDir = createDqcConfigDirectory();
      if ( this.dqcConfigDir == null )
      {
        this.allow = false;
        log.error( "init(): Disabling DqcService." );
        log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
        return;
      }
    }

    // Locate or create DqcConfig document.
    this.dqcConfigFile = new File( this.dqcConfigFileName );
    if ( ! this.dqcConfigFile.exists() )
    {
      this.dqcConfigFile = createDqcConfigFile();
      if ( this.dqcConfigFile == null )
      {
        this.allow = false;
        log.error( "init(): Disabling DqcService." );
        log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
        return;
      }
    }

    log.debug( "init(): DqcService config directory = " + this.dqcConfigDir.toString());
    log.debug( "init(): DqcService config file      = " + this.dqcConfigFile.toString());

    // Read DqcConfig.
    try
    {
      this.dqcConfig = new DqcServletConfig( this.dqcConfigDir, this.dqcConfigFileName );
    }
    catch ( Throwable t )
    {
      this.allow = false;
      log.error( "init(): Disabling DqcService - failed to read DqcConfig document: " + t.getMessage() );
      log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    log.info( "destroy(): " + UsageLog.setupNonRequestContext() );

    // Shutdown all scheduled events

    // clean up anything else needed

//    if ( this.scheduler != null )
//      this.scheduler.stop();
    log.info( "destroy()" + UsageLog.closingMessageNonRequestContext() );
  }


  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    // Gather diagnostics for logging request.
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );

    if ( ! this.allow )
    {
      String msg = "DQC service not supported.";
      log.info( "handleRequestInternal(): " + msg );
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ) );
      response.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return null;
    }

    // Handle requests.
    // Get the request path information.
    String reqPath = request.getPathInfo();
    if ( reqPath.equals( "/" ) )
    {
      List<DqcServletConfigItem> items = new ArrayList<DqcServletConfigItem>();
      Iterator itemIterator = this.dqcConfig.getIterator();
      while ( itemIterator.hasNext() )
        items.add( (DqcServletConfigItem) itemIterator.next() );
      Map<String,Object> model = new HashMap<String,Object>();
      model.put( "contextPath", request.getContextPath() );
      model.put( "servletPath", request.getServletPath() );
      model.put( "dqcConfigItems", items );
      return new ModelAndView( "/thredds/server/dqc/dqcConfig", model);
    }
    else if ( reqPath.equals( "/catalog.xml" ))
    {
      InvCatalog catalog = this.createCatalogRepresentation( request.getContextPath(), request.getServletPath() );
      return new ModelAndView( new InvCatalogXmlView(), "catalog", catalog );
    }
    else if ( reqPath.equals( "/catalog.html" ) )
    {
      InvCatalog catalog = this.createCatalogRepresentation( request.getContextPath(), request.getServletPath() );
      int i = HtmlWriter.getInstance().writeCatalog( response, (InvCatalogImpl) catalog, true );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );

      return null;
    }
    
    return null;
  }

  public InvCatalog createCatalogRepresentation( String contextPath, String servletPath )
  {
    String serviceName = "myDqcServlet";

    // Create the catalog, service, and top-level dataset.
    InvCatalogImpl catalog = new InvCatalogImpl( null, null, null );
    InvService myService = new InvService( serviceName, ServiceType.RESOLVER.toString(), contextPath + servletPath, null, null );
    InvDatasetImpl topDs = new InvDatasetImpl( null, "DqcServlet Available Datasets" );
    // OR ( null, this.mainConfig.getDqcServletTitle() );
    // Add service and top-level dataset to the catalog.
    catalog.addService( myService );
    catalog.addDataset( topDs );

    // Add a dataset to the catalog for each dataset handled by this server.
    DqcServletConfigItem curItem = null;
    Iterator it = this.dqcConfig.getIterator();
    InvDatasetImpl curDs = null;
    InvDocumentation curDoc = null;
    StringBuffer docContent = null;
    while ( it.hasNext() )
    {
      curItem = (DqcServletConfigItem) it.next();
      curDs = new InvDatasetImpl( topDs, curItem.getDescription(), null, serviceName, curItem.getName() + ".xml" );
      docContent = new StringBuffer();
      docContent.append( curItem.getDescription() + "\n" )
              .append( "Using the DqcHandler " + curItem.getHandlerClassName() )
              .append( " with config file " + curItem.getHandlerConfigFileName() + "." );
      curDoc = new InvDocumentation( null, null, null, null, docContent.toString() );
      curDs.addDocumentation( curDoc );
      topDs.addDataset( curDs );
    }

    // Tie up any loose ends in catalog with finish().
    ( (InvCatalogImpl) catalog ).finish();

    return ( catalog );
  }

  /**
   * Return newly created DqcConfig directory. If it already exists, return null.
   *
   * <p>This method should only be called if DqcConfig directory was not found
   * in TdsContext ConfigFileSource. I.e., the following
   * <code>this.tdsContext.getConfigFileSource().getFile( this.dqcConfigDirName )</code>
   * returned null.
   *
   * @return the DqcConfig directory
   */
  private File createDqcConfigDirectory()
  {
    File dqcConfigDir = new File( this.tdsContext.getContentDirectory(), this.dqcConfigDirName);
    if ( dqcConfigDir.exists() )
    {
      log.error( "createDqcConfigDirectory(): Existing DqcConfigDir [" + dqcConfigDir + "] not found in TdsContext ConfigFileSource, check TdsContext config." );
      return null;
    }

    if ( ! this.dqcConfigDir.mkdirs() )
    {
      log.error( "createDqcConfigDirectory(): Failed to create DqcConfig directory." );
      return null;
    }

    if ( ! dqcConfigDir.equals( this.tdsContext.getConfigFileSource().getFile( this.dqcConfigDirName ) ) )
    {
      log.error( "createDqcConfigDirectory(): Newly created DqcConfig directory not found by TdsContext ConfigFileSource." );
      return null;
    }
    return dqcConfigDir;
  }

  /**
   * Return newly created DqcConfig File or null if failed to create new file.
   * If it already exists, return null.
   *
   * @return the DqcConfig File.
   */
  private File createDqcConfigFile()
  {
    File dqcConfigFile = new File( this.dqcConfigDir, this.dqcConfigFileName );
    if ( dqcConfigFile.exists() )
    {
      log.error( "createDqcConfigFile(): Existing DqcConfigFile [" + dqcConfigFile + "] not found in TdsContext ConfigFileSource, check TdsContext config." );
      return null;
    }

    boolean created = false;
    try
    {
      created = dqcConfigFile.createNewFile();
    }
    catch ( IOException e )
    {
      log.error( "createDqcConfigFile(): I/O error while creating DqcConfig file." );
      return null;
    }
    if ( ! created )
    {
      log.error( "createDqcConfigFile(): Failed to create DqcConfig file." );
      return null;
    }

    // Write blank config file. Yuck!!!
    if ( ! this.writeEmptyConfigDocToFile( dqcConfigFile ) )
    {
      log.error( "createDqcConfigFile(): Failed to write empty config file [" + dqcConfigFile + "]." );
      return null;
    }
    return dqcConfigFile;
  }

  private boolean writeEmptyConfigDocToFile( File configFile )
  {
    FileOutputStream fos = null;
    OutputStreamWriter writer = null;
    try
    {
      fos = new FileOutputStream( configFile );
      writer = new OutputStreamWriter( fos, "UTF-8" );
      writer.append( this.genEmptyConfigDocAsString() );
      writer.flush();
    }
    catch ( IOException e )
    {
      log.debug( "writeEmptyConfigDocToFile(): IO error writing blank config file: " + e.getMessage() );
      return false;
    }
    finally
    {
      try
      {
        if ( writer != null )
          writer.close();
        if ( fos != null )
          fos.close();
      }
      catch ( IOException e )
      {
        log.debug( "writeEmptyConfigDocToFile(): IO error closing just written blank config file: " + e.getMessage() );
        return true;
      }
    }
    return true;
  }

  private String genEmptyConfigDocAsString()
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<preferences EXTERNAL_XML_VERSION='1.0'>\n" )
            .append( "  <root type='user'>\n" )
            .append( "    <map>\n" )
            .append( "      <beanCollection key='config' class='thredds.cataloggen.servlet.CatGenTimerTask'>\n" )
            .append( "      </beanCollection>\n" )
            .append( "    </map>\n" )
            .append( "  </root>\n" )
            .append( "</preferences>" );
    return sb.toString();
  }
}
