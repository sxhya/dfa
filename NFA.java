import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sxh on 12.05.17.
 */
public class NFA<State, Alphabet> extends FA<State, Alphabet> {

  NFA(DFA<State, Alphabet> dfa) {
    super(dfa);
  }

  NFA(NFA<State, Alphabet> nfa) {
    super(nfa);
  }

  NFA() {super();}

  DFA<State, Alphabet> convertToDFA() {
    DFA<State, Alphabet> result = new DFA<>();
    NFA<State, Alphabet> closed = getEpsilonClosure();
    // some code:
    return result;
  }

  NFA<State, Alphabet> getEpsilonClosure() {
    NFA<State, Alphabet> result = new NFA<>(this);
    Set<Edge<Alphabet>> epsilonEdges = result.getEpsilonEdges();
    if (epsilonEdges.isEmpty()) return result;

    //Stage 1: transitive closure of epsilon edges
    Map<State, Set<State>> hasEpsilonEdge = new HashMap<>();
    for (Edge<Alphabet> e : epsilonEdges) hasEpsilonEdge.computeIfAbsent(getDomain(e), k -> new HashSet<>()).add(getCodomain(e));
    boolean foundNew;
    do {
      foundNew = false;
      for (State s1 : hasEpsilonEdge.keySet())
        for (State s2 : hasEpsilonEdge.get(s1)) if (hasEpsilonEdge.get(s2) != null)
          for (State s3 : hasEpsilonEdge.get(s2)) if (!hasEpsilonEdge.get(s1).contains(s3)) {
            addEpsilonTransition(s1, s3);
            hasEpsilonEdge.get(s1).add(s3);
            foundNew = true;
          }
    } while (foundNew);

    //Stage 2: saturate accept states
    epsilonEdges = result.getEpsilonEdges();
    for (Edge<Alphabet> e : epsilonEdges) if (getAcceptStates().contains(getCodomain(e))) addAcceptState(getDomain(e));

    //Stage 3: compose non-epsilon edges with epsilon edges
    for (Edge<Alphabet> e : epsilonEdges)
      for (FA.Edge<Alphabet> o : getOutboundEdges(getCodomain(e)))
        addEdge(copyEdge(o), getDomain(e), getCodomain(o));

    //Stage 4: purge all epsilon edges
    for (Edge<Alphabet> e : epsilonEdges) removeEdge(e);

    return result;
  }

  @Override
  void addTransition(State initialState, Alphabet symbol, State resultingState) {
    addUniqueEdge(new NFA.Edge<>(symbol), initialState, resultingState);
  }

  @Override
  void addDefaultTransition(State initialState, State resultingState) {
    addUniqueEdge(new NFA.Edge<>(Edge.SpecialKind.DEFAULT), initialState, resultingState);
  }

  void addEpsilonTransition(State initialState, State resultingState) {
    // no epsilon loops are allowed
    if (initialState.equals(resultingState)) return;

    addUniqueEdge(new NFA.Edge<>(Edge.SpecialKind.EPSILON), initialState, resultingState);
  }

  @Override
  FA.Edge<Alphabet> copyEdge(FA.Edge<Alphabet> edge) {
    Edge<Alphabet> result = new Edge<>(edge.getLabel());
    result.setDefault(edge.isDefault());
    result.myEmpty = (edge instanceof NFA.Edge) && ((Edge) edge).myEmpty;
    return result;
  }

  private Set<Edge<Alphabet>> getEpsilonEdges() {
    Set<Edge<Alphabet>> result = new HashSet<>();
    for (FA.Edge<Alphabet> e : getEdges())
      if (e instanceof NFA.Edge && ((Edge) e).isEpsilon())
        result.add((Edge<Alphabet>) e);
    return result;
  }

  boolean containsEpsilonEdges() {
    return !getEpsilonEdges().isEmpty();
  }

