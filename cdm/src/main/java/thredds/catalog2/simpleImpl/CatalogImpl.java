package thredds.catalog2.simpleImpl;

import thredds.catalog2.*;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogImpl implements Catalog, CatalogBuilder
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private String name;
  private URI docBaseUri;
  private String version;
  private Date expires;
  private Date lastModified;

  private List<ServiceBuilder> serviceBuilders;
  private List<Service> services;
  private Map<String,Service> servicesMap;

  private List<DatasetNodeBuilder> datasetBuilders;
  private List<DatasetNode> datasets;
  private Map<String,DatasetNode> datasetsMapById;

  private PropertyContainer propertyContainer;

  private Set<String> uniqueServiceNames;

  private boolean finished = false;


  public CatalogImpl( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.docBaseUri = docBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.serviceBuilders = new ArrayList<ServiceBuilder>();
    this.services = new ArrayList<Service>();
    this.servicesMap = new HashMap<String,Service>();

    this.datasetBuilders = new ArrayList<DatasetNodeBuilder>();
    this.datasets = new ArrayList<DatasetNode>();
    this.datasetsMapById = new HashMap<String,DatasetNode>();

    this.propertyContainer = new PropertyContainer();

    this.uniqueServiceNames = new HashSet<String>();
  }

  protected void addUniqueServiceName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished().");
    if ( ! this.uniqueServiceNames.add( name ) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in catalog.");
  }

  protected boolean removeUniqueServiceName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished().");
    return this.uniqueServiceNames.add( name );
  }

  protected boolean containUniqueServiceName( String name )
  {
    return this.uniqueServiceNames.contains( name );
  }

  public void setName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void setDocBaseUri( URI docBaseUri )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null." );
    this.docBaseUri = docBaseUri;
  }

  public URI getDocBaseUri()
  {
    return this.docBaseUri;
  }

  public void setVersion( String version )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.version = version;
  }

  public String getVersion()
  {
    return this.version;
  }

  public void setExpires( Date expires )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.expires = expires;
  }

  public Date getExpires()
  {
    return this.expires;
  }

  public void setLastModified( Date lastModified )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.lastModified = lastModified;
  }

  public Date getLastModified()
  {
    return this.lastModified;
  }

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );

    // Track unique service names, throw llegalStateException if name not unique.
    this.addUniqueServiceName( name );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this, null );
    this.serviceBuilders.add( sb );
    this.services.add( sb );
    this.servicesMap.put( name, sb );
    return sb;
  }

  public boolean removeService( ServiceBuilder builder )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( builder == null )
      throw new IllegalArgumentException( "Given ServiceBuilder may not be null.");

    if ( this.serviceBuilders.remove( builder ) )
    {
      if ( ! this.services.remove( builder ))
        log.warn( "removeService(): failed to remove ServiceBuilder [" + builder.getName() +"] (from list).");
      if ( null == this.servicesMap.remove( builder.getName() ) )
        log.warn( "removeService(): failed to remove ServiceBuilder [" + builder.getName() + "] (from map).");

      return true;
    }
    return false;
  }

  public List<Service> getServices()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without build() being called." );
    return Collections.unmodifiableList( this.services);
  }

  public Service getServiceByName( String name )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.servicesMap.get( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return Collections.unmodifiableList( this.serviceBuilders );
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return (ServiceBuilder) this.servicesMap.get( name );
  }

  public void addProperty( String name, String value )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.propertyContainer.addProperty( name, value );
  }

  public List<String> getPropertyNames()
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public DatasetBuilder addDataset( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    DatasetImpl db = new DatasetImpl( name, this, null );
    this.datasetBuilders.add( db );
    this.datasets.add( db );
    return db;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, this, null );
    this.datasetBuilders.add( crb );
    this.datasets.add( crb );
    return crb;
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return Collections.unmodifiableList( this.datasets );
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetsMapById.get( id );
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return Collections.unmodifiableList( this.datasetBuilders );
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return (DatasetNodeBuilder) this.datasetsMapById.get( id);
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.finished )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // ToDo Check any invariants.
    // Check invariants
    // ToDo check that all datasets with Ids have unique Ids

    // Check subordinates.
    for ( ServiceBuilder sb : this.serviceBuilders )
      sb.isBuildable( localIssues );
    for ( DatasetNodeBuilder dnb : this.datasetBuilders )
      dnb.isBuildable( localIssues );
    this.propertyContainer.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public Catalog build() throws BuilderException
  {
    if ( this.finished )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    // ToDo Check any invariants.
    // Check invariants
    // ToDo check that all datasets with Ids have unique Ids

    // Check subordinates.
    for ( ServiceBuilder sb : this.serviceBuilders )
      sb.build();
    for ( DatasetNodeBuilder dnb : this.datasetBuilders )
      dnb.build();
    this.propertyContainer.build();
    
    this.finished = true;
    return this;
  }
}
