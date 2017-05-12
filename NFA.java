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
    NFA<State, Alphabet> t = this;
    if (containsEpsilonEdges()) t = epsilonClosure();
    // some code:
    return result;
  }

  NFA<State, Alphabet> epsilonClosure() {
    NFA<State, Alphabet> result = new NFA<>();

    return result;
  }

  @Override
  void addTransition(State initialState, Alphabet symbol, State resultingState) {
    super.addEdge(new NFA.Edge<>(symbol), initialState, resultingState);
  }

  @Override
  void addDefaultTransition(State initialState, State resultingState) {
    super.addEdge(new NFA.Edge<>(Edge.SpecialKind.DEFAULT), initialState, resultingState);
  }

  void addEpsilonTransition(State initialState, State resultingState) {
    super.addEdge(new NFA.Edge<>(Edge.SpecialKind.EPSILON), initialState, resultingState);
  }

  @Override
  FA.Edge<Alphabet> copyEdge(FA.Edge<Alphabet> edge) {
    Edge<Alphabet> result = new Edge<>(edge.getLabel());
    result.setDefault(edge.isDefault());
    result.myEmpty = (edge instanceof NFA.Edge) && ((Edge) edge).myEmpty;
    return result;
  }

  boolean containsEpsilonEdges() {
    for (FA.Edge<Alphabet> e : getEdges()) if (e instanceof NFA.Edge && ((Edge) e).isEpsilon()) return true;
    return false;
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
  }
}
