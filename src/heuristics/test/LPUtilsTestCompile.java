package heuristics.test;


import heuristics.ziround.LPUtils;

public class LPUtilsTestCompile {
    public static void main(String[] args) {
        try {
            if (args.length > 0)
                LPUtils.fromMPS(args[0]);
            else
                LPUtils.fromMPS("models\\10teams.mps");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
