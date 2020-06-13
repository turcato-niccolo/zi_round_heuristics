package heuristics.test;

import heuristics.ziround.Model;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnit4.class)
public class CplexUtilsTest {
    public static List<String> data() {
        List<String> models = new ArrayList<>();
        File folder = new File(modelsPath);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isFile())
                models.add(fileEntry.getAbsolutePath());
        }
        return models;
    }


    private final static String modelsPath = "D:\\Java\\Repository\\ZIRoundHeuristics\\src\\heuristics\\test\\models";
    private FileWriter writer;
    private List<String> files;

    public CplexUtilsTest() {
        files = data();
        File log = new File("log.txt");
        if (log.exists()) {
            log.delete();
            try {
                log.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }

        writer = null;
        try {
            writer = new FileWriter(log);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    Model model;

    @Test
    public void fromMPS() {
        for (String file : files) {


            try {
                System.out.println(file);
                IloCplex cplex = new IloCplex();
                cplex.importModel(file);
                model = new Model(cplex);
            } catch (IloException e) {
                e.printStackTrace();
                failed(file + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                failed(file + e.getMessage());
            }

            try {
                assertTrue(model.countNumVariables() > 0);
            } catch (IloException e) {
                e.printStackTrace();
                failed(file + e.getMessage());
            }

            try {
                assertTrue(model.countConstraints() > 0);
            } catch (IloException e) {
                e.printStackTrace();
                failed(file + e.getMessage());
            }
            System.gc();
        }

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void failed(String file) {
        try {
            writer.write(file + " FAILED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}