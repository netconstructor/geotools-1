/*
 * MinFunction.java
 *
 * Created on 28 July 2002, 16:03
 */

package org.geotools.filter;

import org.geotools.feature.Feature;

/**
 *
 * @author  James
 */
public class MinFunctionImpl extends org.geotools.filter.MathExpressionImpl implements MinFunction {
    
    Expression a,b;
    
    /** Creates a new instance of MinFunction */
    public MinFunctionImpl() {
    }
    
    
    
    /** Returns a value for this expression.
     *
     * @param feature Specified feature to use when returning value.
     * @return Value of the feature object.
     */
    public Object getValue(Feature feature) {
        double first = ((Number)a.getValue(feature)).doubleValue();
        double second = ((Number)b.getValue(feature)).doubleValue();
        return new Double(Math.min(first,second));
    }
    
    public int getArgCount() {
        return 2;
    }
    
    public String getName() {
        return "Min";
    }
    
    public void setArgs(Expression[] args) {
        a = args[0];
        b = args[1];
    }
    
}
