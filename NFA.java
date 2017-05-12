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
