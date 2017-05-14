import java.util.*;

/**
 * Created by user on 5/12/17.
 */
public abstract class FA<State, Alphabet> extends DirectedGraph<State, FA.Edge<Alphabet>> {
  public enum ProductAnnotation {OR, AND, MINUS};

  private State initialState;
  private Set<State> acceptStates = new HashSet<>();

  public FA() {}

  public FA(FA<State, Alphabet> fa) {
    for (State s : fa.getVertices()) addVertex(s);
    for (FA.Edge<Alphabet> e : fa.getEdges()) {
      addEdge(e.copy(), fa.getDomain(e), fa.getCodomain(e));
    }
    setInitialState(fa.getInitialState());
    for (State s : fa.getAcceptStates()) addAcceptState(s);
  }

  void setInitialState(State state) {
    initialState = state;
  }

  State getInitialState() {return initialState;}

  void setAcceptStates(Set<State> acceptStates) {this.acceptStates = acceptStates;}

  Set<State> getAcceptStates() {return acceptStates;}

  void addAcceptState(State acceptState) {this.acceptStates.add(acceptState);}

  void addTransition(State initialState, Alphabet symbol, State resultingState) {addUniqueEdge(new Edge<>(symbol), initialState, resultingState);}

  void addDefaultTransition(State initialState, State resultingState) {addUniqueEdge(new Edge<>(Edge.SpecialKind.DEFAULT), initialState, resultingState);}

  public void addUniqueEdge(Edge<Alphabet> e, State from, State to) {
    if (e.isEpsilon() && from.equals(to)) return;
    for (FA.Edge<Alphabet> oe : getOutboundEdges(from))
      if (e.equalLabels(oe) && getCodomain(oe).equals(to)) return;

    addEdge(e, from, to);
  }

  boolean acceptedStateAttainable() {
    List<List<State>> components = this.getOrderedVertices(initialState);
    if (components.isEmpty()) return false;
    for (State v : components.get(0)) if (acceptStates.contains(v)) return true;
    return false;
  }

  void purgeUnattainableStates() {
    List<List<State>> components = this.getOrderedVertices(initialState);
    for (int i = 1; i<components.size(); i++)
      for (State v : components.get(i))
        removeVertex(v);
  }

  @Override
  public String toString() {
    String result = "";
    List<List<State>> components = getOrderedVertices(getInitialState());
    for (int i = 0; i < components.size(); i++) {
      int len = 0;
      boolean isLast = i == components.size() - 1;
      List<State> component = components.get(i);
      for (State s : component) {
        int cL = s.toString().length();
        if (getAcceptStates().contains(s) || getInitialState().equals(s)) cL += 3;
        if (cL > len) len = cL;
      }

      for (State s : component) {
        String label = "";
        if (s.equals(getInitialState())) label = "(>) ";
        label += s.toString();
        if (getAcceptStates().contains(s)) label += " (✓)";
        while (label.length() <= len) label += " ";
        result += "| " + label + " | ";

        Set<Edge<Alphabet>> outboundEdges = getOutboundEdges(s);
        List<Set<Edge<Alphabet>>> equalityClasses = new ArrayList<>();
        while (!outboundEdges.isEmpty()) {
          Edge<Alphabet> e = outboundEdges.iterator().next();
          outboundEdges.remove(e);

          boolean alreadyPresent = false;
          for (Set<Edge<Alphabet>> c : equalityClasses)
            if (c.iterator().next().equalLabels(e)) {
              alreadyPresent = true;
              c.add(e);
              break;
            }

          if (!alreadyPresent) {
            HashSet<Edge<Alphabet>> hs = new HashSet<>();
            equalityClasses.add(hs);
            hs.add(e);
          }
        }

        equalityClasses.sort(new Comparator<Set<Edge<Alphabet>>>() {
          @Override
          public int compare(Set<Edge<Alphabet>> edges, Set<Edge<Alphabet>> edges2) {
            Edge<Alphabet> e1 = edges.iterator().next();
            Edge<Alphabet> e2 = edges2.iterator().next();
            if (e1.myDefault && !e2.myDefault) return 1;
            if (e2.myDefault && !e1.myDefault) return -1;
            if (e1.myEmpty && !e2.myEmpty) return 1;
            if (e2.myEmpty && !e1.myEmpty) return -1;
            if (e1.label instanceof Comparable && e2.label instanceof Comparable) {
              Comparable l1 = (Comparable) e1.label;
              Comparable l2 = (Comparable) e2.label;
              return l1.compareTo(l2);
            }
            return 0;
          }
        });

        Map<String, Set<Edge<Alphabet>>> m = new HashMap<>();

        for (Set<Edge<Alphabet>> es : equalityClasses)        {
          result +=  es.iterator().next().toString() + " -> ";
          int cl = es.size() - 1;
          if (cl > 0) result += "{";
          for (Edge<Alphabet> e : es) {
            result += getCodomain(e).toString();
            if (cl > 0) result += ", ";
            cl--;
          }
          if (es.size() > 1) result += "}";
          result += "; ";
        }

        result += "\n";
      }

      if (!isLast) result += "=== next component === \n";
    }

    return result;
  }

  static class Edge<P> {
    enum SpecialKind {EPSILON, DEFAULT};
    private P label = null;
    private boolean myDefault = false;
    private boolean myEmpty = false;

    Edge(P l) {this.label = l;}

    Edge(SpecialKind kind) {
      switch (kind) {
        case EPSILON: myEmpty = true; break;
        case DEFAULT:
        default: myDefault = true; break;
      }
    }

    @Override
    public String toString() {
      if (myDefault) return "default"; else
        if (myEmpty) return "ε"; else return label.toString();
    }

    P getLabel() {return label;}

    boolean isDefault() {return myDefault;}

    boolean isEpsilon() {return myEmpty;}

    void setDefault(boolean d) {myDefault = d;}

    void setEpsilon(boolean d) {myEmpty = d;}

    boolean equalLabels(Edge<P> e) {
      if (myDefault) return e.isDefault();
      if (myEmpty) return e.isEpsilon();
      return label.equals(e.getLabel());
    }

    Edge<P> copy() {
      Edge<P> result = new Edge<P>(label);
      result.myDefault = myDefault;
      result.myEmpty = myEmpty;
      return result;
    }
  }
}
