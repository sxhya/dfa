import java.util.*;

/**
 * Created by user on 5/11/17.
 */
public class DirectedGraph<V, E> {
  private Set<V> vertices = new HashSet<V>();
  private Map<E, Pair<V,V>> boundaries = new HashMap<E, Pair<V, V>>();
  private Map<V, Set<E>> outboundEdges = new HashMap<V, Set<E>>();
  private Map<V, Set<E>> inboundEdges = new HashMap<V, Set<E>>();

  public void addVertex(V v) {
    if (vertices.contains(v)) throw new IllegalArgumentException();
    vertices.add(v);
  }

  public void addEdge(E e, V from, V to) {
    if (boundaries.keySet().contains(e)) throw new IllegalArgumentException();

    if (!vertices.contains(from)) addVertex(from);
    if (!vertices.contains(to)) addVertex(to);
    boundaries.put(e, new Pair<V, V>(from, to));
    Set<E> outbound = outboundEdges.get(from);
    Set<E> inbound = inboundEdges.get(to);
    if (outbound == null) {
      outbound = new HashSet<E>();
      outboundEdges.put(from, outbound);
    }
    if (inbound == null) {
      inbound = new HashSet<E>();
      inboundEdges.put(to, inbound);
    }
    outbound.add(e);
    inbound.add(e);
  }

  public Set<E> getOutboundEdges(V v) {
    Set<E> result = outboundEdges.get(v);
    return result != null ? result : new HashSet<E>();
  }

  public Set<E> getInboundEdges(V v) {
    return inboundEdges.get(v);
  }

  public V getDomain(E e) {
    Pair<V, V> p =  boundaries.get(e);
    return p != null ? p.a : null;
  }

  public V getCodomain(E e) {
    Pair<V, V> p =  boundaries.get(e);
    return p != null ? p.b : null;
  }

  public void removeEdge(E e) {
    V from = getDomain(e);
    V to = getCodomain(e);
    Set<E> outbound = getOutboundEdges(from);
    Set<E> inbound = getInboundEdges(to);
    if (outbound != null) outbound.remove(e);
    if (inbound != null) inbound.remove(e);
    boundaries.remove(e);
  }

  public void removeVertex(V v) {
    Set<E> inboundEdges = getInboundEdges(v);
    Set<E> outboundEdges = getOutboundEdges(v);
    for (E e : inboundEdges) removeEdge(e);
    for (E e : outboundEdges) removeEdge(e);
    vertices.remove(v);
  }

  public Set<V> getVertices() {
    return vertices;
  }

  public Set<E> getEdges() {
    return boundaries.keySet();
  }

  public List<List<V>> getOrderedVertices(V v) {
    List<List<V>> list = new ArrayList<>();
    Set<V> remainingVertices = getVertices();
    Set<V> beenThere = new HashSet<V>();
    do {
      List<V> list2 = new ArrayList<V>();
      list.add(list2);
      list2.add(v);
      beenThere.add(v);
      getVerticesInt(v, list2, beenThere);
      remainingVertices.removeAll(beenThere);
      if (remainingVertices.isEmpty()) {
        v = null;
      } else {
        v = remainingVertices.iterator().next();
      }
    } while (v != null);
    return list;
  }

  private void getVerticesInt(V v, List<V> result, Set<V> beenThere) {
    Set<V> vs = new HashSet<V>();
    Set<V> newvs = new HashSet<V>();
    for (E e : getOutboundEdges(v)) vs.add(getCodomain(e));
    for (V vp : vs) if (!beenThere.contains(vp)) newvs.add(vp);

    beenThere.addAll(newvs);
    result.addAll(newvs);

    for (V vp : newvs) {
      getVerticesInt(vp, result, beenThere);
    }
  }

}
