package heuristics.test;

import heuristics.ziround.Model;
import heuristics.ziround.NumVariable;
import heuristics.ziround.ZiRound;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import javax.management.InvalidAttributeValueException;
import java.util.List;

public class AlgorithmRunTest {
    public static void main(String[] args) throws IloException, NumVariable.ValueOutOfBoundsException, InvalidAttributeValueException {
        List<String> fileModels = CplexUtilsTest.data();

        IloCplex iloCplex = new IloCplex();
        iloCplex.importModel(fileModels.get(0));
        Model model = new Model(iloCplex);
        double[] intSolutions = model.getSolutions();
        double[] relaxedSolutions = model.getRelaxedSolutions();

        ZiRound ziRound = new ZiRound(model, 00.1);
        ziRound.setIntegerSolutions(model.getIntegerConstraints());

        try {
            iloCplex.getStatus();
            ziRound.applyHeuristic();
        } catch (Exception e) {
            e.printStackTrace();
        }
        NumVariable[] solutions = ziRound.Solutions();
        for (int i = 0; i < solutions.length; i++) {
            System.out.println(solutions[i].getType() + " " + solutions[i].getValue() + " => [" + solutions[i].getLowBound() + ", " + solutions[i].getUpBound() + "]");
            System.out.println("Original Value: " + intSolutions[i] + " | Relaxed: " + relaxedSolutions[i]);
        }


    }
}
