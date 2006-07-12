// $Id$

package thredds.cataloggen;

import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.cataloggen.config.CatGenConfigMetadataFactory;
import thredds.cataloggen.config.CatalogGenConfig;
import thredds.cataloggen.config.DatasetSource;
import thredds.datatype.DateType;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//import org.apache.log4j.*;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * CatalogGen crawls dataset sources given in a CatalogGenConfig file
 * to produce THREDDS catalogs.
 *
 * To generate a catalog from a config file:
 * <pre>
 *   String inFileName = "file:/home/edavis/testCatGenConfig.xml";
 *   String outFileName = "/home/edavis/testCatGenConfig-results.xml";
 *   StringBuffer log = new StringBuffer();
 *   CatalogGen catGen = new CatalogGen( inFileName);
 *   if ( catGen.isValid( log))
 *   {
 *     catGen.expand();
 *     catGen.writeCatalog( outFileName);
 *   }
 * </pre>
 *
 *
 * @author Ethan Davis
 * @version $Ver$
 */
public class CatalogGen
{
  //private static Log log = LogFactory.getLog( CatalogGen.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogGen.class);

  /** The catalog: initially as a CatGen config file, until expanded. */
  private InvCatalog catalog = null;

  /** The catalog factory that knows about CatalogGenConfig metadata. */
  protected InvCatalogFactory catFactory = null;

  public List getCatalogRefInfoList()
  {
    return catalogRefInfoList;
  }

  private List catalogRefInfoList = new ArrayList();


  /**
   * Constructs the CatalogGen for the given config document.
   *
   * @param configDocURL - the URL of the configuration document
   */
  public CatalogGen( URL configDocURL)
  {
    // Create a InvCatalogFactory with CATALOG_GEN_CONFIG MetadataType registered.
    log.debug( "CatalogGen(URL): create catalog and CatalogGenConfig converter." );
    this.catFactory = InvCatalogFactory.getDefaultFactory( true );
    this.catFactory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
                                             new CatGenConfigMetadataFactory());

