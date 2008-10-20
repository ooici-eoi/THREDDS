package thredds.catalog2.simpleImpl;

import thredds.catalog2.Service;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;

import java.util.*;

/**
 * Helper class for those classes that contain services: CatalogImpl and ServiceImpl.
 *
 * @author edavis
 * @since 4.0
 */
class ServiceContainer
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  /**
   * Map for contained ServiceImpls keyed by service name.
   */
  private Map<String, ServiceImpl> servicesMap;

//  private List<ServiceBuilder> serviceBuilders;
//  private List<Service> services;
//  private Map<String, Service> servicesMap;

  /**
   * The root container used for tracking ServiceImpl objects by globally unique service names.
   */
  private final ServiceContainer rootContainer;

  /**
   * Map for tracking ServiceImpl objects by globally unique service names if this is root container.
   */
  private Map<String, ServiceImpl> servicesByGloballyUniqueName;

  private boolean isBuilt;

  ServiceContainer( ServiceContainer rootContainer )
  {
    this.isBuilt = false;
    this.servicesMap = null;

    this.rootContainer = rootContainer;
  }

  ServiceContainer getRootServiceContainer()
  {
    if ( this.rootContainer != null )
      return this.rootContainer;
    return this;
  }

  public boolean isServiceNameInUseGlobally( String name )
  {
    if ( this.getServiceByGloballyUniqueName( name ) == null )
      return false;
    return true;
  }

  protected boolean addServiceByGloballyUniqueName( ServiceImpl service )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built." );
    if ( service == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.addServiceByGloballyUniqueName( service );
    else
    {
      if ( this.servicesByGloballyUniqueName == null )
        this.servicesByGloballyUniqueName = new HashMap<String,ServiceImpl>();

      if ( this.servicesByGloballyUniqueName.containsKey( service.getName() ))
        return false;
      ServiceImpl replacedService = this.servicesByGloballyUniqueName.put( service.getName(), service );
      if ( replacedService == null )
        return true;
      else
      {
        String msg = "ServiceContainer in bad state [containsKey(" + service.getName() + ")==false then put()!=null].";
        log.error( "addServiceByGloballyUniqueName(): " + msg );
        throw new IllegalStateException( msg);
      }
    }
  }

  protected boolean removeServiceByGloballyUniqueName( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built." );
    if ( name == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.removeServiceByGloballyUniqueName( name );
    else
    {
      if ( this.servicesByGloballyUniqueName == null )
        return false;

      ServiceImpl removedService = this.servicesByGloballyUniqueName.remove( name );
      if ( removedService == null )
        return false;
      return true;
    }
  }

  protected ServiceImpl getServiceByGloballyUniqueName( String name )
  {
    if ( name == null )
      return null;

    if ( this.rootContainer != null )
      return this.rootContainer.getServiceByGloballyUniqueName( name );
    else
    {
      if ( this.servicesByGloballyUniqueName == null )
        return null;

      return this.servicesByGloballyUniqueName.get( name );
    }
  }

  public boolean isEmpty()
  {
    if ( this.servicesMap == null )
      return true;
    return this.servicesMap.isEmpty();
  }
  
  public int size()
  {
    if ( this.servicesMap == null )
      return 0;
    return this.servicesMap.size();
  }

  /**
   * Add a ServiceImpl to this container.
   *
   * @param service the ServiceImpl to add.
   * @throws IllegalArgumentException if service is null.
   * @throws IllegalStateException if build() has been called on this ServiceContainer or the name of the service is not unique in the root container.
   */
  public void addService( ServiceImpl service )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built.");

    if ( this.servicesMap == null )
      this.servicesMap = new LinkedHashMap<String, ServiceImpl>();

    // Track ServiceImpls by globally unique service names, throw llegalStateException if name not unique.
    if ( ! this.addServiceByGloballyUniqueName( service ))
      throw new IllegalStateException( "Service name is already being used.");

    if ( null != this.servicesMap.put( service.getName(), service ))
        log.error( "addService(): reset service name [" + service.getName() + "]." );

    return;
  }

  /**
   * Remove the Service with the given name from this container if it is present.
   *
   * @param name the name of the Service to remove.
   * @return true if the Service was present and has been removed, otherwise false.
   * @throws IllegalArgumentException if name is null.
   * @throws IllegalStateException if build() has been called on this ServiceContainer.
   */
  public ServiceImpl removeService( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built." );

    if ( name == null )
      return null;

    if ( this.servicesMap == null )
      return null;

    ServiceImpl removedService = this.servicesMap.remove( name );
    if ( removedService == null )
      return null;

    if ( ! this.removeServiceByGloballyUniqueName( name ) )
    {
      String msg = "Unique service name removal inconsistent with service removal [" + name + "].";
      log.error( "removeService(): " + msg );
      throw new IllegalStateException( msg );
    }
    return removedService;
  }

  public List<Service> getServices()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );

    if ( this.servicesMap == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Service>( this.servicesMap.values() ) );
  }

  public boolean containsServiceName( String name )
  {
    if ( name == null )
      return false;

    if ( this.servicesMap == null )
      return false;

    if ( this.servicesMap.get( name ) == null )
      return false;
    return true;
  }

  public Service getServiceByName( String name )
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    if ( name == null )
      return null;
    if ( this.servicesMap == null )
      return null;
    return this.servicesMap.get( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( this.servicesMap == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ServiceBuilder>( this.servicesMap.values() ) );
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( name == null )
      return null;
    if ( this.servicesMap == null )
      return null;

    return this.servicesMap.get( name );
  }

  /**
   * Check whether contained ServiceBuilders are all in a state such that
   * calling their build() will succeed.
   *
   * @param issues a list into which any issues that come up during isBuildable() will be add.
   * @return true if this ServiceContainer is in a state where build() will succeed.
   */
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // Check on contained ServiceImpl objects.
    if ( this.servicesMap != null )
      for ( ServiceBuilder sb : this.servicesMap.values() )
        sb.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  /**
   * Call build() on all contained services.
   *
   * @throws BuilderException if any of the contained services are not in a valid state.
   */
  public void build()
          throws BuilderException
  {
    if ( this.isBuilt )
      return;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( ! isBuildable( issues ) )
      throw new BuilderException( issues );

    // Build contained ServiceImpl objects.
    if ( this.servicesMap != null )
      for ( ServiceBuilder sb : this.servicesMap.values() )
        sb.build();

    this.isBuilt = true;
    return;
  }
}