package thredds.catalog2.builder;

import thredds.catalog2.DatasetNode;

import java.util.List;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNodeBuilder
{
  public String getId();
  public void setId( String id );

  public String getName();
  public void setName( String name );

  public void addProperty( String name, String value );
  public List<String> getPropertyNames();
  public String getPropertyValue( String name );

  public MetadataBuilder addMetadata();

  public CatalogBuilder getParentCatalogBuilder();
  public DatasetBuilder getParentDatasetBuilder();

  public boolean isCollection();

  public DatasetBuilder addDataset( String name );
  public DatasetAliasBuilder addDatasetAlias( String name, DatasetNodeBuilder alias);
  public CatalogRefBuilder addCatalogRef( String name, URI reference);

  public List<DatasetNodeBuilder> getDatasetNodeBuilders();
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id );
  public DatasetNodeBuilder getDatasetNodeBuilderByName( String name );

  public boolean isFinished();
  public DatasetNode finish();
}
