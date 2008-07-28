package thredds.catalog2.builder;

import thredds.catalog2.Catalog;
import thredds.catalog2.explorer.CatalogExplorer;

import java.net.URI;
import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogBuilder extends CatalogExplorer
{
  public void setName( String name);
  public void setBaseUri( URI baseUri);
  public void setVersion( String version );
  public void setExpires( Date expires );
  public void setLastModified( Date lastModified );

  public ServiceBuilder addService();
  public ServiceBuilder addService( int index);

  public DatasetBuilder addDataset();
  public DatasetBuilder addDataset( int index);

  public CatalogRefBuilder addCatalogRef();
  public CatalogRefBuilder addCatalogRef( int index);

  public void addProperty( String name, String value );

  /**
   * Generate the resulting Catalog.
   *
   * @return the resulting Catalog object (immutable?).  
   * @throws IllegalStateException if any Catalog invariants are violated.
   */
  public Catalog finish();

}
