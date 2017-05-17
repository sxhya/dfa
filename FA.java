import java.util.*;

/**
 * Created by user on 5/12/17.
 */
public abstract class FA<State, Alphabet> extends DirectedGraph<State, FA.Edge<Alphabet>> {
  public enum ProductAnnotation {OR, AND, MINUS}

  ;

  private State initialState;
  private Set<State> acceptStates = new HashSet<>();

  public FA() {
  }

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

  State getInitialState() {
    return initialState;
  }

  void setAcceptStates(Set<State> acceptStates) {
    this.acceptStates = acceptStates;
  }

  Set<State> getAcceptStates() {
    return acceptStates;
  }

  void addAcceptState(State acceptState) {
    this.acceptStates.add(acceptState);
  }

  void addTransition(State initialState, Alphabet symbol, State resultingState) {
    addUniqueEdge(new Edge<>(symbol), initialState, resultingState);
  }

  void addDefaultTransition(State initialState, State resultingState) {
    addUniqueEdge(new Edge<>(Edge.SpecialKind.DEFAULT), initialState, resultingState);
  }

  public void addUniqueEdge(Edge<Alphabet> e, State from, State to) {
    if (e.isEpsilon() && from.equals(to)) return;
    for (FA.Edge<Alphabet> oe : getOutboundEdges(from))
      if (e.equalLabels(oe) && getCodomain(oe).equals(to)) return;

    addEdge(e, from, to);
  }

  boolean acceptedStateAttainable() {
    List<List<State>> components = this.getOrderedVertices(initialState, false);
    if (components.isEmpty()) return false;
    for (State v : components.get(0)) if (acceptStates.contains(v)) return true;
    return false;
  }

  void purgeUnattainableStates() {
    List<List<State>> components = this.getOrderedVertices(initialState, false);
    for (int i = 1; i < components.size(); i++)
      for (State v : components.get(i))
        removeVertex(v);
  }

  boolean allGoingTo(State s1, State s2) {
    Set<FA.Edge<Alphabet>> es = getOutboundEdges(s1);
    boolean result = es.size() > 0;
    for (FA.Edge<Alphabet> e2 : es)
      if (!getCodomain(e2).equals(s2)) {
        result = false;
        break;
      }
    return result;
  }

  private void glueIfDeadEnd(State s) {
    List<State> result = this.getOrderedVertices(s, true).iterator().next();
    boolean accept = false;
    for (State as : acceptStates) if (result.contains(as)) return;
    if (result.size() > 1) {
      Iterator<State> sx = result.iterator();
      State s1 = sx.next();
      while (sx.hasNext()) {
        State s2 = sx.next();
        State s3;
        for (FA.Edge<Alphabet> e : getInboundEdges(s2)) if (!result.contains(s3 = getDomain(e))) {
          removeEdge(e);
          addUniqueEdge(e.copy(), s3, s1);
        }
      }
      sx = result.iterator();
      sx.next();
      while (sx.hasNext()) {removeVertex(sx.next());}
      for (FA.Edge<Alphabet> e : getOutboundEdges(s1)) removeEdge(e);
      addDefaultTransition(s1, s1);
    }
  }

