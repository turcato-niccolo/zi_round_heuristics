package heuristics.test;

import heuristics.ziround.LPUtils;
import heuristics.ziround.MMipModel;
import heuristics.ziround.MMipModel.ExprType;
import heuristics.ziround.NumVariable;
import ilog.concert.IloException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.management.InvalidAttributeValueException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * min 2 x1 − x2
 * x1 + 2 x2 ≥ 7
 * 2 x1 − x2 ≥ −6
 * −3x1 + 2x2 ≥ 8
 * x1, x2 ≥ 0
 * <p>
 * x1, x2 <= 100
 * <p>
 * A:   <>  b
 * 1   2   >= 7
 * 2  -1   >= -6
 * -3   2   >= 8
 * <p>
 * SOLUTION: [0, 6]
 *
 * @author Turcato
 */
@RunWith(Parameterized.class)
public class MMipModelTest {
    private static final int MAX_MODELS = 10;
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws NumVariable.ValueOutOfBoundsException, InvalidAttributeValueException {
        Object[] mipModels = CplexUtilsTest.data().toArray();
        ArrayList<MMipModel> models = new ArrayList<>();
        for (int j = 0; j < mipModels.length && j < MAX_MODELS; j++) {
            Object[] o = (Object[]) mipModels[j];
            for (Object content : o)
                if (content instanceof String) {
                    try {
                        models.add(LPUtils.fromMPS((String) content));
                    } catch (IloException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    }
                }
        }
        Object[][] myData = new Object[][]{
                {
                        //Variables
                        new NumVariable[]{
                                new NumVariable(NumVariable.VarType.INT, 0, 100, 0),
                                new NumVariable(NumVariable.VarType.INT, 0, 100, 0),
                        },
                        //A matrix
                        new double[][]{
                                {1, 2},
                                {2, -1},
                                {-3, 2}
                        },
                        //Constraints' expressions' verses
                        new MMipModel.ExprType[]{
                                MMipModel.ExprType.MORE_OR_EQUAL,
                                MMipModel.ExprType.MORE_OR_EQUAL,
                                MMipModel.ExprType.MORE_OR_EQUAL
                        },
                        //Vector b
                        new double[]{7, -6, 8},
                        //Vector c
                        new double[]{2, -1},
                        //Obj function type
                        MMipModel.ObjType.MIN,
                        //Known solutions
                        new double[]{0, 6}
                },
                {
                        new NumVariable[]{
                                new NumVariable(NumVariable.VarType.INT, 0, Integer.MAX_VALUE, Integer.MIN_VALUE),
                                new NumVariable(NumVariable.VarType.INT, 0, Integer.MAX_VALUE, 0),
                                new NumVariable(NumVariable.VarType.INT, 0, Integer.MAX_VALUE, 0),
                        },
                        new double[][]{
                                {-1, -1, -2},
                                {-2, 1, 0},
                                {0, 2, 0},
                                {2, 0, -1}
                        },
                        new MMipModel.ExprType[]{
                                ExprType.LESS_OR_EQUAL,
                                ExprType.LESS_OR_EQUAL,
                                ExprType.MORE_OR_EQUAL,
                                ExprType.EQUAL
                        },
                        new double[]{1, 2, -3, 2},
                        new double[]{0, 1, -1},
                        MMipModel.ObjType.MAX,
                        new double[]{1, 4, 0},
                }};
        Object[][] data = new Object[models.size() + myData.length][myData[0].length];
        for (int i = 0; i < data.length; i++) {
            if (i < models.size())
                data[i] = new Object[]{
                        models.get(i).getVariables(),
                        models.get(i).getMultiplierMatrix(),
                        models.get(i).getConstraintsExprTypes(),
                        models.get(i).getConstraintsExpressionsValues(),
                        models.get(i).getObjMultipliers(),
                        models.get(i).getObjType(),
                        //Solutions
                        new double[0]
                };
            else {
                data[i] = myData[i - models.size()];
            }
        }
        models = null;

        return Arrays.asList(data);
        //Add models to this method
    }

    private double[] solutions;


    MMipModel testModel;
    NumVariable[] vars;
    MMipModel.ExprType[] constraintsExpr;
    double[] b;
    double[][] A;
    double[] c;
    MMipModel.ObjType objType;

    public MMipModelTest(NumVariable[] vars, double[][] multiplierMatrix, ExprType[] constraintsExpr, double[] b, double[] c, MMipModel.ObjType objType, double[] solutions) {
        this.vars = vars;
        this.A = multiplierMatrix;
        this.constraintsExpr = constraintsExpr;
        this.b = b;
        this.c = c;
        this.objType = objType;
        this.solutions = solutions;
        testModel = new MMipModel();
        testModel.setVariables(vars);
        testModel.setConstraints(A, b, constraintsExpr);
        testModel.setObjective(objType, c);
    }