    // Read the given XML config file.
    log.debug( "CatalogGen(URL): reading the config doc <" + configDocURL.toString() + ">.");
    this.catalog = this.catFactory.readXML( configDocURL.toString());
    log.debug( "CatalogGen(URL): done.");
  }

  /**
   * Constructs the CatalogGen for the given config document InputStream.
   *
   * @param configDocInputStream - the InputStream from which to read the config document.
   * @param configDocURL - the URL for the config document.
   */
  public CatalogGen( InputStream configDocInputStream, URL configDocURL )
  {
    // Create a InvCatalogFactory with CATALOG_GEN_CONFIG MetadataType registered.
    log.debug( "CatalogGen(InputStream): create catalog and CatalogGenConfig converter." );
    this.catFactory = new InvCatalogFactory( "default", true );
    this.catFactory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
                                               new CatGenConfigMetadataFactory() );

    // Read the given XML config file.
    log.debug( "CatalogGen(InputStream): reading the config doc <" + configDocURL.toString() + ">." );
    this.catalog = this.catFactory.readXML( configDocInputStream, URI.create( configDocURL.toExternalForm()) );
    log.debug( "CatalogGen(InputStream): CatalogGenConfig doc <" + this.catalog.getName() + "> read.");

  }

  /**
   * Checks the validity of the configuration file.
   * @param out - a StringBuffer with validity error and warning messages.
   * @return - true if no errors, false if errors exist
   */
  public boolean isValid( StringBuffer out)
  {
    log.debug( "isValid(): start");
    return( this.catalog.check( out));
  }

  /**
   * Expand the catalog. Each of the CatalogGenConfig metadata elements
   * is expanded into its constituent datasets.
   */
  public InvCatalog expand()
  {
    CatalogGenConfig tmpCgc = null;
    List cgcList = null;
    DatasetSource dss = null;

    // Find and loop through each CatGenConfigMetadata object.
    List mdataList = findCatGenConfigMdata( this.catalog.getDatasets());
    for ( int i = 0; i < mdataList.size(); i++)
    {
      InvMetadata curMdata = (InvMetadata) mdataList.get( i);
      InvDatasetImpl curParentDataset = ( (InvDatasetImpl) curMdata.getParentDataset());

      // Loop through the CatalogGenConfig objects in current InvMetadata.
      cgcList = (List) curMdata.getContentObject();
      for ( int j = 0; j < cgcList.size(); j++)
      {
        tmpCgc = (CatalogGenConfig) cgcList.get( j);
        log.debug( "expand(): mdata # " + i + " and catGenConfig # " + j + "." );
        dss = tmpCgc.getDatasetSource();
        InvCatalog generatedCat = null;
        try
        {
          generatedCat = dss.fullExpand();
        }
        catch ( IOException e )
        {
          String tmpMsg = "Error: IOException on fullExpand() of DatasetSource <" + dss.getName() + ">: " + e.getMessage();
          log.error( "expand(): " + tmpMsg);
          curParentDataset.addDataset( new InvDatasetImpl( curParentDataset, tmpMsg));
          break;
        }

        catalogRefInfoList.addAll( dss.getCatalogRefInfoList());

        // Always a single top-level dataset in catalog returned by DatasetSource.fullExpand()
        InvDataset genTopDs = (InvDataset) generatedCat.getDatasets().get( 0);

        // Add all services in the generated catalog to the parent catalog.
        for( Iterator it = generatedCat.getServices().iterator(); it.hasNext(); )
        {
          ( (InvCatalogImpl) curParentDataset.getParentCatalog() ).addService( (InvService) it.next() );
        }

        // Add the generated catalog to the parent datasets, i.e., add all the
        // datasets that are children of the generated catalogs' top-level dataset.
        for ( Iterator it = genTopDs.getDatasets().iterator(); it.hasNext(); )
        {
          InvDatasetImpl curGenDataset = (InvDatasetImpl) it.next();
          if ( curGenDataset.hasNestedDatasets())
          {
            // If the dataset has children datasets, add serviceName and make it inherited.
            ThreddsMetadata tm = new ThreddsMetadata( false);
            tm.setServiceName( genTopDs.getServiceDefault().getName());
            InvMetadata md = new InvMetadata( genTopDs, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm);
            curGenDataset.getLocalMetadata().addMetadata( md);
          }
          else
          {
            // Otherwise, add serviceName not inherited.
            curGenDataset.getLocalMetadata().setServiceName(  genTopDs.getServiceDefault().getName());
          }
          curParentDataset.addDataset( curGenDataset);
        }

        curParentDataset.finish();
      }
//      log.debug( "expand(): List datasets that are siblinks of current metadata record CGCM(" + i + ").");
//      List list = curMdata.getParentDataset().getDatasets();
//      Iterator it = list.iterator();
//      while ( it.hasNext())
//      {
//        InvDatasetImpl curDs = (InvDatasetImpl) it.next();
//        log.debug( "Dataset URL is " + curDs.getUrlPath() + ".");
//        log.debug( "Dataset name is " + curDs.getName() + ".");
//      }

      // Remove the current metadata element from its parent dataset.
      log.debug( "expand(): Remove metadata record CGCM(" + i + ")." );
      curParentDataset.removeLocalMetadata( curMdata);
      // *****
    }

    // Finish this catalog now that done building it.
    ((InvCatalogImpl) this.catalog).finish();

    return( this.catalog);
  }

  public void setCatalogExpiresDate( DateType expiresDate )
  {
    ((InvCatalogImpl) this.catalog).setExpires( expiresDate);
  }

  /**
   * Writes the catalog as XML. The catalog is written to the file given
   * in <tt>outFileName</tt>. If <tt>outFileName</tt> is null, the catalog
   * is written to standard out.
   *
   * @param outFileName - the pathname of the output file.
   */
  public boolean writeCatalog( String outFileName)
  {
    log.debug( "writeCatalog(): writing catalog to " + outFileName + ".");

    String invCatDTD = "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd";
    log.debug( "writeCatalog(): set the catalogs DTD (" + invCatDTD + ").");
    // Set the catalogs DTD.
    ( (InvCatalogImpl) catalog).setDTDid( invCatDTD);

    // Print the catalog as an XML document.
    if ( outFileName == null)
    {
      try
      {
        log.debug( "writeCatalog(): write catalog to System.out.");
        this.catFactory.writeXML( (InvCatalogImpl) catalog, System.out);
      }
      catch ( java.io.IOException e)
      {
        log.debug( "writeCatalog(): exception when writing to stdout.\n" +
                e.toString());
        //e.printStackTrace();
        return( false);
      }
      return( true);
    }
    else
    {
      log.debug( "writeCatalog(): try writing catalog to the output file (" + outFileName + ").");
      try
      {

        if ( ! this.catalog.getVersion().equals( "1.0" ) )
        {
          this.catFactory.writeXML( (InvCatalogImpl) catalog, outFileName );
        }
        else
        {
          // Override default output catalog version. (kludge for IDV backward compatibility)
          InvCatalogFactory10 fac10 = (InvCatalogFactory10) this.catFactory.getCatalogConverter( XMLEntityResolver.CATALOG_NAMESPACE_10 );
          fac10.setVersion( this.catalog.getVersion() );
          BufferedOutputStream osCat = new BufferedOutputStream( new FileOutputStream( outFileName ) );
          fac10.writeXML( (InvCatalogImpl) catalog, osCat );
          osCat.close();
        }



      }
      catch ( IOException e )
      {
        log.debug( "writeCatalog(): IOException, catalog not written to " + outFileName + ": " + e.getMessage() );
        return ( false );
      }
      log.debug( "writeCatalog(): catalog written to " + outFileName + "." );
      return ( true );
    }
  }

  InvCatalog getCatalog() { return( this.catalog ); }

  private List findCatGenConfigMdata( List datasets)
  {
    List mdataList = new ArrayList();
    if ( datasets == null) return( mdataList );

    // Iterate through list of datasets.
    Iterator it = datasets.iterator();
    InvDataset curDataset = null;
    while ( it.hasNext() )
    {
      curDataset = (InvDataset) it.next();

      // Get all the local metadata for the given dataset.
      ThreddsMetadata tm = ((InvDatasetImpl) curDataset).getLocalMetadata();

      // Iterate over the local InvMetadata checking for CatalogGenConfig metadata.
      Iterator itMdata = tm.getMetadata().iterator();
      InvMetadata curMetadata = null;
      while ( itMdata.hasNext())
      {
        curMetadata = (InvMetadata) itMdata.next();
        if ( (curMetadata.getMetadataType() != null) &&
             curMetadata.getMetadataType().equals( MetadataType.CATALOG_GEN_CONFIG.toString() ) )
        {
          mdataList.add( curMetadata );
        }
        else if ( (curMetadata.getNamespaceURI() != null) &&
                  curMetadata.getNamespaceURI().equals( CatalogGenConfig.CATALOG_GEN_CONFIG_NAMESPACE_URI_0_5 ))
        {
          mdataList.add( curMetadata );
        }
      }

      // Recurse through nested datasets and find CatalogGenConfig metadata.
      mdataList.addAll( this.findCatGenConfigMdata( curDataset.getDatasets() ) );
    }

    return( mdataList);
  }
}
