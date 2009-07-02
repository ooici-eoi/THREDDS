package thredds.catalog2.xml.parser;

import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.util.List;
import java.net.URI;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import ucar.nc2.units.DateType;

/**
 * Utility methods for generating catalog XML. 
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogXmlUtils
{
  private static final String catName = "thredds.catalog2.xml.parser.CatalogXmlUtils";
  private static final String catVersion = "1.0.2";

  private CatalogXmlUtils(){}

  public static String getCatalog() {
    return getCatalog( null );
  }

  public static String getCatalog( DateType expires ) {
    return wrapThreddsXmlInCatalog( "", expires );
  }

  public static String wrapThreddsXmlInCatalog( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" )
            .append( "         xmlns:xlink='http://www.w3.org/1999/xlink'\n" );
    if ( expires != null )
      sb.append( "         expires='" ).append( expires.toString() ).append( "'\n" );
    sb      .append( "         name='").append( catName).append( "'\n" )
            .append( "         version='").append( catVersion).append( "'>\n" )
            .append(    threddsXml )
            .append( "</catalog>" );

    return sb.toString();
  }

  public static void assertCatalogAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires )
  {
    assertEquals( catBuilder.getDocBaseUri(), docBaseUri);
    assertEquals( catBuilder.getName(), catName);
    if ( expires != null )
      assertEquals( catBuilder.getExpires().toString(), expires.toString());
  }

  public static String wrapThreddsXmlInCatalogWithService( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append(    threddsXml );

    return wrapThreddsXmlInCatalog(  sb.toString(), expires );
  }

  public static void assertCatalogWithServiceAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires)
  {
    assertCatalogAsExpected( catBuilder, docBaseUri, expires );

    List<ServiceBuilder> serviceBldrs = catBuilder.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty() );
    assertTrue( serviceBldrs.size() == 1 );
    ServiceBuilder serviceBldr = serviceBldrs.get( 0 );
    assertEquals( serviceBldr.getName(), "odap" );
    assertEquals( serviceBldr.getType(), ServiceType.OPENDAP );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/dodsC/" );
  }

  public static String wrapThreddsXmlInCatalogWithCompoundService( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <service name='all' serviceType='Compound' base=''>\n" )
            .append( "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/' />\n" )
            .append( "    <service name='wms' serviceType='WMS' base='/thredds/wms/' />\n" )
            .append( "  </service>\n" )
            .append(    threddsXml );

    return wrapThreddsXmlInCatalog(  sb.toString(), expires );
  }

  public static void assertCatalogWithCompoundServiceAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires )
  {
    assertCatalogAsExpected( catBuilder, docBaseUri, expires );

    List<ServiceBuilder> serviceBldrs = catBuilder.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty() );
    assertTrue( serviceBldrs.size() == 1 );
    ServiceBuilder serviceBldr = serviceBldrs.get( 0 );
    assertEquals( serviceBldr.getName(), "all" );
    assertEquals( serviceBldr.getType(), ServiceType.COMPOUND );
    assertEquals( serviceBldr.getBaseUri().toString(), "" );

    serviceBldrs = serviceBldr.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty());
    assertEquals( serviceBldrs.size(), 3 );

    serviceBldr = serviceBldrs.get( 0);
    assertEquals( serviceBldr.getName(), "odap" );
    assertEquals( serviceBldr.getType(), ServiceType.OPENDAP );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/dodsC/" );

    serviceBldr = serviceBldrs.get( 1);
    assertEquals( serviceBldr.getName(), "wcs" );
    assertEquals( serviceBldr.getType(), ServiceType.WCS );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/wcs/" );

    serviceBldr = serviceBldrs.get( 2);
    assertEquals( serviceBldr.getName(), "wms" );
    assertEquals( serviceBldr.getType(), ServiceType.WMS );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/wms/" );
  }

  public static String getCatalogWithSingleAccessDatasetWithRawServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <serviceName>odap</serviceName>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetWithMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <metadata>\n" )
            .append( "      <serviceName>odap</serviceName>\n" )
            .append( "    </metadata>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetWithInheritedMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <metadata inherited='true'>\n" )
            .append( "      <serviceName>odap</serviceName>\n" )
            .append( "    </metadata>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetOldStyle()
  {
    String sb = "<dataset name='Test1' urlPath='test/test1.nc' serviceName='odap' />\n";

    return wrapThreddsXmlInCatalogWithCompoundService( sb, null );
  }

  public static void assertCatalogHasSingleAccessDataset( CatalogBuilder catBuilder,
                                                                            URI docBaseUri )
  {
    assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );

    List<DatasetNodeBuilder> dsBuilders = catBuilder.getDatasetNodeBuilders();
    assertTrue( dsBuilders.size() == 1 );
    DatasetNodeBuilder dsnBuilder = dsBuilders.get( 0 );
    if ( !( dsnBuilder instanceof DatasetBuilder ) )
    {
      fail( "DatasetNode [" + dsnBuilder.getName() + "] not a Dataset." );
      return;
    }
    DatasetBuilder dsBldr = (DatasetBuilder) dsnBuilder;
    List<AccessBuilder> accesses = dsBldr.getAccessBuilders();
    assertFalse( "Dataset [" + dsBldr.getName() + "] not accessible.", accesses.isEmpty() );
    assertTrue( accesses.size() == 1 );
    AccessBuilder access = accesses.get( 0 );
    assertEquals( access.getUrlPath(), "test/test1.nc" );
    assertEquals( access.getServiceBuilder().getType(), ServiceType.OPENDAP );
    assertEquals( access.getServiceBuilder().getBaseUri().toString(), "/thredds/dodsC/" );
  }

  public static String wrapThreddsXmlInCatalogDataset( String threddsXml )
  {

    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='container dataset'>\n" )
            .append(      threddsXml )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalog( sb.toString(), null );
  }

  public static String wrapThreddsXmlInCatalogDatasetMetadata( String threddsXml )
  {
    return wrapThreddsXmlInCatalogDataset( "<metadata>" + threddsXml + "</metadata>" );
  }

  public static String wrapThreddsXmlInCatalogDatasetMetadataInherited( String threddsXml )
  {
    return wrapThreddsXmlInCatalogDataset( "<metadata inherited='true'>" + threddsXml + "</metadata>" );
  }

}
