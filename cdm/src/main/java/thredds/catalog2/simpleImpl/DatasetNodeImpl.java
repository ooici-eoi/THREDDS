package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.Property;
import thredds.catalog2.Metadata;
import thredds.catalog2.Catalog;
import thredds.catalog2.DatasetNode;

import java.util.List;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetNodeImpl implements DatasetNode, DatasetNodeBuilder
{
  private String id;
  private String name;
  private List<Property> properties;
  private Map<String,Property> propertiesMap;
  private List<Metadata> metadata;

  private Catalog parentCatalog;
  private DatasetNode parent;
  private List<DatasetNode> children;
  private Map<String,DatasetNode> childrenNameMap;
  private Map<String,DatasetNode> childrenIdMap;

  public CatalogRefBuilder addCatalogRef( int index )
  {
    return null;
  }

  public void setId( String id )
  {
  }

  public void setName( String name )
  {
  }

  public void addProperty( String name, String value )
  {
  }

  public MetadataBuilder addMetadata()
  {
    return null;
  }

  public DatasetBuilder addDataset()
  {
    return null;
  }

  public DatasetBuilder addDataset( int index )
  {
    return null;
  }

  public DatasetAliasBuilder addDatasetAlias()
  {
    return null;
  }

  public CatalogRefBuilder addCatalogRef()
  {
    return null;
  }

  public String getId()
  {
    return null;
  }

  public String getName()
  {
    return null;
  }

  public List<Property> getProperties()
  {
    return null;
  }

  public List<Metadata> getMetadata()
  {
    return null;
  }

  public Catalog getParentCatalog()
  {
    return null;
  }

  public DatasetNode getParent()
  {
    return null;
  }

  public boolean isCollection()
  {
    return false;
  }

  public List<DatasetNode> getDatasets()
  {
    return null;
  }

  public DatasetNode getDatasetByName( String name )
  {
    return null;
  }

  public DatasetNode getDatasetById( String id )
  {
    return null;
  }

  public Property getPropertyByName( String name )
  {
    return null;
  }

  public Property getProperty( String name )
  {
    return null;
  }

  public List<String> getPropertyNames()
  {
    return null;
  }

  public String getPropertyValue( String name )
  {
    return null;
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    return null;
  }

  public DatasetBuilder getParentDatasetBuilder()
  {
    return null;
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderByName( String name )
  {
    return null;
  }
}
