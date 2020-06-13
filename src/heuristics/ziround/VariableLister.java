package heuristics.ziround;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.Comparator;
import java.util.Iterator;

/**
 * VariableLister provides a static method to scan a CPLEX model and extract
 * a list of variables it contains.
 * <p>
 * Usage:
 * IloNumVar[] vars = VariableLister.parse(model);
 */
public class VariableLister {
    private IloLPMatrix matrix;
    private IloObjective obj;

    /**
     * A function to parse CPLEX models (instances of IloCplex) and return
     * a vector of all variables contained in the model. The vector is
     * sorted alphabetically by the names of the variables. If the variable
     * has no name, one is assigned to it (prefix + counter). The prefix is
     * a static field of the class, and can be changed at will.
     *
     * @param model the model to parse
     * @throws IloException            if CPLEX has trouble iterating over the model
     * @throws VariableListerException if an entity is encountered whose type cannot be deciphered
     */
    public void parse(IloCplex model)
            throws IloException, VariableListerException {
        long counter = 0;  // used to create variable names
        Iterator it = model.iterator();
        IloLinearNumExpr lExpr;
        IloQuadNumExpr qExpr;
        IloLinearNumExprIterator lit;
        IloQuadNumExprIterator qit;
        while (it.hasNext()) {
            IloAddable thing = (IloAddable) it.next();
            System.out.print("Scanning " + thing.getName() + " (type ");
            if (thing instanceof IloRange || thing instanceof IloObjective) {
                Object o;
                if (thing instanceof IloRange) {
                    System.out.println("IloRange)");
                } else {
                    System.out.println("IloObjective)");
                    obj = (IloObjective) thing;
                }
            } else if (thing instanceof IloSOS1) {
                System.out.println("IloSOS1)");
            } else if (thing instanceof IloSOS2) {
                System.out.println("IloSOS2)");
            } else if (thing instanceof IloLPMatrix) {
                System.out.println("IloLPMatrix)");
                matrix = (IloLPMatrix) thing;
            } else if (thing instanceof IloNumVar) {
                System.out.println("IloNumVar)");
            } else {
                throw new VariableListerException("An object of unrecognized type was "
                        + "encountered while parsing the "
                        + "model.");
            }
        }
        return;
    }

    public IloLPMatrix getMatrix() {
        return matrix;
    }

    public IloObjective getObj() {
        return obj;
    }

    /**
     * Custom exception class to deal with unknown model object types.
     */
    public static class VariableListerException extends Exception {
        /**
         * Constructor.
         *
         * @param msg message string to include in the exception
         */
        public VariableListerException(String msg) {
            super(msg);
        }
    }

    /**
     * Custom comparator used to sort variables based on their names.
     */
    private static class VariableComparator implements Comparator<IloNumVar> {

        @Override
        public int compare(IloNumVar o1, IloNumVar o2) {
            return o1.getName().compareTo(o2.getName());
        }

    }
}
