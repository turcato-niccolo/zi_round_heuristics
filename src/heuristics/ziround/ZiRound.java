package heuristics.ziround;

//import heuristics.interfaces.Heuristic;

import heuristics.interfaces.HeuristicListener;
import ilog.concert.IloException;
import org.jetbrains.annotations.NotNull;

import javax.management.InvalidAttributeValueException;

/**
 * This class uses the library {@link ilog.cplex}
 * https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.0/ilog.odms.cplex.help/refjavacplex/html/ilog/cplex/IloCplex.html
 *
 * @author Turcato
 */
public class ZiRound // implements Heuristic
{
    private Model model;
    private NumVariable[] solutions;
    private int[] integerSolutions;
    private double threshold;
    private HeuristicListener solutionListener;

    /**
     * Constructor that initializes an instance of {@link #ZiRound}
     * Takes a solved Model
     * <p>
     * Note: the given model will receive changes in any case
     *
     * @param model     A relaxed solved Model
     * @param threshold threshold param for the ZiRound algorithm
     */
    public ZiRound(@NotNull Model model, double threshold) throws IloException {
        this.model = model;
        this.threshold = threshold;
    }

    /**
     * @param integerSolutions The indexes of the variables to round to their integer value
     */
    public void setIntegerSolutions(int[] integerSolutions) {
        this.integerSolutions = integerSolutions;
    }

    /**
     * The algorithm is designed to work on models that have only <, <=, = constraints
     * <p>
     * call {@link #setIntegerSolutions(int[])} before of this method to select solutions to round to integer
     * <p>
     * TODO: solve <= constraints
     */
    public void applyHeuristic() throws NumVariable.ValueOutOfBoundsException, InvalidAttributeValueException, IloException {
        //To get <, <=, = constraints
        model.changeConstraintsToLessThan();
        //The model is solved, we retrieve the solutions
        double[] numSolutions = model.getRelaxedSolutions();
        solutions = model.getVariables();
        for (int i = 0; i < solutions.length; i++) {
            solutions[i].setValue(numSolutions[i]);
        }

        NumVariable[] toBeRounded = new NumVariable[integerSolutions.length];
        boolean[] rounded = new boolean[toBeRounded.length];
        //select solutions to be rounded
        for (int i = 0; i < integerSolutions.length; i++) {
            toBeRounded[i] = solutions[integerSolutions[i]];
            toBeRounded[i].setType(NumVariable.VarType.REAL); //relaxing the variable
        }

        double ZI = 0;
        double[] zis;

        double slacksLB = 0;
        double slacksUB = 0;
        boolean noUpdates;
        do {
            noUpdates = true;
            zis = getZis(toBeRounded);
            /**
             * {@code noUpdates} bool remains true if isn't executed or if none of the inside block of code
             */
            for (int i = 0; i < integerSolutions.length; i++) {
                if (!rounded[i] && computeZI(toBeRounded[i]) != 0) {
                    double UB = Math.min(toBeRounded[i].getUpBound() - toBeRounded[i].getValue(), getSlackUB(i));

                    /// TODO: added to the original algorithm, to be verified
                    UB = Math.min(UB, Math.ceil(toBeRounded[i].getValue()) - toBeRounded[i].getValue());
                    ///

                    double LB = Math.min(toBeRounded[i].getValue() - toBeRounded[i].getLowBound(), getSlackLB(i));

                    /// TODO: added to the original algorithm, to be verified
                    LB = Math.min(LB, toBeRounded[i].getValue() - Math.floor(toBeRounded[i].getValue()));
                    ///

                    //UB, LB, threshold available
                    if (computeZI(toBeRounded[i].getValue() + UB) == computeZI(toBeRounded[i].getValue() - LB)
                            && computeZI(toBeRounded[i].getValue() + UB) < zis[i]) {
                        try {
                            //Rounding based on the objective function
                            if (model.getObjType() == Model.ObjType.MIN && model.getObjMultiplier(integerSolutions[i]) > 0
                                    || model.getObjType() == Model.ObjType.MAX && model.getObjMultiplier(integerSolutions[i]) < 0)
                                toBeRounded[i].setValue(toBeRounded[i].getValue() - LB);
                            else
                                toBeRounded[i].setValue(toBeRounded[i].getValue() + UB);
                            noUpdates = noUpdates && false;
                        } catch (InvalidAttributeValueException e) {
                            //won't happen as long as the given problem was relaxed
                        }
                    } else if (computeZI(toBeRounded[i].getValue() + UB) < computeZI(toBeRounded[i].getValue() - LB)
                            && computeZI(toBeRounded[i].getValue() + UB) < zis[i]) {
                        try {
                            toBeRounded[i].setValue(toBeRounded[i].getValue() + UB);
                            noUpdates = noUpdates && false;
                        } catch (InvalidAttributeValueException e) {
                            //won't happen as long as the given problem was relaxed
                        }
                    } else if (computeZI(toBeRounded[i].getValue() - LB) < computeZI(toBeRounded[i].getValue() + UB)
                            && computeZI(toBeRounded[i].getValue() - LB) < zis[i]) {
                        try {
                            toBeRounded[i].setValue(toBeRounded[i].getValue() - LB);
                            noUpdates = noUpdates && false;
                        } catch (InvalidAttributeValueException e) {
                            //won't happen as long as the given problem was relaxed
                        }
                    } else noUpdates = noUpdates && true;

                    if (computeZI(toBeRounded[i]) == 0) {
                        rounded[i] = true;
                    }
                }

            }


/**
 slacksLB = 0;
 slacksUB = 0;
 //Computing the exit condition
 for (int i = 0; i < model.countNumVariables(); i++) {
 slacksLB += getSlackLB(i);
 slacksUB += getSlackUB(i);
 }
 //slacks stay 0 if all variables were rounded
 */
        }
        while (!noUpdates); //no updates can be found

        if (sumZis(getZis(toBeRounded)) == 0) {
            //The algorithm has found a solution for the Mip problem
            for (NumVariable var : toBeRounded) {
                var.setType(NumVariable.VarType.INT);
            }
        }

    }

