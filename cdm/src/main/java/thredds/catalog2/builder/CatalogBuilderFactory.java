package thredds.catalog2.builder;

import thredds.catalog2.*;
import thredds.catalog2.simpleImpl.ServiceImpl;
import thredds.catalog.ServiceType;

import java.util.Date;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogBuilderFactory
{
  public CatalogBuilder newCatalogBuilder( String name, URI docBaseUri, String version, Date expires, Date lastModified );
  public CatalogBuilder newCatalogBuilder( Catalog catalog );

  public ServiceBuilder newServiceBuilder( String name, ServiceType type, URI baseUri );
  public ServiceBuilder newServiceBuilder( Service service );

  public DatasetBuilder newDatasetBuilder( String name );
  public DatasetBuilder newDatasetBuilder( Dataset dataset );
  public DatasetAliasBuilder newDatasetAliasBuilder( String name, DatasetNodeBuilder dsToAlias );
  public DatasetAliasBuilder newDatasetAliasBuilder( DatasetAlias dsAlias);
  public CatalogRefBuilder newCatalogRefBuilder( String name, URI reference );
  public CatalogRefBuilder newCatalogRefBuilder( CatalogRef catRef);

  public MetadataBuilder newMetadataBuilder();

  public AccessBuilder newAccessBuilder( ServiceImpl service, String urlPath );
  public AccessBuilder newAccessBuilder( Access access);
}
