package heuristics.test;

import heuristics.ziround.MMipModel;
import heuristics.ziround.NumVariable;
import heuristics.ziround.ZiRound;
import ilog.concert.IloException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.management.InvalidAttributeValueException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Class created to test utility methods of ZiRound class
 * <p>
 * uses the same models that have been tested to be working with class {@link MMipModel}
 *
 * @author Turcato
 */
@RunWith(Parameterized.class)
public class ZiRoundTest {
    public final static double TEST_THRESHOLD = Double.MIN_VALUE;

    private ZiRound ziRoundHeuristic;

    private double[] b;
    private double[] c;
    private MMipModel.ObjType objType;
    private double[] numSolutions;
    private MMipModel.ExprType[] constraintsExpr;
    private double[][] A;
    private NumVariable[] vars;
    private NumVariable[] solutions;
    private int[] integerSolutions;
    private double threshold;
    private MMipModel testModel;


    @Parameterized.Parameters
    public static Collection<Object[]> data() throws NumVariable.ValueOutOfBoundsException, InvalidAttributeValueException {
        return MMipModelTest.data();
    }


    public ZiRoundTest(NumVariable[] vars, double[][] multiplierMatrix, MMipModel.ExprType[] constraintsExpr, double[] b, double[] c, MMipModel.ObjType objType, double[] solutions) {
        this.vars = vars;
        this.A = multiplierMatrix;
        this.constraintsExpr = constraintsExpr;
        this.b = b;
        this.c = c;
        this.objType = objType;
        this.numSolutions = solutions;
        //TODO testModel = new Model();
        testModel.setVariables(vars);
        testModel.setConstraints(A, b, constraintsExpr);
        testModel.setObjective(objType, c);
        testModel.setRelaxed();
        try {
            for (int j = 0; j < testModel.countNumVariables(); j++)
                testModel.getVariable(j).setValue(numSolutions[j]);
        } catch (InvalidAttributeValueException e) {
            //won't happen since model was relaxed
            fail();
        } catch (NumVariable.ValueOutOfBoundsException e) {
            //won't happen as long as the given solutions are within the variables' bounds
            fail();
        }

        this.solutions = testModel.getVariables();
        //ziRoundHeuristic = new ZiRound(testModel, TEST_THRESHOLD);
        //TODO fix this test

    }

    @Before
    public void setUP() {
        //TODO ziRoundHeuristic.setIntegerSolutions();
        testModel.changeConstraintsToLessThan();
    }

    @Test
    public void getSlackUB() throws IloException {
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            double minSlack = -1;
            //The slacks are always >= 0 if the constraints are satisfied
            for (int i = 0; i < testModel.countConstraints(); i++) {
                if (testModel.getConstraintsMultiplier(i, j) > 0) {
                    if (minSlack == -1)
                        minSlack = testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j);
                    if (testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j) < minSlack)
                        minSlack = testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j);
                }
            }
            minSlack = (minSlack == -1 ? 0 : minSlack);
            assertEquals(minSlack, ziRoundHeuristic.getSlackUB(j));
        }
    }

    @Test
    public void getSlackLB() throws IloException {
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            double minSlack = -1;
            //The slacks are always >= 0 if the constraints are satisfied
            for (int i = 0; i < testModel.countConstraints(); i++) {
                if (testModel.getConstraintsMultiplier(i, j) < 0) {
                    if (minSlack == -1)
                        minSlack = -testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j);
                    if (-testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j) < minSlack)
                        minSlack = -testModel.getConstraintSlack(i) / testModel.getConstraintsMultiplier(i, j);
                }
            }
            minSlack = (minSlack == -1 ? 0 : minSlack);
            assertEquals(minSlack, ziRoundHeuristic.getSlackLB(j));
        }
    }

    @Test
    public void getZis() {
    }

    @Test
    public void sumZis() {
    }

    @Test
    public void computeZI() {
    }

    @Test
    public void testComputeZI() {
    }
}