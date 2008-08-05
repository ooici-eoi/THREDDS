package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.Property;
import thredds.catalog2.Metadata;
import thredds.catalog2.Catalog;
import thredds.catalog2.DatasetNode;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.net.URI;

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
  protected DatasetNode parent;
  private List<DatasetNodeBuilder> childrenBuilders;
  private List<DatasetNode> children;
  private Map<String,DatasetNode> childrenNameMap;
  private Map<String,DatasetNode> childrenIdMap;

  private boolean finished = false;

  protected DatasetNodeImpl( String name, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
    this.parentCatalog = (Catalog) parentCatalog;
    this.parent = (DatasetNode) parent;
  }

  @Override
  public void setId( String id )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished().");
//ToDo    ((CatalogImpl) this.parentCatalog).getCatalogSearchHelper();
    this.id = id;
    ((DatasetNodeImpl) this.parent).childrenIdMap.put( id, this );
  }

  @Override
  public String getId()
  {
    return this.id;
  }

  @Override
  public void setName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null." );
    this.name = name;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public void addProperty( String name, String value )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    PropertyImpl property = new PropertyImpl( name, value );
    Property curProp = this.propertiesMap.get( name );
    if ( curProp != null )
    {
      int index = this.properties.indexOf( curProp );
      this.properties.remove( index );
      this.propertiesMap.remove( name );
      this.properties.add( index, property );
    }
    else
    {
      this.properties.add( property );
    }

    this.propertiesMap.put( name, property );
    return;
  }

  @Override
  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.properties );
  }

  @Override
  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.propertiesMap.get( name);
  }

  @Override
  public List<String> getPropertyNames()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return Collections.unmodifiableList( new ArrayList<String>( this.propertiesMap.keySet() ) );
  }

  @Override
  public String getPropertyValue( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.propertiesMap.get( name ).getValue();
  }

  @Override
  public MetadataBuilder addMetadata()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    //MetadataBuilder mb = new MetadataImpl();
    return null;
  }

  @Override
  public List<Metadata> getMetadata()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public DatasetBuilder addDataset( String name)
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetBuilder db = new DatasetImpl( name, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( db );
    this.children.add( (DatasetNode) db );
    this.childrenNameMap.put( name, (DatasetNode) db );
    return db;
  }

  @Override
  public DatasetAliasBuilder addDatasetAlias( String name, DatasetNodeBuilder alias )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetAliasBuilder dab = new DatasetAliasImpl( name, alias, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( dab );
    this.children.add( (DatasetNode) dab );
    this.childrenNameMap.put( name, (DatasetNode) dab );
    return dab;
  }

  @Override
  public CatalogRefBuilder addCatalogRef( String name, URI reference)
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    CatalogRefBuilder crb = new CatalogRefImpl( name, reference, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( crb );
    this.children.add( (DatasetNode) crb );
    this.childrenNameMap.put( name, (DatasetNode) crb );
    return crb;
  }

  @Override
  public Catalog getParentCatalog()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parentCatalog;
  }

  @Override
  public DatasetNode getParent()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parent;
  }

  @Override
  public boolean isCollection()
  {
    return ! this.childrenBuilders.isEmpty();
  }

  @Override
  public List<DatasetNode> getDatasets()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.children);
  }

  @Override
  public DatasetNode getDatasetByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.childrenNameMap.get( name );
  }

  @Override
  public DatasetNode getDatasetById( String id )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.childrenIdMap.get( id);
  }

  @Override
  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetBuilder getParentDatasetBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderByName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public boolean isFinished()
  {
    return this.finished;
  }

  public DatasetNode finish()
  {
    if ( this.finished ) return this;

    // Check invariants.

    // Mark as finished.
    this.finished = true;
    return this;
  }
}
