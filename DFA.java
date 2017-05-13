import java.util.*;

/**
 * Created by user on 5/11/17.
 */
public class DFA<State, Alphabet> extends FA<State, Alphabet> {

  public DFA(DFA<State, Alphabet> dfa) {
    super(dfa);
  }

  public DFA() {super();}

  public static<State1, State2, Alphabet> DFA<Pair<State1, State2>, Alphabet> directProduct(DFA<State1, Alphabet> dfa1, DFA<State2, Alphabet> dfa2, ProductAnnotation annotation) {
    DFA<Pair<State1, State2>, Alphabet> result = new DFA<>();
    for (State1 v1 : dfa1.getVertices()){
      Pair<Map<Alphabet, FA.Edge<Alphabet>>, Edge<Alphabet>> outbound1 = getLabelMap(dfa1.getOutboundEdges(v1));
      for (State2 v2 : dfa2.getVertices()) {
        Pair<Map<Alphabet, FA.Edge<Alphabet>>, Edge<Alphabet>> outbound2 = getLabelMap(dfa2.getOutboundEdges(v2));
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
          FA.Edge<Alphabet> s1 = outbound1.a.get(a);
          FA.Edge<Alphabet> s2 = outbound2.a.get(a);
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

    result.setInitialState(new Pair<>(dfa1.getInitialState(), dfa2.getInitialState()));

    Set<Pair<State1, State2>> aS = new HashSet<>();

    for (State1 s1 : dfa1.getVertices())
      for (State2 s2 : dfa2.getVertices()) {
        boolean a1 = dfa1.getAcceptStates().contains(s1);
        boolean a2 = dfa2.getAcceptStates().contains(s2);
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
    S s = dfa.getInitialState();
    for (A a : list)
      s = dfa.getNewState(s, a);
    success = dfa.getAcceptStates().contains(s);
    return success;
  }

  private State getNewState(State initialState, Alphabet symbol) {
    Pair<Map<Alphabet, Edge<Alphabet>>, Edge<Alphabet>> outbound = DFA.getLabelMap(this.getOutboundEdges(initialState));
    FA.Edge<Alphabet> edge = outbound.a.get(symbol);
    if (edge == null) {
      edge = outbound.b;
    }
    assert (edge != null);
    return getCodomain(edge);
  }

  private static<X> Pair<Map<X, Edge<X>>, Edge<X>> getLabelMap(Set<FA.Edge<X>> edges) {
    HashMap<X, Edge<X>> result = new HashMap<>();
    Edge<X> def = null;
    for (FA.Edge<X> e : edges) {
      if (e.isDefault()) {
        assert (def == null); // there can be only one default edge
        def = e;
      } else {
        assert (e.getLabel() != null);
        result.put(e.getLabel(), e);
      }
    }
    return new Pair<>(result, def);
  }
}
