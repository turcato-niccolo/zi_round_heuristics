package heuristics.ziround;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jetbrains.annotations.NotNull;

import javax.management.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper for reading data from {@link IloCplex} instances
 *
 * @author Turcato
 */
public class Model {
    private IloCplex cplex;
    private double[] solutions;
    private IloLPMatrix matrix;
    private IloObjective obj;
    private List<Double> mMultipliers;

    private final static String CONSTRAINT_READ_ERROR = "Impossible to read constraint at row ";
    private final static String VARIABLE_READ_ERROR = "Error reading variable j=";

    /**
     * Builds the object starting from a valid {@link IloCplex} instance
     *
     * @param iloCplexInstance A valid instance containing a model
     */
    public Model(@NotNull IloCplex iloCplexInstance) {
        cplex = iloCplexInstance;
        VariableLister modelLister = new VariableLister();
        try {
            modelLister.parse(cplex);
        } catch (VariableLister.VariableListerException | IloException e) {
            e.printStackTrace();
            //The model might still be correct
        }
        onModelListed(modelLister);
        try {
            solutions = new double[matrix.getNcols()];
        } catch (IloException e) {
            e.printStackTrace();
            //What the fuck
        }
    }

    /**
     * @param lister A valid lister that contains the data from the given {@link IloCplex} instance
     */
    public void onModelListed(VariableLister lister) {
        matrix = lister.getMatrix();
        obj = lister.getObj();
        mMultipliers = new ArrayList<>();
        try {
            if (obj.getExpr() instanceof IloLinearNumExpr) {
                IloLinearNumExpr lexpr = (IloLinearNumExpr) obj.getExpr();
                IloLinearNumExprIterator it = lexpr.linearIterator();

                while (it.hasNext()) {
                    it.nextNumVar();
                    mMultipliers.add(it.getValue());
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes all MORE_THAN and MORE_OR_EQUAL constraints to LESS_THAN and LESS_OR_EQUAL respectively
     */
    public void changeConstraintsToLessThan() throws IloException {
        /*
            IloRange notes:
            for expr == rhs, set lb = ub = rhs
            for expr <= rhs, set lb = -infinity and ub = rhs
            for expr >= rhs, set lb = rhs and ub = infinity
         */
        for (int i = 0; i < matrix.getNrows(); i++) {
            IloRange rangedExpression = matrix.getRange(i);
            if (rangedExpression.getUB() >= Double.POSITIVE_INFINITY) {
                rangedExpression.setBounds(Double.NEGATIVE_INFINITY, -rangedExpression.getUB());
                for (int j = 0; j < matrix.getNrows(); j++) {
                    matrix.setNZ(i, j, -matrix.getNZ(i, j));
                }
            }
        }
    }

    /**
     * Changes all LESS_THAN and LESS_OR_EQUAL constraints to MORE_THAN and MORE_OR_EQUAL respectively
     */
    public void changeConstraintsToMoreThan() throws IloException {
                /*
            IloRange notes:
            for expr == rhs, set lb = ub = rhs
            for expr <= rhs, set lb = -infinity and ub = rhs
            for expr >= rhs, set lb = rhs and ub = infinity
         */
        for (int i = 0; i < matrix.getNrows(); i++) {
            IloRange rangedExpression = matrix.getRange(i);
            if (rangedExpression.getLB() <= Double.NEGATIVE_INFINITY) {
                rangedExpression.setBounds(-rangedExpression.getLB(), Double.POSITIVE_INFINITY);
                for (int j = 0; j < matrix.getNrows(); j++) {
                    matrix.setNZ(i, j, -matrix.getNZ(i, j));
                }
            }
        }
    }

    /**
     * Returns the slacks for the given constraints, if the constraint is a = expression, it will always return 0
     * If the constraint is satisfied in the model, the slack will always be >= 0
     *
     * @param i Index of a constraint
     * @return Current slack of i° constraint
     */
    public double getConstraintSlack(int i) throws IloException {
        IloRange rangedExpression = matrix.getRange(i);
        if (rangedExpression.getLB() == rangedExpression.getUB())
            return 0;
        //value of the left member of the constraint's expression
        double leftValue = 0;
        for (int col = 0; col < matrix.getNrows(); col++) {
            leftValue += matrix.getNZ(i, col) * solutions[col];
        }
        if (rangedExpression.getLB() <= Double.NEGATIVE_INFINITY) {
            return rangedExpression.getUB() - leftValue;
        } else if (rangedExpression.getUB() >= Double.POSITIVE_INFINITY) {
            return leftValue - rangedExpression.getLB();
        }
        return 0;
    }

    /**
     * @return The number of variables of this model
     * @throws NullPointerException If there aren't any variables
     */
    public int countNumVariables() throws NullPointerException, IloException {
        return matrix.getNcols();
    }

    /**
     * @return The number of constraints of thi model
     */
    public int countConstraints() throws IloException {
        return matrix.getNrows();
    }

    /**
     * @return The type of objective function {max, min}
     */
    public ObjType getObjType() throws IloException {
        if (obj.getSense() == IloObjectiveSense.Maximize)
            return ObjType.MAX;
        else
            return ObjType.MIN;
    }

    public double getExpressionValue(int i) throws IloException {
        IloRange rangedExpression = matrix.getRange(i);
        if (rangedExpression.getUB() == rangedExpression.getLB())
            return rangedExpression.getUB();
        else if (rangedExpression.getLB() <= Double.NEGATIVE_INFINITY) {
            return rangedExpression.getUB();
        } else if (rangedExpression.getUB() >= Double.POSITIVE_INFINITY) {
            return rangedExpression.getLB();
        }
        throw new IloException(CONSTRAINT_READ_ERROR + i);
    }

    public double getObjMultiplier(int i) throws IloException {
        return mMultipliers.get(i);
    }

    /**
     * @param i #Row
     * @param j #Column
     * @return The multiplier a row i and column j of the constraints matrix
     */
    public double getConstraintsMultiplier(int i, int j) throws IloException {
        return matrix.getNZ(i, j);
    }

    public ExprType getExprType(int i) throws IloException {
        IloRange rangedExpression = matrix.getRange(i);
        if (rangedExpression.getUB() == rangedExpression.getLB())
            return ExprType.EQUAL;
        else if (rangedExpression.getLB() <= Double.NEGATIVE_INFINITY) {
            return ExprType.LESS_OR_EQUAL;
        } else if (rangedExpression.getUB() >= Double.POSITIVE_INFINITY) {
            return ExprType.MORE_OR_EQUAL;
        }
        throw new IloException(CONSTRAINT_READ_ERROR + i);
    }

    /**
     * @param j Index of a variable
     * @return The variable at index j
     * @throws IloException If the variable doesn't exist
     */
    public NumVariable getVariable(int j) throws IloException {
        IloNumVar variable = matrix.getNumVar(j);
        NumVariable.VarType type = null;
        if (variable.getType().equals(IloNumVarType.Int))
            type = NumVariable.VarType.INT;
        else if (variable.getType().equals(IloNumVarType.Float))
            type = NumVariable.VarType.REAL;
        NumVariable var = null;
        try {
            var = new NumVariable(type, solutions[j], variable.getUB(), variable.getLB());
        } catch (InvalidAttributeValueException | NumVariable.ValueOutOfBoundsException e) {
            System.out.print(VARIABLE_READ_ERROR + j);
            e.printStackTrace();
        }
        return var;
    }

    public NumVariable[] getVariables() throws IloException {
        NumVariable[] vars = new NumVariable[countNumVariables()];
        for (int i = 0; i < countNumVariables(); i++) {
            vars[i] = getVariable(i);
        }
        return vars;
    }

    /**
     * @return An {@code int[]} array containing the indexes of the variables that are constrained to INT
     */
    public int[] getIntegerConstraints() throws IloException {
        List<Integer> integerConstraints = new ArrayList<>();
        for (int j = 0; j < countNumVariables(); j++) {
            if (matrix.getNumVar(j).getType().equals(IloNumVarType.Int))
                integerConstraints.add(j);
        }
        int[] returnArray = new int[integerConstraints.size()];
        int i = 0;
        for (int intConstraint : integerConstraints) {
            returnArray[i++] = intConstraint;
        }
        return returnArray;
    }

    /**
     * @return The model's solutions (if they exist), otherwise {@code null}
     */
    public double[] getSolutions() throws IloException {
        if (cplex.solve()) {
            solutions = cplex.getValues(matrix.getNumVars());
            return solutions;
        }
        return null;
    }

    /**
     * @return The int-relaxed model's solutions (if they exist), otherwise {@code null}
     */
    public double[] getRelaxedSolutions() throws IloException {
        cplex.setParam(IloCplex.Param.MIP.Limits.Nodes, 0); //setting to relaxed
        if (cplex.solve()) {
            solutions = cplex.getValues(matrix.getNumVars());
            return solutions;
        }
        return null;
    }


    public enum ExprType {
        MORE_THAN,
        MORE_OR_EQUAL,
        EQUAL,
        LESS_OR_EQUAL,
        LESS_THAN
    }

    public enum ObjType {
        MIN,
        MAX
    }
}
