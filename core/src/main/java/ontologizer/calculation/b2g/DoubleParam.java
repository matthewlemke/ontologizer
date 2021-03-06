package ontologizer.calculation.b2g;

class DoubleParam extends B2GParam
{
    private double min = Double.NaN;

    private double max = Double.NaN;

    private double val;

    public DoubleParam(Type type, double val)
    {
        super(type);

        this.val = val;
    }

    public DoubleParam(DoubleParam p)
    {
        super(p);

        this.val = p.val;
    }

    public DoubleParam(Type type)
    {
        super(type);

        if (type == Type.FIXED) {
            throw new IllegalArgumentException("Parameter could not be instanciated of type Fixed.");
        }
    }

    double getValue()
    {
        return this.val;
    }

    void setValue(double newVal)
    {
        this.val = newVal;
        setType(Type.FIXED);
    }

    /**
     * Applicable for Variables of type MCMC or EM.
     *
     * @param min
     */
    public void setMin(double min)
    {
        this.min = min;
    }

    /**
     * Applicable for Variables of type MCMC or EM.
     *
     * @param max
     */
    public void setMax(double max)
    {
        this.max = max;
    }

    /**
     * Applicable for Variables of type MCMC or EM.
     *
     * @return NaN if no maximum has been specified.
     */
    public double getMin()
    {
        return this.min;
    }

    /**
     * Returns whether variable has a minimum.
     *
     * @return
     */
    public boolean hasMin()
    {
        return !Double.isNaN(this.min);
    }

    /**
     * Applicable for Variables of type MCMC or EM.
     *
     * @return NaN if no maximum has been specified.
     */
    public double getMax()
    {
        return this.max;
    }

    /**
     * Returns whether variable has a maximum.
     *
     * @return
     */
    public boolean hasMax()
    {
        return !Double.isNaN(this.max);
    }

    @Override
    public String toString()
    {
        if (isFixed()) {
            return String.format("%g", this.val);
        }
        return getType().toString();
    }
}
