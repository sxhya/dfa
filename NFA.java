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

    System.out.println("INPUT:");
    System.out.println(result);

    //Stage 1: transitive closure of epsilon edges
    Map<State, Set<State>> hasEpsilonEdge = new HashMap<>();
    for (Edge<Alphabet> e : epsilonEdges) hasEpsilonEdge.computeIfAbsent(result.getDomain(e), k -> new HashSet<>()).add(result.getCodomain(e));
    boolean foundNew;
    do {
      foundNew = false;
      for (State s1 : hasEpsilonEdge.keySet())
        for (State s2 : hasEpsilonEdge.get(s1)) if (hasEpsilonEdge.get(s2) != null)
          for (State s3 : hasEpsilonEdge.get(s2)) if (!hasEpsilonEdge.get(s1).contains(s3)) {
            result.addEpsilonTransition(s1, s3);
            hasEpsilonEdge.get(s1).add(s3);
            foundNew = true;
          }
    } while (foundNew);

    System.out.println("After Stage 1:");
    System.out.println(result);

    //Stage 2: saturate accept states
    epsilonEdges = result.getEpsilonEdges();
    for (Edge<Alphabet> e : epsilonEdges) if (result.getAcceptStates().contains(result.getCodomain(e))) result.addAcceptState(result.getDomain(e));

    System.out.println("After Stage 2:");
    System.out.println(result);

    //Stage 3: compose non-epsilon edges with epsilon edges
    for (Edge<Alphabet> e : epsilonEdges)
      for (FA.Edge<Alphabet> o : result.getOutboundEdges(result.getCodomain(e)))
        result.addEdge(o.copy(), result.getDomain(e), result.getCodomain(o));

    System.out.println("After Stage 3:");
    System.out.println(result);

    //Stage 4: purge all epsilon edges
    for (Edge<Alphabet> e : epsilonEdges) result.removeEdge(e);

    result.purgeUnattainableStates();

    System.out.println("After all stages:");
    System.out.println(result);

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
    // no epsilon loops are allowed
    if (initialState.equals(resultingState)) return;

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

  public static<X> NFA<Boolean, X> emptyNFA() {
    NFA<Boolean, X> result = new NFA<>();
    result.addVertex(true);
    result.addVertex(false);
    result.addDefaultTransition(false, false);
    result.addAcceptState(true);
    result.setInitialState(false);
    return result;
  }

  public static<X> NFA<Void, X> zeroNFA() {
    NFA<Void, X> result = new NFA<>();
    result.addVertex(null);
    result.addAcceptState(null);
    result.setInitialState(null);
    return result;
  }

  public static<X> NFA<Boolean, X> simpleNFA(X x) {
    NFA<Boolean, X> result = emptyNFA();
    result.addTransition(false, x, true);
    return result;
  }

  public static<X, S1, S2> NFA<UniteState<S1, S2>, X> uniteNFA(NFA<S1, X> nfa1, NFA<S2, X> nfa2) {
    NFA<UniteState<S1, S2>, X> result = new NFA<>();
    UniteState<S1, S2> start = UniteState.start();
    UniteState<S1, S2> end = UniteState.stop();
    nfa1.transform(result, UniteState::getX);
    nfa2.transform(result, UniteState::getY);
    result.addEpsilonTransition(start, UniteState.getX(nfa1.getInitialState()));
    result.addEpsilonTransition(start, UniteState.getY(nfa2.getInitialState()));
    result.setInitialState(start);
    result.addAcceptState(end);
    for (S1 s : nfa1.getAcceptStates()) result.addEpsilonTransition(UniteState.getX(s), end);
    for (S2 s : nfa2.getAcceptStates()) result.addEpsilonTransition(UniteState.getY(s), end);
    return result.getEpsilonClosure();
  }

  public static<X, S1, S2> NFA<UniteState<S1, S2>, X> concatNFA(NFA<S1, X> nfa1, NFA<S2, X> nfa2) {
    NFA<UniteState<S1, S2>, X> result = new NFA<>();
    UniteState<S1, S2> start = UniteState.start();
    UniteState<S1, S2> end = UniteState.stop();
    result.setInitialState(start);
    nfa1.transform(result, UniteState::getX);
    result.addEpsilonTransition(start, UniteState.getX(nfa1.getInitialState()));
    nfa2.transform(result, UniteState::getY);
    for (S1 s : nfa1.getAcceptStates()) result.addEpsilonTransition(UniteState.getX(s), UniteState.getY(nfa2.getInitialState()));
    result.addAcceptState(end);
    for (S2 s : nfa2.getAcceptStates()) result.addEpsilonTransition(UniteState.getY(s), end);
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

  enum OperationState {INIT, WORKING, DONE}

  static class UniteState<X, Y> {
    X x = null;
    Y y = null;
    OperationState state = null;

    UniteState(X x, Y y) {
      this.x = x;
      this.y = y;
      state = OperationState.WORKING;
    }

    UniteState(OperationState state) {
      this.state = state;
    }

    static<X1, Y1> UniteState getX(X1 x) {
      return new UniteState<X1, Y1>(x, null);
    }

    static<X1, Y1> UniteState getY(Y1 y) {
      return new UniteState<X1, Y1>(null, y);
    }

    static<X1, Y1> UniteState start() {
      return new UniteState(OperationState.INIT);
    }

    static<X1, Y1> UniteState stop() {
      return new UniteState(OperationState.DONE);
    }

    @Override
    public int hashCode() {
      int result = state.hashCode()*31;
      if (x != null) result += x.hashCode();
      result *= 31;
      if (y != null) result += y.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof UniteState) {
        UniteState us = (UniteState) o;
        if (state.equals(us.state)) {
          if (!state.equals(OperationState.WORKING)) return true;
          if (x != null && x.equals(us.x)) {
            assert (y == null && us.y == null);
            return true;
          }
          if (y != null && y.equals(us.y)) {
            assert (x == null && us.x == null);
            return true;
          }
        }
        return false;
      }
      return false;
    }

    @Override
    public String toString() {
      switch (state) {
        case INIT: return "INIT";
        case DONE: return "DONE";
        default:
      }
      if (x != null) return "[L "+x.toString() + "]";
      if (y != null) return "[R "+y.toString() + "]";
      return "NULL";
    }
  }

}
