package thredds.catalog2.simpleImpl;

import thredds.catalog2.DatasetNode;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.DatasetNodeBuilder;

import java.util.*;

/**
 * Helper class for those classes that contain dataset nodes: CatalogImpl and DatasetNodeImpl.
 *
 * @author edavis
 * @since 4.0
 */
class DatasetNodeContainer
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

//  /**
//   * Map for contained DatasetNodeImpls keyed by dataset ID.
//   */
//  private Map<String, DatasetNodeImpl> datasetNodeImplMapById;

  /**
   * List of contained DatasetNodeImpl objects.
   */
  private List<DatasetNodeImpl> datasetNodeImplList;
  private List<String> localIdList;

  /**
   * The root container used for tracking DatasetNodeImpl objects by globally unique ID.
   */
  private final DatasetNodeContainer rootContainer;

  /**
   * Map for tracking DatasetNodeImpl objects by globally unique ID if this is root container.
   */
  private Map<String, DatasetNodeImpl> datasetNodeImplMapByGloballyUniqueId;

  private boolean isBuilt;

  DatasetNodeContainer( DatasetNodeContainer rootContainer )
  {
    this.isBuilt = false;
    this.datasetNodeImplList = null;

    this.rootContainer = rootContainer;
  }

  DatasetNodeContainer getRootContainer()
  {
    if ( this.rootContainer != null )
      return this.rootContainer;
    return this;
  }

  public boolean isDatasetNodeIdInUseGlobally( String id )
  {
    if ( this.getDatasetNodeByGloballyUniqueId( id ) == null )
      return false;
    return true;
  }

  boolean addDatasetNodeByGloballyUniqueId( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( datasetNode == null )
      return false;
    if ( datasetNode.getId() == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.addDatasetNodeByGloballyUniqueId( datasetNode );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        this.datasetNodeImplMapByGloballyUniqueId = new HashMap<String,DatasetNodeImpl>();

      if ( this.datasetNodeImplMapByGloballyUniqueId.containsKey( datasetNode.getId() ))
        return false;
      DatasetNodeImpl replacedDatasetNode = this.datasetNodeImplMapByGloballyUniqueId.put( datasetNode.getId(), datasetNode );
      if ( replacedDatasetNode == null )
        return true;
      else
      {
        String msg = "DatasetNodeContainer in bad state [MapByGloballyUniqueId: containsKey(" + datasetNode.getId() + ")==false then put()!=null].";
        log.error( "addDatasetNodeByGloballyUniqueId(): " + msg );
        throw new IllegalStateException( msg);
      }
    }
  }

  protected boolean removeDatasetNodeByGloballyUniqueId( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.removeDatasetNodeByGloballyUniqueId( id );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        return false;

      DatasetNodeImpl removedDatasetNode = this.datasetNodeImplMapByGloballyUniqueId.remove( id );
      if ( removedDatasetNode == null )
        return false;
      return true;
    }
  }

  boolean addDatasetNodeToLocalById( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( datasetNode == null )
      return false;
    if ( datasetNode.getId() == null )
      return false;

    if ( this.localIdList == null )
      this.localIdList = new ArrayList<String>();

    return this.localIdList.add( datasetNode.getId() );
  }

  protected boolean removeDatasetNodeFromLocalById( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return false;

    if ( this.localIdList == null )
      return false;

    return this.localIdList.remove( id );
  }

  protected DatasetNodeImpl getDatasetNodeByGloballyUniqueId( String id )
  {
    if ( id == null )
      return null;

    if ( this.rootContainer != null )
      return this.rootContainer.getDatasetNodeByGloballyUniqueId( id );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        return null;

      return this.datasetNodeImplMapByGloballyUniqueId.get( id );
    }
  }

  public boolean isEmpty()
  {
    if ( this.datasetNodeImplList == null )
      return true;
    return this.datasetNodeImplList.isEmpty();
  }

  public int size()
  {
    if ( this.datasetNodeImplList == null )
      return 0;
    return this.datasetNodeImplList.size();
  }

  /**
   * Add a DatasetNodeImpl to this container.
   *
   * @param datasetNode the DatasetNodeImpl to add.
   * @throws IllegalArgumentException if datasetNode is null.
   * @throws IllegalStateException if build() has been called on this DatasetNodeContainer or the id of the DatasetNode is not unique in the root container.
   */
  public void addDatasetNode( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built.");

    // If datasetNode has an ID, make sure it is globally unique (i.e., not in use).
    // If it is unique, track it both globally and as part of this collection.
    if ( datasetNode.getId() != null )
    {
      if ( ! this.addDatasetNodeByGloballyUniqueId( datasetNode ) )
        throw new IllegalStateException( "Globally unique DatasetNode ID is already being used." );
      if ( this.localIdList == null )
        this.localIdList = new ArrayList<String>();
      this.localIdList.add( datasetNode.getId() );
    }

    if ( this.datasetNodeImplList == null )
      this.datasetNodeImplList = new ArrayList<DatasetNodeImpl>();

    if ( ! this.datasetNodeImplList.add( datasetNode ))
      log.error( "addDatasetNode(): failed to add datasetNode name [" + datasetNode.getName() + "]." );

    return;
  }

  /**
   * Remove the given DatasetNode from this container if it is present.
   *
   * @param datasetNode the DatasetNode to remove.
   * @return true if the DatasetNode was present and has been removed, otherwise false.
   * @throws IllegalArgumentException if datasetNode is null.
   * @throws IllegalStateException if build() has been called on this DatasetNodeContainer.
   */
  public boolean removeDatasetNode( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );

    if ( datasetNode == null )
      return false;

    if ( this.datasetNodeImplList == null )
      return false;

    // Remove from container.
    if ( ! this.datasetNodeImplList.remove( datasetNode ))
      return false;

    // Check if has global ID and remove from Map tracking DataseNodes by global ID.
    String id = datasetNode.getId();
    if ( id != null )
    {
      if ( this.localIdList != null && this.localIdList.remove( id ) )
      {
        if ( ! this.removeDatasetNodeByGloballyUniqueId( id ) )
        {
          String msg = "Removal from DatasetNode by global ID inconsistent with DatasetNode removal [" + datasetNode.getName() + "].";
          log.error( "removeDatasetNode(): " + msg );
          throw new IllegalStateException( msg );
        }
      }
    }
    return true;
  }

  public List<DatasetNode> getDatasets()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeCollection has escaped its Builder before being built." );

    if ( this.datasetNodeImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DatasetNode>( this.datasetNodeImplList ));
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeCollection has escaped its Builder before being built." );
    if ( id == null )
      return null;
    if ( this.datasetNodeImplList == null )
      return null;

    if ( this.localIdList != null && this.localIdList.contains( id ) )
      return this.getDatasetNodeByGloballyUniqueId( id );
    return null;
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( this.datasetNodeImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DatasetNodeBuilder>( this.datasetNodeImplList ) );
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return null;
    if ( this.datasetNodeImplList == null )
      return null;

    if ( this.localIdList != null && this.localIdList.contains( id ) )
      return this.getDatasetNodeByGloballyUniqueId( id );
    return null;
  }

  /**
   * Check whether contained DatasetNodeBuilders are all in a state such that
   * calling their build() will succeed.
   *
   * @param issues a list into which any issues that come up during isBuildable() will be add.
   * @return true if this DatasetNodeContainer is in a state where build() will succeed.
   */
  public boolean isBuildable( List<BuilderIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderIssue> localIssues = new ArrayList<BuilderIssue>();

    // Check on contained DatasetNodeImpl objects.
    if ( this.datasetNodeImplList != null )
      for ( DatasetNodeBuilder dnb : this.datasetNodeImplList )
        dnb.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  /**
   * Call build() on all contained datasets.
   *
   * @throws thredds.catalog2.builder.BuilderException if any of the contained datasets are not in a valid state.
   */
  public void build()
          throws BuilderException
  {
    if ( this.isBuilt )
      return;

    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( ! isBuildable( issues ) )
      throw new BuilderException( issues );

    // Build contained DatasetNodeImpl objects.
    if ( this.datasetNodeImplList != null )
      for ( DatasetNodeBuilder dnb : this.datasetNodeImplList )
        dnb.build();

    this.isBuilt = true;
    return;
  }
}