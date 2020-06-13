package heuristics.ziround;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jetbrains.annotations.NotNull;

import javax.management.InvalidAttributeValueException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils class for common use methods
 * <p>
 * https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.0/ilog.odms.cplex.help/refjavacplex/html/ilog/concert/IloLPMatrix.html
 * https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.0/ilog.odms.cplex.help/refjavacplex/html/ilog/cplex/IloCplex.html#importModel(java.lang.String)
 * https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.0/ilog.odms.cplex.help/refjavacplex/html/ilog/concert/IloRange.html
 *
 * @author Turcato
 */
public class LPUtils {


    /**
     * Builds a LP model starting from a valid MPS file
     *
     * @param fileName The name of the file from which the model is read. Absolute path required
     *                 Must be a MPS format of a LP model
     * @return The Model built from the MPS file
     * @throws IloException TODO: find exception cause, not stated in {@link ilog.cplex.IloCplex} documentation
     */
    public static MMipModel fromMPS(@NotNull String fileName) throws IloException, InvalidAlgorithmParameterException {
        IloCplex iloCplexInstance = new IloCplex();
        iloCplexInstance.importModel(fileName); //This probably causes exceptions if the file is not found, ect..
        MMipModel model = new MMipModel();

        VariableLister modelLister = new VariableLister();
        try {
            modelLister.parse(iloCplexInstance);
        } catch (VariableLister.VariableListerException e) {
            e.printStackTrace();
            //The model might still be correct
        }

        IloLPMatrix matrix = modelLister.getMatrix();

        IloObjective obj = modelLister.getObj();

        NumVariable[] vars = new NumVariable[matrix.getNcols()];
        for (int j = 0; j < matrix.getNcols(); j++) {
            NumVariable.VarType varType = null;
            if (matrix.getNumVar(j).getType().equals(IloNumVarType.Int))
                varType = NumVariable.VarType.INT;
            else if (matrix.getNumVar(j).getType().equals(IloNumVarType.Float))
                varType = NumVariable.VarType.REAL;
            try {
                vars[j] = new NumVariable(varType, matrix.getNumVar(j).getUB(), matrix.getNumVar(j).getUB(), matrix.getNumVar(j).getLB());
            } catch (InvalidAttributeValueException | NumVariable.ValueOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        model.setConstraints(matrix);

        List<Double> mMultipliers = new ArrayList<>();
        if (obj.getExpr() instanceof IloLinearNumExpr) {
            IloLinearNumExpr lexpr = (IloLinearNumExpr) obj.getExpr();
            IloLinearNumExprIterator it = lexpr.linearIterator();

            int i = 0;
            while (it.hasNext()) {
                it.nextNumVar();
                mMultipliers.add(it.getValue());
            }
        }

        double[] objMultipliers = new double[mMultipliers.size()];
        for (int i = 0; i < mMultipliers.size(); i++) {
            objMultipliers[i] = mMultipliers.get(i);
        }

        MMipModel.ObjType objType;
        if (obj.getSense() == IloObjectiveSense.Maximize)
            objType = MMipModel.ObjType.MAX;
        else
            objType = MMipModel.ObjType.MIN;


        model.setVariables(vars);
        model.setObjective(objType, objMultipliers);
        return model;
    }
}
