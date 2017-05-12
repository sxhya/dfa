import java.util.*;

/**
 * Created by user on 5/11/17.
 */
public class DFA<State, Alphabet> extends DirectedGraph<State, DFA.Edge<Alphabet>> {
  private State initialState;
  private Set<State> acceptStates = new HashSet<>();
  public enum ProductAnnotation {OR, AND, MINUS};

  void setInitialState(State state) {
    initialState = state;
  }

  void setAcceptStates(Set<State> acceptStates) {
    this.acceptStates = acceptStates;
  }

  void addTransition(State initialState, Alphabet symbol, State resultingState) {
    super.addEdge(new Edge<>(symbol), initialState, resultingState);
  }

  void addDefaultTransition(State initialState, State resultingState) {
    super.addEdge(new Edge<>(), initialState, resultingState);
  }

  public static<State1, State2, Alphabet> DFA<Pair<State1, State2>, Alphabet> directProduct(DFA<State1, Alphabet> dfa1, DFA<State2, Alphabet> dfa2, ProductAnnotation annotation) {
    DFA<Pair<State1, State2>, Alphabet> result = new DFA<>();
    for (State1 v1 : dfa1.getVertices()){
      Pair<Map<Alphabet, DFA.Edge<Alphabet>>, Edge<Alphabet>> outbound1 = getLabelMap(dfa1.getOutboundEdges(v1));
      for (State2 v2 : dfa2.getVertices()) {
        Pair<Map<Alphabet, DFA.Edge<Alphabet>>, Edge<Alphabet>> outbound2 = getLabelMap(dfa2.getOutboundEdges(v2));
        Pair<State1, State2> p = new Pair<>(v1, v2);
        Set<Alphabet> unionAlphabet = new HashSet<>();
        unionAlphabet.addAll(outbound1.a.keySet());
        unionAlphabet.addAll(outbound2.a.keySet());

        State1 defaultState1 = outbound1.b == null ? null : dfa1.getCodomain(outbound1.b);
        State2 defaultState2 = outbound2.b == null ? null : dfa2.getCodomain(outbound2.b);

        if (defaultState1 != null && defaultState2 != null) {
          result.addDefaultTransition(p, new Pair<>(defaultState1, defaultState2));
        }

        for (Alphabet a : unionAlphabet) {
          DFA.Edge<Alphabet> s1 = outbound1.a.get(a);
          DFA.Edge<Alphabet> s2 = outbound2.a.get(a);
          State1 newState1;
          State2 newState2;
          if (s1 == null) {
            assert (defaultState1 != null);
            newState1 = defaultState1;
          } else {
            newState1 = dfa1.getCodomain(s1);
          }
          if (s2 == null) {
            assert (defaultState2 != null);
            newState2 = defaultState2;
          } else {
            newState2 = dfa2.getCodomain(s2);
          }

          Pair<State1, State2> p2 = new Pair<>(newState1, newState2);
          result.addTransition(p, a, p2);
        }
      }
    }

    result.setInitialState(new Pair<>(dfa1.initialState, dfa2.initialState));

    Set<Pair<State1, State2>> aS = new HashSet<>();

    for (State1 s1 : dfa1.getVertices())
      for (State2 s2 : dfa2.getVertices()) {
        boolean a1 = dfa1.acceptStates.contains(s1);
        boolean a2 = dfa2.acceptStates.contains(s2);
        boolean a;
        switch (annotation) {
          case OR: a = a1 || a2; break;
          case AND: a = a1 && a2; break;
          case MINUS:
          default: a = a1 && !a2;
        }
        if (a) aS.add(new Pair<>(s1,s2));
      }

    result.setAcceptStates(aS);

    return result;
  }

  public static<S, A> boolean runDFA(DFA<S, A> dfa, Iterable<A> list) {
    boolean success;
    S s = dfa.initialState;
    for (A a : list)
      s = dfa.getNewState(s, a);
    success = dfa.acceptStates.contains(s);
    return success;
  }

  @Override
  public String toString() {
    String result = "";
    List<List<State>> components = getOrderedVertices(initialState);
    for (int i = 0; i < components.size(); i++) {
      int len = 0;
      boolean isLast = i == components.size() - 1;
      List<State> component = components.get(i);
      for (State s : component) {
        int cL = s.toString().length();
        if (acceptStates.contains(s) || initialState.equals(s)) cL += 3;
        if (cL > len) len = cL;
      }

      for (State s : component) {
        String label = "";
        if (s.equals(initialState)) label = "(>) ";
        label += s.toString();
        if (acceptStates.contains(s)) label += " (✓)";
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

  private State getNewState(State initialState, Alphabet symbol) {
    Pair<Map<Alphabet, Edge<Alphabet>>, Edge<Alphabet>> outbound = DFA.getLabelMap(this.getOutboundEdges(initialState));
    DFA.Edge<Alphabet> edge = outbound.a.get(symbol);
    if (edge == null) {
      edge = outbound.b;
    }
    assert (edge != null);
    return getCodomain(edge);
  }

  private static<X> Pair<Map<X, Edge<X>>, Edge<X>> getLabelMap(Set<DFA.Edge<X>> edges) {
    HashMap<X, Edge<X>> result = new HashMap<>();
    Edge<X> def = null;
    for (DFA.Edge<X> e : edges) {
      if (e.myDefault) {
        assert (def == null); // there can be only one default edge
        def = e;
      } else {
        assert (e.label != null);
        result.put(e.label, e);
      }
    }
    return new Pair<>(result, def);
  }

  static class Edge<P> {
    private P label = null;
    private boolean myDefault = false;

    Edge(P l) {
      this.label = l;
    }
    Edge() {this.myDefault = true; }

    @Override
    public String toString() {
      if (myDefault) return "default"; else return label.toString();
    }
  }
}
