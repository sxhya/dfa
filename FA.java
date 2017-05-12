import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
      addEdge(copyEdge(e), fa.getDomain(e), fa.getCodomain(e));
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

  void addDefaultTransition(State initialState, State resultingState) {addUniqueEdge(new Edge<>(), initialState, resultingState);}

  Edge<Alphabet> copyEdge(Edge<Alphabet> edge) {
    Edge<Alphabet> result = new Edge<>();
    result.label = edge.label;
    result.myDefault = edge.myDefault;
    return result;
  }

  public void addUniqueEdge(Edge<Alphabet> e, State from, State to) {
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
    for (int i = 1; i<components.size()-1; i++) {
      for (State v : components.get(i)) removeVertex(v);
    }
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
        for (Edge<Alphabet> e : getOutboundEdges(s)) {
          result +=  e.toString() + " -> " + getCodomain(e) + "; ";
        }
        result += "\n";
      }

      if (!isLast) result += "=== next component === \n";
    }

    return result;
  }

  static class Edge<P> {
    private P label = null;
    private boolean myDefault = false;

    Edge(P l) {this.label = l;}
    Edge() {this.myDefault = true;}

    @Override
    public String toString() {
      if (myDefault) return "default"; else return label.toString();
    }

    P getLabel() {return label;}

    boolean isDefault() {return myDefault;}

    void setDefault(boolean d) {myDefault = d;}

    boolean equalLabels(Edge<P> e) {
      if (myDefault) return e.isDefault();
      return label.equals(e.getLabel());
    }
  }
}
