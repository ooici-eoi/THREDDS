/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.ugrid;

import ucar.nc2.dt.ugrid.geom.LatLonPolygon2D;
import java.util.ArrayList;

/**
 *
 * @author Kyle
 */
public class Cell {

  private LatLonPolygon2D polygon;
  private ArrayList<Entity> entities;
  private ArrayList<Node> nodes;
  private ArrayList<Edge> edges;
  private ArrayList<Face> faces;
  

  public Cell() {
    // Initialize the entities ArrayList
    entities = new ArrayList<Entity>();
    nodes = new ArrayList<Node>();
    faces = new ArrayList<Face>();
    edges = new ArrayList<Edge>();
  }

  private void setEntities(Entity e) {
    if (!entities.contains(e)) {
      entities.add(e);
    }
  }

  private void setEntities(ArrayList<? extends Entity> ets) {
    for (Entity e :ets) {
      setEntities(e);
    }
  }

  public ArrayList<Entity> getEntities() {
    return entities;
  }

  public LatLonPolygon2D getPolygon() {
    return polygon;
  }
  public void setPolygon(LatLonPolygon2D poly) {
    polygon = poly;
  }

  public ArrayList<Node> getNodes() {
    return nodes;
  }
  public void setNodes(ArrayList<Node> nodes) {
    this.nodes = nodes;
    setEntities(nodes);
  }
  public void addNode(Node node) {
    this.nodes.add(node);
    setEntities(node);
  }

  public ArrayList<Edge> getEdges() {
    return edges;
  }
  public void setEdges(ArrayList<Edge> edges) {
    this.edges = edges;
    setEntities(edges);
  }
  public void addEdge(Edge edge) {
    this.edges.add(edge);
    setEntities(edge);
  }

  public ArrayList<Face> getFaces() {
    return faces;
  }
  public void setFaces(ArrayList<Face> faces) {
    this.faces = faces;
    setEntities(faces);
  }
  public void addFace(Face face) {
    this.faces.add(face);
    setEntities(face);
  }

}
