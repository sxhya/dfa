import java.util.*;

/**
 * Created by sxh on 12.05.17.
 */
public class NFA<State, Alphabet> extends FA<State, Alphabet> {

  NFA(NFA<State, Alphabet> nfa) {
    super(nfa);
  }

  NFA() {super();}

  NFA<State, Alphabet> getEpsilonClosure() {
    NFA<State, Alphabet> result = new NFA<>(this);
    Set<Edge<Alphabet>> epsilonEdges = result.getEpsilonEdges();
    if (epsilonEdges.isEmpty()) return result;

    //Stage 1: transitive closure of epsilon edges
    Map<State, Set<State>> hasEpsilonEdge = new HashMap<>();
    for (Edge<Alphabet> e : epsilonEdges) hasEpsilonEdge.computeIfAbsent(result.getDomain(e), k -> new HashSet<>()).add(result.getCodomain(e));
    boolean foundNew;
    do {
      foundNew = false;
      Set<Pair<State, State>> appendLater = new HashSet<>();
      for (State s1 : hasEpsilonEdge.keySet())
        for (State s2 : hasEpsilonEdge.get(s1)) if (hasEpsilonEdge.get(s2) != null)
          for (State s3 : hasEpsilonEdge.get(s2)) if (!hasEpsilonEdge.get(s1).contains(s3)) {
            result.addEpsilonTransition(s1, s3);
            appendLater.add(new Pair<>(s1,s3));
            foundNew = true;
          }
      for (Pair<State, State> p : appendLater) hasEpsilonEdge.get(p.a).add(p.b);
    } while (foundNew);

    for (State s : hasEpsilonEdge.keySet())
      for (State s2 : hasEpsilonEdge.get(s))
        result.addEpsilonTransition(s,s2);

    //Stage 2: saturate accept states
    epsilonEdges = result.getEpsilonEdges();
    for (Edge<Alphabet> e : epsilonEdges) if (result.getAcceptStates().contains(result.getCodomain(e))) result.addAcceptState(result.getDomain(e));

    Map<State, Set<Alphabet>> nonDefaultSymbols = new HashMap<State, Set<Alphabet>>();
    Set<State> hasDefaultOutboundEdge = new HashSet<State>();

    for (State v : result.getVertices())
      for (FA.Edge<Alphabet> e : getOutboundEdges(v))
        if (e.isDefault()) hasDefaultOutboundEdge.add(v); else
          if (!e.isEpsilon()) nonDefaultSymbols.computeIfAbsent(v, k -> new HashSet<>()).add(e.getLabel());

    //Stage 3: compose non-epsilon edges with epsilon edges
    for (Edge<Alphabet> e : epsilonEdges)
      for (FA.Edge<Alphabet> o : result.getOutboundEdges(result.getCodomain(e))) {
        State s = result.getDomain(e);
        Set<Alphabet> nonDefault = nonDefaultSymbols.get(s);
        boolean hasDefault = hasDefaultOutboundEdge.contains(s);
        if (!o.isDefault() && !o.isEpsilon() && hasDefault && (nonDefault == null || !nonDefault.contains(o.getLabel()))) {
          for (FA.Edge<Alphabet> ex : result.getOutboundEdges(s))
            if (ex.isDefault()) result.addUniqueEdge(o.copy(), s, result.getCodomain(ex));
        }

        result.addUniqueEdge(o.copy(), s, result.getCodomain(o));
      }
        // this implementation is erroneous


    //Stage 4: purge all epsilon edges
    for (Edge<Alphabet> e : epsilonEdges) result.removeEdge(e);

    result.simplify();

    return result;
  }

  @Override
  void addTransition(State initialState, Alphabet symbol, State resultingState) {
    addUniqueEdge(new FA.Edge<>(symbol), initialState, resultingState);
  }

  @Override
  void addDefaultTransition(State initialState, State resultingState) {
    addUniqueEdge(new FA.Edge<>(Edge.SpecialKind.DEFAULT), initialState, resultingState);
  }

  void addEpsilonTransition(State initialState, State resultingState) {
    addUniqueEdge(new FA.Edge<>(Edge.SpecialKind.EPSILON), initialState, resultingState);
  }

  private Set<Edge<Alphabet>> getEpsilonEdges() {
    Set<Edge<Alphabet>> result = new HashSet<>();
    for (FA.Edge<Alphabet> e : getEdges())
      if (e.isEpsilon()) result.add(e);
    return result;
  }

  boolean containsEpsilonEdges() {
    return !getEpsilonEdges().isEmpty();
  }

  private Set<State> getNewState(Set<State> initialState, Alphabet symbol) {
    Set<FA.Edge<Alphabet>> edges = new HashSet<>();
    for (State s : initialState) edges.addAll(this.getOutboundEdges(s));
    Pair<Map<Alphabet, Set<FA.Edge<Alphabet>>>, Set<FA.Edge<Alphabet>>> outbound = NFA.getLabelMap(edges);
    Set<FA.Edge<Alphabet>> edge = outbound.a.get(symbol);
    if (edge == null) {
      edge = outbound.b;
    }
    assert (edge != null && !edge.isEmpty());
    HashSet<State> result = new HashSet<>();
    for (FA.Edge<Alphabet> e : edge) result.add(this.getCodomain(e));
    return result;
  }

