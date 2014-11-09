package RetrievalModel;

/**
 * Created by XiangyuSun on 9/20/14.
 */
public class RetrievalModelBM25 extends RetrievalModel {
    private double k1,b,k3;
    /**
     *  Set a retrieval model parameter.
     *  @param parameterName The name of the parameter to set.
     *  @param value The parameter's value.
     *  @return true if the parameter is set successfully, false otherwise.
     */
    public boolean setParameter (String parameterName, double value){
        if (parameterName.equalsIgnoreCase("BM25:k_1")) {
            k1 = value;
        } else if (parameterName.equalsIgnoreCase("BM25:b")) {
            b = value;
        } else if (parameterName.equalsIgnoreCase("BM25:k_3")) {
            k3 = value;
        } else
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
        if (parameterName.equalsIgnoreCase("BM25:k_1")) {
            k1 = Double.parseDouble(value);
        } else if (parameterName.equalsIgnoreCase("BM25:b")) {
            b = Double.parseDouble(value);
        } else if (parameterName.equalsIgnoreCase("BM25:k_3")) {
            k3 = Double.parseDouble(value);
        } else
            return false;
        return true;
    }
    /**
     * Return the parameter K_1
     * @return double
     */
    public double K_1() {return k1;}
    /**
     * Return the parameter B
     * @return double
     */
    public double B() {return b;}
    /**
     * Return the parameter K_3
     * @return double
     */
    public double K_3() {return k3;}

}
