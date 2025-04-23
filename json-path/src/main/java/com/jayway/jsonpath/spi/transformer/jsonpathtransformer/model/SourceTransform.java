package com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model;


/**
 * Captures a single additional Path OR Constant along with Operation to be performed
 * on the Source Value read from the Json Document.
 * <p>
 * <p>
 * If a Constant Value is specified without Operator and the srcPath at the parent level
 * is null then effectively the constant is set as the value of the target path in the
 * transformed document.
 */
public class SourceTransform {


    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Object getConstantSourceValue() {
        return constantSourceValue;
    }

    public void setConstantSourceValue(Object constantSourceValue) {
        this.constantSourceValue = constantSourceValue;
    }


    @Override
    public String toString() {
        return " AdditionalSource [operator = " + operator + ", sourcePath = " + sourcePath +
                ", constantSourceValue = " + constantSourceValue + "]";
    }

    private String operator;
    private String sourcePath;
    private Object constantSourceValue;
}