  public boolean runNFA(Iterable<Alphabet> list) {
    Set<State> s = new HashSet<>();
    s.add(this.getInitialState());
    for (Alphabet a : list) s = this.getNewState(s, a);
    for (State st : getAcceptStates()) if (s.contains(st)) return true;
    return false;
  }

  public boolean runNFA_(Alphabet... list) {
    List<Alphabet> lst = new LinkedList<>();
    Collections.addAll(lst, list);
    return runNFA(lst);
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

    result.purgeUnattainableStates();
    result.simplify();

    return result;
  }

  public static<X> NFA<Integer, X> emptyLanguageNFA() {
    NFA<Integer, X> result = new NFA<>();
    result.addVertex(0);
    result.addVertex(1);
    result.addVertex(-1);
    result.addDefaultTransition(-1, -1);
    result.addDefaultTransition(0, -1);
    result.addDefaultTransition(1, -1);
    result.addAcceptState(1);
    result.setInitialState(0);
    return result;
  }

  public static<X> NFA<Integer, X> emptyWordNFA() {
    NFA<Integer, X> result = new NFA<>();
    result.addVertex(1);
    result.addVertex(-1);
    result.addAcceptState(1);
    result.setInitialState(1);
    result.addDefaultTransition(1, -1);
    result.addDefaultTransition(-1, -1);
    return result;
  }

  public static<X> NFA<Integer, X> singleSymbolNFA(X x) {
    NFA<Integer, X> result = emptyLanguageNFA();
    result.addTransition(0, x, 1);
    return result;
  }

  public static<X> NFA<Integer, X> anySymbolNFA() {
    NFA<Integer, X> result = new NFA<>();
    result.addDefaultTransition(0, 1);
    result.addDefaultTransition(1, 2);
    result.addDefaultTransition(2, 2);
    result.setInitialState(0);
    result.addAcceptState(1);
    return result;
  }

  public static<X> NFA<Integer, X> anyWordNFA() {
    NFA<Integer, X> result = new NFA<>();
    result.addVertex(0);
    result.addDefaultTransition(0, 0);
    result.setInitialState(0);
    result.addAcceptState(0);
    return result;
  }

  public static<X, S1, S2> NFA<UnionState<S1, S2>, X> uniteNFA(NFA<S1, X> nfa1, NFA<S2, X> nfa2) {
    NFA<UnionState<S1, S2>, X> result = new NFA<>();
    UnionState<S1, S2> start = UnionState.begin();
    UnionState<S1, S2> end = UnionState.end();
    nfa1.transform(result, UnionState::getX);
    nfa2.transform(result, UnionState::getY);
    result.addEpsilonTransition(start, UnionState.getX(nfa1.getInitialState()));
    result.addEpsilonTransition(start, UnionState.getY(nfa2.getInitialState()));
    result.setInitialState(start);
    result.addAcceptState(end);
    for (S1 s : nfa1.getAcceptStates()) result.addEpsilonTransition(UnionState.getX(s), end);
    for (S2 s : nfa2.getAcceptStates()) result.addEpsilonTransition(UnionState.getY(s), end);
    return result.getEpsilonClosure();
  }

  public static<X, S1, S2> NFA<UnionState<S1, S2>, X> concatNFA(NFA<S1, X> nfa1, NFA<S2, X> nfa2) {
    NFA<UnionState<S1, S2>, X> result = new NFA<>();
    UnionState<S1, S2> start = UnionState.begin();
    UnionState<S1, S2> end = UnionState.end();
    result.setInitialState(start);
    nfa1.transform(result, UnionState::getX);
    result.addEpsilonTransition(start, UnionState.getX(nfa1.getInitialState()));
    nfa2.transform(result, UnionState::getY);
    for (S1 s : nfa1.getAcceptStates()) result.addEpsilonTransition(UnionState.getX(s), UnionState.getY(nfa2.getInitialState()));
    result.addAcceptState(end);
    for (S2 s : nfa2.getAcceptStates()) result.addEpsilonTransition(UnionState.getY(s), end);
    return result.getEpsilonClosure();
  }

  public static<X, S> NFA<Pair<S, Integer>, X> kleeneClosure(NFA<S, X> nfa) {
    NFA<Pair<S, Integer>, X> result = new NFA<>();
    result.setInitialState(new Pair<>(null, 0));
    nfa.transform(result, v -> new Pair<>(v, 1));
    result.addEpsilonTransition(new Pair<>(null, 0), new Pair<>(nfa.getInitialState(), 1));
    for (S s : nfa.getAcceptStates()) result.addEpsilonTransition(new Pair<>(s, 1), new Pair<>(null, 2));
    result.addAcceptState(new Pair<>(null, 2));
    result.addEpsilonTransition(new Pair<>(null, 0), new Pair<>(null, 2));
    result.addEpsilonTransition(new Pair<>(null, 2), new Pair<>(null, 0));
    return result.getEpsilonClosure();
  }

  public static<X> NFA<Integer, X> counter(int min, int max) {
    NFA<Integer, X> result = new NFA<>();
    assert (min>=1 && max >= min);
    result.addVertex(0);
    result.setInitialState(0);
    for (int i=1; i< max; i++) {
      result.addVertex(i);
      result.addDefaultTransition(i-1, i);
      if (i >= min) result.addAcceptState(i);
    }
    result.addVertex(-1);
    result.addDefaultTransition(max-1, -1);
    result.addDefaultTransition(-1, -1);
    return result;
  }
}