  void simplify() { //some heuristic nfa simplification
    int vC;
    do {
      vC = getVertices().size();


      //Step 1: Purge unattainable states
      purgeUnattainableStates();

      //Step 2: Purge dead branches
      for (State s : getVertices()) glueIfDeadEnd(s);

      //Step 3: Identify "devil" vertices
      FA.Edge<Alphabet> e;
      Set<State> devilVertices = new HashSet<>();
      for (State v : getVertices()) if (allGoingTo(v, v) && !acceptStates.contains(v)) devilVertices.add(v);

      if (devilVertices.size() >= 1) {
        Iterator<State> vs = devilVertices.iterator();
        State devil = vs.next();
        while (vs.hasNext()) {
          State v2 = vs.next();
          for (FA.Edge<Alphabet> e2 : getOutboundEdges(v2)) removeEdge(e2);
          for (FA.Edge<Alphabet> e2 : getInboundEdges(v2)) {
            State v3 = getDomain(e2);
            removeEdge(e2);
            addUniqueEdge(e2, v3, devil);
          }
          removeVertex(v2);
        }

        for (FA.Edge<Alphabet> i : getInboundEdges(devil)) {
          boolean hasOtherEdges = false;
          for (FA.Edge<Alphabet> e2 : getOutboundEdges(getDomain(i)))
            if (e2.equalLabels(i) && !e2.equals(i)) {
              hasOtherEdges = true;
              break;
            }
          if (hasOtherEdges) removeEdge(i);
        }
      }

      //Step 4: Remove explicit edges when default edge has the same effect
      for (State s : getVertices()) {
        Map<Alphabet, Set<State>> labelStates = new HashMap<>();
        Set<State> defaultStates = new HashSet<>();
        Set<Alphabet> purgeLabels = new HashSet<>();
        for (FA.Edge<Alphabet> e2 : getOutboundEdges(s))
          if (e2.isDefault()) defaultStates.add(getCodomain(e2)); else
            labelStates.computeIfAbsent(e2.getLabel(), k -> new HashSet<>()).add(getCodomain(e2));
        for (Alphabet a : labelStates.keySet())
          if (defaultStates.equals(labelStates.get(a)))
            purgeLabels.add(a);
        for (Alphabet a : purgeLabels)
          for (FA.Edge<Alphabet> e2 : getOutboundEdges(s))
            if (e2.getLabel() != null && e2.getLabel().equals(a)) removeEdge(e2);
      }

      //Step 5: Identify equivalent states
      Map<Pair<Pair<Map<Alphabet, Set<State>>, Set<State>>, Boolean>, Set<State>> glueData = new HashMap<>();

      for (State s : getVertices()) {
        Set<State> defaultCodomains = new HashSet<>();
        Map<Alphabet, Set<State>> labelEdges = new HashMap<>();
        for (FA.Edge<Alphabet> e2 : getOutboundEdges(s))
          if (e2.isDefault()) defaultCodomains.add(getCodomain(e2)); else if (!e2.isEpsilon())
            labelEdges.computeIfAbsent(e2.getLabel(), k -> new HashSet<>()).add(getCodomain(e2));
        Pair<Pair<Map<Alphabet, Set<State>>, Set<State>>, Boolean> key = new Pair<>(new Pair<>(labelEdges, defaultCodomains), acceptStates.contains(s));
        glueData.computeIfAbsent(key, k -> new HashSet<State>()).add(s);
      }

      for (Object key : glueData.keySet()) {
        Iterator<State> it = glueData.get(key).iterator();
        State s1 = it.next();
        while (it.hasNext()) {
          State s2 = it.next();
          if (initialState.equals(s2)) setInitialState(s1);

          for (FA.Edge<Alphabet> e2 : getInboundEdges(s2)) {
            State s3 = getDomain(e2);
            removeEdge(e2);
            addUniqueEdge(e2.copy(), s3, s1);
          }

          for (FA.Edge<Alphabet> e2 : getOutboundEdges(s2)) {
            State s3 = getCodomain(e2);
            removeEdge(e2);
            addUniqueEdge(e2.copy(), s1, s3);
          }

          removeVertex(s2);
        }
      }

    } while (getVertices().size() < vC);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    List<List<State>> components = getOrderedVertices(getInitialState(), false);
    Map<State, Integer> indices = new HashMap<State, Integer>();
    int counter = 0;
    for (int i = 0; i < components.size(); i++) {
      int len = 0;
      boolean isLast = i == components.size() - 1;
      List<State> component = components.get(i);
      for (State s : component) {
        indices.put(s, counter++);
        int cL = String.valueOf(counter - 1).length();
        if (getInitialState().equals(s)) cL += 4;
        if (getAcceptStates().contains(s)) cL += 3;
        if (cL > len) len = cL;
      }

      for (State s : component) {
        StringBuilder label = new StringBuilder();
        if (s.equals(getInitialState())) label = new StringBuilder("(>) ");
        int index = indices.get(s);
        label.append(String.valueOf(index));
        if (getAcceptStates().contains(s)) label.append(" (✓)");
        while (label.length() <= len) label.append(" ");
        result.append("| ").append(label).append(" | ");

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

        Map<Set<State>, List<FA.Edge<Alphabet>>> coc = new LinkedHashMap<>();
        for (Set<Edge<Alphabet>> se : equalityClasses)  {
          Set<State> cds = new HashSet<>();
          for (Edge<Alphabet> e2 : se) cds.add(getCodomain(e2));
          coc.computeIfAbsent(cds, k -> new ArrayList<>()).add(se.iterator().next());
        }

        Map<String, Set<Edge<Alphabet>>> m = new HashMap<>();

        for (Set<State> es : coc.keySet()) {
          int cl = coc.get(es).size() - 1;
          if (cl > 0) result.append("{");
          for (FA.Edge<Alphabet> e2 : coc.get(es)) {
            result.append(e2.toString());
            if (cl > 0)
              result.append(", ");
            cl--;
          }
          if (coc.get(es).size() > 1) result.append("}");

          result.append(" -> ");

          cl = es.size() - 1;
          if (cl > 0) result.append("{");
          for (State s2 : es) {
            index = indices.get(s2);
            result.append(String.valueOf(index));
            if (cl > 0) result.append(", ");
            cl--;
          }
          if (es.size() > 1) result.append("}");

          result.append("; ");
        }

        result.append("\n");
      }

      if (!isLast) result.append("=== next component === \n");
    }

    return result.toString();
  }

  static class Edge<P> {
    enum SpecialKind {EPSILON, DEFAULT}

    ;
    private P label = null;
    private boolean myDefault = false;
    private boolean myEmpty = false;

    Edge(P l) {
      this.label = l;
    }

    Edge(SpecialKind kind) {
      switch (kind) {
        case EPSILON:
          myEmpty = true;
          break;
        case DEFAULT:
        default:
          myDefault = true;
          break;
      }
    }

    @Override
    public String toString() {
      if (myDefault) return "default";
      else if (myEmpty) return "ε";
      else return label.toString();
    }

    P getLabel() {
      return label;
    }

    boolean isDefault() {
      return myDefault;
    }

    boolean isEpsilon() {
      return myEmpty;
    }

    void setDefault(boolean d) {
      myDefault = d;
    }

    void setEpsilon(boolean d) {
      myEmpty = d;
    }

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
