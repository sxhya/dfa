import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by sxh on 12.05.17.
 */
public class Tests {
    @Test
    public void test1() {
        DFA<String, Integer> dfa1 = new DFA<>();
        DFA<String, Integer> dfa2 = new DFA<>();
        DFA dfap;

        dfa1.setInitialState("s1");
        dfa1.addAcceptState("t1");

        dfa2.setInitialState("s2");
        dfa2.addAcceptState("t21");
        dfa2.addAcceptState("t22");

        dfa1.addTransition("s1",0,"s1");
        dfa1.addTransition("s1",1,"t1");
        dfa1.addDefaultTransition("t1", "t1");

        dfa2.addTransition("s2",1,"s2");
        dfa2.addTransition("s2",0,"q2");
        dfa2.addTransition("q2",0,"q2");
        dfa2.addTransition("q2",1,"t21");
        dfa2.addDefaultTransition("t21", "t21");
        dfa2.addDefaultTransition("t22", "t22");

        List<Integer> state = new ArrayList<Integer>();
        state.add(0);

        dfap = DFA.directProduct(dfa1, dfa2, DFA.ProductAnnotation.AND);

        System.out.println(dfa1);
        System.out.println(dfa2);
        System.out.println(dfap);

        assert (!DFA.runDFA(dfa1, state));

        state.clear(); state.add(1);

        assert (DFA.runDFA(dfa1, state));
        assert (!DFA.runDFA(dfap, state));

        state.clear(); state.add(0); state.add(1);

        assert (DFA.runDFA(dfap, state));
    }
}
