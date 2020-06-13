package heuristics.ziround;

import javax.management.InvalidAttributeValueException;
import java.util.Objects;

/**
 * Simple class representing a Variable, can be of 2 types:
 * -Integer
 * -Real
 * <p>
 * Uses 64 bit floating point (double) to save the value
 */
public class NumVariable {
    private VarType type;
    private double value;
    private double upBound;
    private double lowBound;

    private static final String VALUE_NOT_INT_ERROR = "The var's type is Integer but the value is not";
    private static final String VALUE_OUT_OF_BOUNDS_ERROR = "The var's value can't be out of the given bounds";

    /**
     * Constructor, builds the object variable starting from the Type, the value, the UPPER and LOWER bound
     *
     * @param type     Type of Variable
     * @param value    Value of the Variable
     * @param upBound  UpperBound
     * @param lowBound LowerBound
     * @throws InvalidAttributeValueException If the variable's type is integer and the value is not
     *                                        OR the value is out of the given bounds
     */
    public NumVariable(VarType type, double value, double upBound, double lowBound) throws InvalidAttributeValueException, ValueOutOfBoundsException {
        if (type == VarType.INT && !isInt(value))
            throw new InvalidAttributeValueException(VALUE_NOT_INT_ERROR);
        if (value < lowBound || value > upBound)
            throw new ValueOutOfBoundsException(VALUE_OUT_OF_BOUNDS_ERROR);
        this.type = type;
        this.value = value;
        this.lowBound = lowBound;
        this.upBound = upBound;
    }

    /**
     * @return {@code True} if the value is integer, {@code False} otherwise
     */
    public boolean isInt() {
        return isInt(value);
    }

    /**
     * @param value A numerical value
     * @return {@code True} if the value is integer
     */
    private boolean isInt(double value) {
        return Math.min(value - Math.floor(value), Math.ceil(value) - value) == 0;
    }

    /**
     * @return The variable's type
     */
    public VarType getType() {
        return type;
    }

    /**
     * @param type The new type for the variable
     * @throws InvalidAttributeValueException if the variable's type is integer and the value is not
     */
    public void setType(VarType type) throws InvalidAttributeValueException {
        if (type == VarType.INT && !isInt(value))
            throw new InvalidAttributeValueException(VALUE_NOT_INT_ERROR);
        this.type = type;
    }

    /**
     * @return The variable's current value
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value The new value for the variable
     * @throws InvalidAttributeValueException If the variable's type is integer and the value is not
     *                                        OR the value is out of the given bounds
     */
    public void setValue(double value) throws InvalidAttributeValueException, ValueOutOfBoundsException {
        if (type == VarType.INT && !isInt(value))
            throw new InvalidAttributeValueException(VALUE_NOT_INT_ERROR);
        if (value < lowBound || value > upBound)
            throw new ValueOutOfBoundsException(VALUE_OUT_OF_BOUNDS_ERROR);
        this.value = value;
    }

    /**
     * @return The variable's current lower bound
     */
    public double getLowBound() {
        return lowBound;
    }

    /**
     * @return The variable's current upper bound
     */
    public double getUpBound() {
        return upBound;
    }

    public enum VarType {
        INT,
        REAL
    }

    /**
     * Exception to use when the value of a variable is out od its bounds
     */
    public static class ValueOutOfBoundsException extends Exception {
        public ValueOutOfBoundsException(String message) {
            super(message);
        }

        public ValueOutOfBoundsException() {
            super();
        }
    }

    /**
     * @param o Another object of the class {@link NumVariable} or its subclasses
     * @return {@code True} if all fields are equals, {@code False} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumVariable)) return false;
        NumVariable that = (NumVariable) o;
        return Double.compare(that.value, value) == 0 &&
                Double.compare(that.upBound, upBound) == 0 &&
                Double.compare(that.lowBound, lowBound) == 0 &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, upBound, lowBound);
    }
}