  private static<X> Pair<Map<X, Set<FA.Edge<X>>>, Set<FA.Edge<X>>> getLabelMap(Set<FA.Edge<X>> edges) {
    HashMap<X, Set<FA.Edge<X>>> result = new HashMap<>();
    Set<FA.Edge<X>> def = new HashSet<>();
    for (FA.Edge<X> e : edges) {
      if (e.isDefault()) {
        def.add(e);
      } else {
        assert (e.getLabel() != null);
        result.computeIfAbsent(e.getLabel(), k -> new HashSet<>()).add(e);
      }
    }
    return new Pair<>(result, def);
  }

  public static<State1, State2, Alphabet> NFA<Pair<State1, State2>, Alphabet> directProduct(NFA<State1, Alphabet> nfa1, NFA<State2, Alphabet> nfa2, ProductAnnotation annotation) {
    NFA<Pair<State1, State2>, Alphabet> result = new NFA<>();
    for (State1 v1 : nfa1.getVertices()){
      Pair<Map<Alphabet, Set<FA.Edge<Alphabet>>>, Set<FA.Edge<Alphabet>>> outbound1 = getLabelMap(nfa1.getOutboundEdges(v1));
      for (State2 v2 : nfa2.getVertices()) {
        Pair<Map<Alphabet, Set<FA.Edge<Alphabet>>>, Set<FA.Edge<Alphabet>>> outbound2 = getLabelMap(nfa2.getOutboundEdges(v2));
        Pair<State1, State2> p = new Pair<>(v1, v2);
        Set<Alphabet> unionAlphabet = new HashSet<>();
        unionAlphabet.addAll(outbound1.a.keySet());
        unionAlphabet.addAll(outbound2.a.keySet());

        for (FA.Edge<Alphabet> s1 : outbound1.b)
          for (FA.Edge<Alphabet> s2 : outbound2.b)
            result.addDefaultTransition(p, new Pair<>(nfa1.getCodomain(s1), nfa2.getCodomain(s2)));

        for (Alphabet a : unionAlphabet) {
          Set<FA.Edge<Alphabet>> ss1 = outbound1.a.get(a);
          Set<FA.Edge<Alphabet>> ss2 = outbound2.a.get(a);

          if (ss1 == null) {
            assert (!outbound1.b.isEmpty());
            ss1 = outbound1.b;
          }

          if (ss2 == null) {
            assert (!outbound2.b.isEmpty());
            ss2 = outbound2.b;
          }

          for (FA.Edge<Alphabet> s1 : ss1)
            for (FA.Edge<Alphabet> s2 : ss2){
              State1 newState1 = nfa1.getCodomain(s1);
              State2 newState2 = nfa2.getCodomain(s2);

              Pair<State1, State2> p2 = new Pair<>(newState1, newState2);
              result.addTransition(p, a, p2);
            }
        }
      }
    }

    result.setInitialState(new Pair<>(nfa1.getInitialState(), nfa2.getInitialState()));

    Set<Pair<State1, State2>> aS = new HashSet<>();

    for (State1 s1 : nfa1.getVertices())
      for (State2 s2 : nfa2.getVertices()) {
        boolean a1 = nfa1.getAcceptStates().contains(s1);
        boolean a2 = nfa2.getAcceptStates().contains(s2);
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

  static class Edge<P> extends FA.Edge<P> {
    enum SpecialKind {EPSILON, DEFAULT};
    private boolean myEmpty = false;

    Edge(SpecialKind kind) {
      super(null);
      switch (kind) {
        case EPSILON: setDefault(false); myEmpty = true; break;
        case DEFAULT:
        default:
      }
    }

    Edge(P label) {super(label);}

    @Override
    public String toString() {
      if (myEmpty) return "empty"; else return super.toString();
    }

    public boolean isEpsilon() {return myEmpty;}

    @Override
    boolean equalLabels(FA.Edge<P> e) {
      if (e instanceof NFA.Edge && ((Edge) e).myEmpty) return myEmpty;
      if (myEmpty) return false;

      return super.equalLabels(e);
    }
  }
}
