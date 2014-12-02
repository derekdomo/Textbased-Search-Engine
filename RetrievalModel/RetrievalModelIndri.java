package RetrievalModel;

/**
 * Created by XiangyuSun on 9/20/14.
 */
public class RetrievalModelIndri extends RetrievalModel {
    private double lambda;
    private int mu;
    /**
     *  Set a retrieval model parameter.
     *  @param parameterName The name of the parameter to set.
     *  @param value The parameter's value.
     *  @return true if the parameter is set successfully, false otherwise.
     */
    public boolean setParameter (String parameterName, double value){
        if (parameterName.equalsIgnoreCase("Indri:lambda")) {
            lambda = value;
        } else if (parameterName.equalsIgnoreCase("Indri:mu")) {
            mu = (int)value;
        }else
            return false;
        return true;
    }
    /**
     *  Set a retrieval model parameter.
     *  @param parameterName The name of the parameter to set.
     *  @param value The parameter's value.
     *  @return true if the parameter is set successfully, false otherwise.
     */
    public boolean setParameter (String parameterName, String value) {
        if (parameterName.equalsIgnoreCase("Indri:mu")) {
            mu = Integer.parseInt(value);
        } else if (parameterName.equalsIgnoreCase("Indri:lambda")) {
            System.out.println(lambda);
            lambda = Double.parseDouble(value);
        } else
            return false;
        return true;
    }
    /**
     * Return the parameter mu
     * @return int
     */
    public int mu() {return mu;}
    /**
     * Return the parameter lamda
     * @return double
     */
    public double lambda() {return lambda;}
}
