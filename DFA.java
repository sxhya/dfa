import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 5/11/17.
 */
public class DFA<State, Alphabet> extends DirectedGraph<State, DFA.Edge<Alphabet>> {
  private State initialState;
  private Set<State> acceptStates = new HashSet<State>();
  public enum ProductAnnotation {OR, AND, MINUS};

  public void setInitialState(State state) {
    initialState = state;
  }

  public void setAcceptStates(Set<State> acceptStates) {
    this.acceptStates = acceptStates;
  }

  public void addTransition(State initialState, Alphabet symbol, State resultingState) {
    super.addEdge(new Edge<Alphabet>(symbol), initialState, resultingState);
  }

  public Set<Alphabet> assertRegular() {
    Set<Alphabet> result = null;
    for (State s : this.getVertices()) {
      Set<DFA.Edge<Alphabet>> edges = this.getOutboundEdges(s);
      Set<Alphabet> edgeLabels = new HashSet<Alphabet>();
      for (DFA.Edge<Alphabet> e : edges) edgeLabels.add(e.label);
      if (result == null) result = edgeLabels; else assert (result.equals(edgeLabels));
    }
    return result;
  }

  public static<State1, State2, Alphabet> DFA<Pair<State1, State2>, Alphabet> directProduct(DFA<State1, Alphabet> dfa1, DFA<State2, Alphabet> dfa2, ProductAnnotation annotation) {
    Set<Alphabet> alphabet = dfa1.assertRegular();
    assert (alphabet.equals(dfa2.assertRegular()));

    DFA<Pair<State1, State2>, Alphabet> result = new DFA<Pair<State1, State2>, Alphabet>();
    for (State1 v1 : dfa1.getVertices()){
      Map<Alphabet, DFA.Edge<Alphabet>> outbound1 = getLabelMap(dfa1.getOutboundEdges(v1));
      for (State2 v2 : dfa2.getVertices()) {
        Map<Alphabet, DFA.Edge<Alphabet>> outbound2 = getLabelMap(dfa2.getOutboundEdges(v2));
        Pair<State1, State2> p = new Pair<State1, State2>(v1, v2);
        for (Alphabet a : alphabet) {
          Pair<State1, State2> p2 = new Pair<State1, State2>(dfa1.getCodomain(outbound1.get(a)), dfa2.getCodomain(outbound2.get(a)));
          result.addEdge(new Edge<Alphabet>(a), p, p2);
        }
      }
    }

    result.setInitialState(new Pair<State1, State2>(dfa1.initialState, dfa2.initialState));

    Set<Pair<State1, State2>> aS = new HashSet<Pair<State1, State2>>();

    for (State1 s1 : dfa1.getVertices())
      for (State2 s2 : dfa2.getVertices()) {
        boolean a1 = dfa1.acceptStates.contains(s1);
        boolean a2 = dfa2.acceptStates.contains(s2);
        boolean a;
        switch (annotation) {
          case OR: a = a1 || a2; break;
          case AND: a = a1 && a2; break;
          default: a = a1 && !a2;
        }
        if (a) aS.add(new Pair<State1, State2>(s1,s2));
      }

    result.setAcceptStates(aS);

    return result;
  }

  private static<X> Map<X, Edge<X>> getLabelMap(Set<DFA.Edge<X>> edges) {
    HashMap<X, Edge<X>> result = new HashMap<X, Edge<X>>();
    for (DFA.Edge<X> e : edges) result.put(e.label, e);
    return result;
  }

  static class Edge<P> {
    private P label;

    Edge(P l) {
      this.label = l;
    }
  }
}