    public NumVariable[] Solutions() {
        return solutions;
    }

    /**
     * @param j Index of a variable in the model
     * @return The ub of xj = min(i) {si/aij: aij > 0}, 0 if there's no aij > 0
     */
    public double getSlackUB(int j) throws IloException {
        double min = -1;
        //The slacks are always >= 0 if the constraints are satisfied
        for (int i = 0; i < model.countConstraints(); i++) {
            if (model.getConstraintsMultiplier(i, j) > 0) {
                if (min == -1)
                    min = model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j);
                if (model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j) < min)
                    min = model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j);
            }
        }
        return (min == -1 ? 0 : min);
    }

    /**
     * @param j Index of a variable in the model
     * @return The lb of xj = min(i) {-si/aij: aij < 0}, 0 if there's no aij < 0
     */
    public double getSlackLB(int j) throws IloException {
        double min = -1;
        //The slacks are always >= 0 if the constraints are satisfied
        for (int i = 0; i < model.countConstraints(); i++) {
            if (model.getConstraintsMultiplier(i, j) < 0) {
                if (min == -1)
                    min = -model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j);
                if (-model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j) < min)
                    min = -model.getConstraintSlack(i) / model.getConstraintsMultiplier(i, j);
            }
        }
        return (min == -1 ? 0 : min);
    }

    public static double[] getZis(NumVariable[] vars) {
        double[] zis = new double[vars.length];
        for (int i = 0; i < zis.length; i++) {
            zis[i] = computeZI(vars[i]);
        }
        return zis;
    }

    /**
     * @param zis
     * @return
     */
    public static double sumZis(double[] zis) {
        double ZI = 0;
        for (double zi : zis)
            ZI += zi;
        return ZI;
    }

    /**
     * @param var A numerical variable
     * @return ZI(var) (always >= 0)
     */
    public static double computeZI(@NotNull NumVariable var) {
        return computeZI(var.getValue());
    }

    /**
     * @param value A numerical variable's value
     * @return ZI(value) (always >= 0)
     */
    public static double computeZI(double value) {
        return Math.min(value - Math.floor(value), Math.ceil(value) - value);
    }

    //    @Override
    public void setHeuristicListener(HeuristicListener listener) {
        solutionListener = listener;
    }
}
