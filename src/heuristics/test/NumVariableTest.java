package heuristics.test;

import heuristics.ziround.NumVariable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.management.InvalidAttributeValueException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(Parameterized.class)
public class NumVariableTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NumVariable.VarType.INT, 10, 0, 20},
                {NumVariable.VarType.REAL, 22.1, 0, 30},
                {NumVariable.VarType.INT, 43, 0, 50},
                {NumVariable.VarType.INT, 23, 0, 30},
                {NumVariable.VarType.INT, 50, 0, 60},
                {NumVariable.VarType.INT, -1, -10, 20},
                {NumVariable.VarType.REAL, -1000.12, -Double.MAX_VALUE, Double.MAX_VALUE},
                {NumVariable.VarType.INT, 10, 0, Integer.MAX_VALUE},
                {NumVariable.VarType.INT, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {NumVariable.VarType.REAL, 10000.5, 0, Double.MAX_VALUE},
        });
    }

    private NumVariable.VarType type;
    private double value;
    private double lowBound;
    private double upBound;
    private NumVariable variable;

    public NumVariableTest(NumVariable.VarType type, double value, double lowBound, double upBound) {
        this.type = type;
        this.value = value;
        this.lowBound = lowBound;
        this.upBound = upBound;
        try {
            this.variable = new NumVariable(type, value, upBound, lowBound);
        } catch (InvalidAttributeValueException e) {
            //Value isn't int while type is signed as INT
            fail();
        } catch (NumVariable.ValueOutOfBoundsException e) {
            //Value out of given bounds
            fail();
        }
    }

    @Test
    public void isInt() {
        if (type.equals(NumVariable.VarType.INT)) {
            try {
                int v = (int) value;
            } catch (NumberFormatException e) {
                //Value Not really int
                fail();
            }
            assertTrue(variable.isInt());
        }
    }

    @Test
    public void getType() {
        assertEquals(type, variable.getType());
    }

    @Test
    public void setType() {
        if (type.equals(NumVariable.VarType.INT))
            try {
                variable.setType(NumVariable.VarType.REAL);
            } catch (InvalidAttributeValueException e) {
                fail(); //unexpected
            }
        else if (type.equals(NumVariable.VarType.REAL)) {
            try {
                variable.setType(NumVariable.VarType.INT);
            } catch (InvalidAttributeValueException e) {
                assertNotEquals(0, Math.min(Math.ceil(value) - value, value - Math.floor(value)));
            }
        }
    }

    @Test
    public void getValue() {
        assertEquals(value, variable.getValue());
    }

    @Test
    public void setValueINTVar() {
        if (type.equals(NumVariable.VarType.INT)) {
            try {
                variable.setValue(-Math.PI);
            } catch (NumVariable.ValueOutOfBoundsException e) {
                if (-Math.PI < lowBound || -Math.PI > upBound)
                    return;
                fail(); //unexpected since it launched the exception without being out of bounds
            } catch (InvalidAttributeValueException e) {
                //expected
            }
        }
    }

    @Test
    public void setValueREALVar() {
        if (type.equals(NumVariable.VarType.REAL)) {
            try {
                variable.setValue(-Math.PI);
            } catch (NumVariable.ValueOutOfBoundsException e) {
                if (-Math.PI < lowBound || -Math.PI > upBound)
                    return;
                fail(); //unexpected since it launched the exception without being out of bounds
            } catch (InvalidAttributeValueException e) {
                fail();//unexpected, since the variable is REAL it should accept whatever double value
            }
        }
    }

    @Test
    public void getLowBound() {
        assertEquals(lowBound, variable.getLowBound());
    }

    @Test
    public void getUpBound() {
        assertEquals(upBound, variable.getUpBound());
    }

}