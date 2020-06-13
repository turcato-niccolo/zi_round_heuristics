package heuristics.ziround;

import heuristics.test.CplexUtilsTest;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.management.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.Collection;

import static heuristics.test.ZiRoundTest.TEST_THRESHOLD;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(Parameterized.class)
public class ZiRoundHeuristicTest {
    private static final int MAX_MODELS = 10;
    public static int executionCount = 0;
    public static final double DELTA = 0.01;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws NumVariable.ValueOutOfBoundsException, InvalidAttributeValueException {
        Collection<Object[]> models = new ArrayList<>();
        Object[] mipModels = CplexUtilsTest.data().toArray();

        for (int j = 0; j < mipModels.length && j < MAX_MODELS; j++) {
            for (Object content : mipModels)
                if (content instanceof String) {
                    IloCplex instance;
                    try {
                        instance = new IloCplex();
                        instance.importModel((String) content);

                        models.add(new Object[]{instance});

                    } catch (IloException e) {
                        e.printStackTrace();
                    }
                }
        }
        return models;
    }

    private ZiRound ziRoundHeuristic;


    private double[] b;
    private double[] c;
    private double[] numSolutions;
    private Model testModel;

    private int[] integerConstraints;

    public ZiRoundHeuristicTest(IloCplex cplexTestModel) throws IloException {
        testModel = new Model(cplexTestModel);
        this.numSolutions = testModel.getSolutions();
        integerConstraints = testModel.getIntegerConstraints();


    }

    @Before
    public void setUP() throws IloException {
        ziRoundHeuristic = new ZiRound(testModel, TEST_THRESHOLD);
        ziRoundHeuristic.setIntegerSolutions(integerConstraints);
    }

    @Test
    public void RunAlgorithm() throws IloException {
        if (executionCount++ == 1) {
            executionCount = executionCount;
        }

        try {
            ziRoundHeuristic.applyHeuristic();
        } catch (InvalidAttributeValueException | NumVariable.ValueOutOfBoundsException e) {
            fail();
        } catch (IloException e) {
            e.printStackTrace();
            fail();
        }
        NumVariable[] solutions = ziRoundHeuristic.Solutions();

        //Checks if the manipulated solutions are Integers
        for (int j = 0; j < integerConstraints.length; j++) {
            assertTrue(solutions[integerConstraints[j]].isInt());
        }
    }
}