    @Test
    public void setVariables() {
        MMipModel constructionTestModel = new MMipModel();
        assertTrue(constructionTestModel.setVariables(vars));
    }

    @Test
    public void setConstraints() {
        MMipModel constructionTestModel = new MMipModel();
        constructionTestModel.setVariables(vars);
        //Must have variables defined
        assertTrue(constructionTestModel.setConstraints(A, b, constraintsExpr));
    }

    @Test
    public void setObjective() {
        MMipModel constructionTestModel = new MMipModel();
        constructionTestModel.setVariables(vars);
        //Must have variables defined
        assertTrue(constructionTestModel.setObjective(MMipModel.ObjType.MIN, c));
    }

    @Test
    public void getObjType() {
        assertEquals(objType, testModel.getObjType());
    }

    @Test
    public void getNumVariables() {
        assertEquals(vars.length, testModel.countNumVariables());
    }

    @Test
    public void setRelaxed() {
        testModel.setRelaxed();
        for (NumVariable var : testModel.getVariables()) {
            assertEquals(NumVariable.VarType.REAL, var.getType());
        }
    }

    @Test
    public void setIntegerConstraints() {
        int[] intConstraints = new int[]{1};
        testModel.setRelaxed();
        testModel.setIntegerConstraints(intConstraints);

        List<Integer> intConstraintsIndex = new ArrayList<>();
        for (int index : intConstraints) {
            intConstraintsIndex.add(index);
        }
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            if (intConstraintsIndex.contains(j))
                assertEquals(NumVariable.VarType.INT, testModel.getVariable(j).getType());
            else
                assertEquals(NumVariable.VarType.REAL, testModel.getVariable(j).getType());
        }

    }

    @Test
    public void changeConstraintsToLessThan() {
        ExprType[] oldExpressions = new ExprType[testModel.countConstraints()];
        System.arraycopy(testModel.getConstraintsExprTypes(), 0, oldExpressions, 0, testModel.countConstraints());

        double[][] oldConstraintsMatrix = new double[testModel.countConstraints()][testModel.countNumVariables()];
        for (int i = 0; i < testModel.countConstraints(); i++) {
            System.arraycopy(testModel.getMultiplierMatrix()[i], 0, oldConstraintsMatrix[i], 0, testModel.countNumVariables());
        }

        double[] oldExprValue = new double[testModel.countConstraints()];
        System.arraycopy(testModel.getConstraintsExpressionsValues(), 0, oldExprValue, 0, testModel.countConstraints());

        testModel.changeConstraintsToLessThan();
        for (MMipModel.ExprType type : testModel.getConstraintsExprTypes()) {
            assertTrue(type.equals(ExprType.LESS_OR_EQUAL) || type.equals(ExprType.LESS_THAN)
                    || type.equals(ExprType.EQUAL));
        }

        assertTrue(checkConstraintsChanged(oldExpressions, oldConstraintsMatrix, oldExprValue));
    }

    /**
     * returns true if the given old constraints have changed in the current testModel accordingly with a change of sign in the Inequation
     * Ignores equations
     */
    public boolean checkConstraintsChanged(ExprType[] oldExpressions, double[][] oldConstraintsMatrix, double[] oldExprValue) {
        for (int i = 0; i < testModel.countConstraints(); i++) {
            if (!testModel.getExprType(i).equals(oldExpressions[i]) && !testModel.getExprType(i).equals(ExprType.EQUAL)) {
                int j = 0;
                for (double multiplier : testModel.getMultiplierMatrix()[i]) {
                    if (!(multiplier == -oldConstraintsMatrix[i][j++]))
                        return false;
                }
                if (!(testModel.getExpressionValue(i) == -oldExprValue[i]))
                    return false;
            }
        }
        return true;
    }

    @Test
    public void changeConstraintsToMoreThan() {
        ExprType[] oldExpressions = new ExprType[testModel.countConstraints()];
        System.arraycopy(testModel.getConstraintsExprTypes(), 0, oldExpressions, 0, testModel.countConstraints());

        double[][] oldConstraintsMatrix = new double[testModel.countConstraints()][testModel.countNumVariables()];
        for (int i = 0; i < testModel.countConstraints(); i++) {
            System.arraycopy(testModel.getMultiplierMatrix()[i], 0, oldConstraintsMatrix[i], 0, testModel.countNumVariables());
        }

        double[] oldExprValue = new double[testModel.countConstraints()];
        System.arraycopy(testModel.getConstraintsExpressionsValues(), 0, oldExprValue, 0, testModel.countConstraints());

        testModel.changeConstraintsToMoreThan();
        for (MMipModel.ExprType type : testModel.getConstraintsExprTypes()) {
            assertTrue(type.equals(MMipModel.ExprType.MORE_OR_EQUAL) || type.equals(MMipModel.ExprType.MORE_THAN)
                    || type.equals(ExprType.EQUAL));
        }

        assertTrue(checkConstraintsChanged(oldExpressions, oldConstraintsMatrix, oldExprValue));
    }

    @Test
    public void getConstraintSlack() {
        try {
            for (int j = 0; j < testModel.countNumVariables(); j++)
                testModel.getVariable(j).setValue(solutions[j]);
        } catch (InvalidAttributeValueException e) {
            // values are integer
        } catch (NumVariable.ValueOutOfBoundsException e) {
            //Values are out of bounds
            fail();
        }
        double[] slacks = new double[testModel.countConstraints()];

        for (int i = 0; i < testModel.countConstraints(); i++) {
            for (int j = 0; j < testModel.countNumVariables(); j++) {
                try {
                    slacks[i] += testModel.getVariable(j).getValue() * testModel.getConstraintsMultiplier(i, j);
                } catch (IloException e) {
                    e.printStackTrace();
                    fail();
                }
            }
            if (testModel.getExprType(i).equals(MMipModel.ExprType.MORE_OR_EQUAL) || testModel.getExprType(i).equals(MMipModel.ExprType.MORE_THAN))
                slacks[i] = slacks[i] - testModel.getExpressionValue(i);
            else
                slacks[i] = testModel.getExpressionValue(i) - slacks[i];
        }

        int i = 0;
        for (double slack : slacks) {
            assertTrue(testModel.getConstraintSlack(i) >= 0);
            assertTrue(slack >= 0);
            assertEquals(slack, testModel.getConstraintSlack(i++), 0);
        }
    }

    @Test
    public void countNumVariables() {
        assertEquals(vars.length, testModel.countNumVariables());
    }

    @Test
    public void countConstraints() {
        assertEquals(constraintsExpr.length, testModel.countConstraints());
    }

    @Test
    public void getExpressionsValues() {
        assertEquals(b, testModel.getConstraintsExpressionsValues());
    }

    @Test
    public void getExpressionValue() {
        for (int i = 0; i < testModel.countConstraints(); i++) {
            assertEquals(b[i], testModel.getExpressionValue(i), 0);
        }
    }

    @Test
    public void getObjMultipliers() {
        assertEquals(c, testModel.getObjMultipliers());
    }

    @Test
    public void getObjMultiplier() {
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            assertEquals(c[j], testModel.getObjMultiplier(j), 0);
        }
    }

    @Test
    public void getMultiplierMatrix() {
        double[][] matrix = testModel.getMultiplierMatrix();
        for (int i = 0; i < testModel.countConstraints(); i++) {
            assertEquals(A[i], matrix[i]);
        }
    }

    @Test
    public void getConstraintsMultiplier() {
        double[][] matrix = testModel.getMultiplierMatrix();
        for (int i = 0; i < testModel.countConstraints(); i++) {
            for (int j = 0; j < testModel.countNumVariables(); j++) {
                assertEquals(A[i][j], matrix[i][j], 0);
            }
        }
    }

    @Test
    public void getConstraintsExprTypes() {
        ExprType[] exprTypes = testModel.getConstraintsExprTypes();
        for (int i = 0; i < testModel.countConstraints(); i++) {
            assertEquals(constraintsExpr[i], exprTypes[i]);
        }
    }

    @Test
    public void getExprType() {
        for (int i = 0; i < testModel.countConstraints(); i++) {
            assertEquals(constraintsExpr[i], testModel.getExprType(i));
        }
    }

    @Test
    public void getVariables() {
        NumVariable[] variables = testModel.getVariables();
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            assertEquals(vars[j], variables[j]);
        }
    }

    @Test
    public void getVariable() {
        for (int j = 0; j < testModel.countNumVariables(); j++) {
            assertEquals(vars[j], testModel.getVariable(j));
        }
    }

    @Test
    public void getIntegerConstraints() {
        int[] intConstraints = testModel.getIntegerConstraints();
        for (int intConstraint : intConstraints) {
            assertEquals(testModel.getVariable(intConstraint).getType(), NumVariable.VarType.INT);
        }
    }

}