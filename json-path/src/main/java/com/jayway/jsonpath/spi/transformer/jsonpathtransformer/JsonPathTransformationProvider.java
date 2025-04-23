package com.jayway.jsonpath.spi.transformer.jsonpathtransformer;

import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.transformer.*;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.LookupTable;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.OperatorRegistry;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.Operator;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.PathMapping;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.SourceTransform;
import com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.TransformationModel;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

import static com.jayway.jsonpath.spi.transformer.jsonpathtransformer.JsonPathTransformationSpec.isArrayWildCard;
import static com.jayway.jsonpath.spi.transformer.jsonpathtransformer.JsonPathTransformationSpec.isScalar;
import static com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model.JsonPathTransformerValidationError.*;


public class JsonPathTransformationProvider implements TransformationProvider<JsonPathTransformationSpec> {

    @Override
    public TransformationSpec spec(String input, Configuration configuration) {
        JsonPathTransformationSpec ret;

        if (input == null) {
            throw new IllegalArgumentException("parameter 'input' cannot be null");
        }
        try {
            TransformationModel model = configuration.mappingProvider().map(
                    configuration.jsonProvider().parse(input),
                    TransformationModel.class, configuration);
            ret = new JsonPathTransformationSpec(model);
        } catch (Exception ex) {
            throw new TransformationSpecValidationException(ex);
        }
        //Note: some implementations of the transformation provider might choose not to call validate implicitly
        //instead it will be a separate API call from the client.
        List<ValidationError> errors = ret.validate();
        if (errors != null && !errors.isEmpty()) {
            throw new TransformationSpecValidationException(stringifyErrors(errors));
        }
        return ret;
    }


    @Override
    public TransformationSpec spec(InputStream input, Configuration configuration) {

        JsonPathTransformationSpec ret;

        if (input == null) {
            throw new IllegalArgumentException(
                    getStringFromBundle(NULL_PARAMETER, "input"));
        }
        try {
            TransformationModel model = configuration.mappingProvider().map(
                    configuration.jsonProvider().parse(input, Charset.defaultCharset().name()),
                    TransformationModel.class, configuration);
            ret = new JsonPathTransformationSpec(model);
        } catch (Exception ex) {
            throw new TransformationSpecValidationException(ex);
        }
        //Note: some implementations of the transformation provider might choose not to call validate implicitly
        //instead it will be a separate API call from the client.
        List<ValidationError> errors = ret.validate();
        if (errors != null && !errors.isEmpty()) {
            throw new TransformationSpecValidationException(stringifyErrors(errors));
        }
        return ret;

    }

    @Override
    public Object transform(Object source, JsonPathTransformationSpec spec, Configuration configuration) {

        configuration.addOptions(Option.CREATE_MISSING_PROPERTIES_ON_DEFINITE_PATH);

        //start with an empty InputObject
        String inputObject = "{ }";
        Object transformed = null;
        boolean first = true;
        Object sourceJson = source;

        if (source == null) {
            throw new IllegalArgumentException(
                    getStringFromBundle(NULL_PARAMETER, "source"));
        }

        if (source instanceof String) {
            sourceJson = configuration.jsonProvider().parse((String) source);
        } else if (source instanceof InputStream) {
            sourceJson = configuration.jsonProvider().parse((InputStream) source,
                    Charset.defaultCharset().name());
        }
        if (!configuration.jsonProvider().isArray(sourceJson) &&
                !configuration.jsonProvider().isMap(sourceJson)) {
            throw new IllegalArgumentException(
                    getStringFromBundle(INVALID_JSON_OBJECT, source.getClass().getName()));
        }

        DocumentContext jsonContext = JsonPath.parse(sourceJson);
        TransformationModel model = (TransformationModel) spec.get();

        for (PathMapping pm : model.getPathMappings()) {

            String srcPath = pm.getSource();
            if (srcPath != null) {
                JsonPath compiledSrcPath = JsonPath.compile(srcPath);
                JsonPath compiledDstPath = JsonPath.compile(pm.getTarget());

                if (!compiledSrcPath.isDefinite() && !compiledDstPath.isDefinite()
                        && isArrayWildCard(pm.getSource(), true)
                        && isArrayWildCard(pm.getTarget(), false)) {
                    //TODO: handle multiple wild-cards : construct a tree and each path from root to leaf
                    //would then provide one expanded Path.
                    List<PathMapping> expanded = computedExpandedPathForArrays(
                            source, srcPath, pm.getTarget(), configuration);
                    for (PathMapping exp : expanded) {
                        if (first) {
                            transformed = transform(
                                    exp, first, inputObject, configuration, jsonContext, transformed, model
                                    , pm.getLookupTable());
                            //if the very first path is not found in the source.
                            first = (transformed != null) ? false : true;
                        } else {
                            transformed = transform(
                                    exp, first, inputObject, configuration, jsonContext, transformed, model
                                    , pm.getLookupTable());
                        }

                    }
                    continue;

                }
            }

            if (first) {
                transformed = transform(
                        pm, first, inputObject, configuration, jsonContext, transformed, model
                        , pm.getLookupTable());
                //if the very first path is not found in the source.
                first = (transformed != null) ? false : true;
            } else {
                transformed = transform(
                        pm, first, inputObject, configuration, jsonContext, transformed, model
                        , pm.getLookupTable());
            }
        }

        return transformed;
    }

    private Object transform(PathMapping pm,
                             boolean first, String inputObject,
                             Configuration configuration, DocumentContext jsonContext, Object transformed,
                             TransformationModel model, String lookupTable) {


        String srcPath = pm.getSource();
        JsonPath compiledSrcPath = null;

        if (srcPath != null) {
            compiledSrcPath = JsonPath.compile(srcPath);
        }

        JsonPath compiledDstPath = JsonPath.compile(pm.getTarget());
        Object srcValue = null;
        boolean srcValueIsConstant = false;

        if (srcPath == null && pm.getAdditionalTransform() == null && pm.getTarget() != null) {
            //throw No sourcepath value specified for setting target path
            throw new TransformationException(
                    getStringFromBundle(NO_SOURCE_VALUE_FOR_MAPPING, pm.getTarget()));
        }
        if (pm.getTarget() == null) {
            //throw target cannot be null
            throw new TransformationException(
                    getStringFromBundle(MISSING_TARGET_PATH_FOR_MAPPING,
                            (srcPath != null) ? srcPath : "null"));
        }
        SourceTransform additionalTransform = pm.getAdditionalTransform();
        Object constantSourceValue = (additionalTransform != null) ?
                additionalTransform.getConstantSourceValue() : null;
        String additionalSourcePath = (additionalTransform != null) ?
                additionalTransform.getSourcePath() : null;
        String operator = (additionalTransform != null) ?
                additionalTransform.getOperator() : null;


        if (additionalTransform != null) {
            if (constantSourceValue != null &&
                    additionalSourcePath != null) {
                //throw ambiguous additionalTransform Operand
                throw new TransformationException(
                        getStringFromBundle(AMBIGUOUS_ADDITIONAL_TRANSFORM,
                                srcPath));
            }
            if (srcPath == null && constantSourceValue != null) {
                if (operator != null) {
                    //throw ambiguous operator since sourcePath is null
                    throw new TransformationException(
                            getStringFromBundle(INVALID_OPERATOR_NULL_SRC_PATH,
                                    operator));
                }
                //allows a constant value to be set directly onto the target path
                srcValue = constantSourceValue;
                srcValueIsConstant = true;

            }
            if (additionalSourcePath != null && operator == null) {
                // throw missing operator for additional source transform
                throw new TransformationException(
                        getStringFromBundle(MISSING_OPERATOR,
                                srcPath, pm.getAdditionalTransform().getSourcePath()));
            }
        }
        if (!srcValueIsConstant && srcPath != null) {
            try {
                srcValue = jsonContext.read(compiledSrcPath);
                if (srcValue == null) {
                    // TODO: log here. we are going to ignore any additionalTransform as well.
                    return transformed;
                }
            } catch (PathNotFoundException ex) {
                // if the source path does not exist then nothing to do
                // just return what came in
                // TODO: log here. we are going to ignore any additionalTransform as well.
                return transformed;
            }
        }

        //assert srcValue is a scalar type. We do not want an Array
        if (!isScalar(srcValue)) {
            //now check if its an array of size 1
            //applicable when the src path has filter predicates
            if (configuration.jsonProvider().isArray(srcValue)) {
                if (configuration.jsonProvider().length(srcValue) == 1) {
                    srcValue = configuration.jsonProvider().getArrayIndex(srcValue, 0);
                } else if (configuration.jsonProvider().length(srcValue) == 0) {
                    // TODO: log here. we are going to ignore any additionalTransform as well.
                    return transformed;
                }
            } else {
                throw new TransformationException(
                        getStringFromBundle(SOURCE_NOT_SCALAR, srcPath, srcValue.getClass().getName()));
            }
        }
        if (lookupTable != null && (srcValue instanceof String)) {
            srcValue = lookup(srcValue, lookupTable, model.getLookupTables());
        }

        //process any additional source transforms
        Object additonalSourceValue = null;
        if (!srcValueIsConstant && additionalTransform != null) {
            if (constantSourceValue != null) {
                //case of a constant operand with operator to be applied on srcValue
                additonalSourceValue = constantSourceValue;
            } else if (additionalSourcePath != null) {
                //case of additional JsonPath from source document with operator to be applied
                //on srcValue
                try {
                    additonalSourceValue =
                            jsonContext.read(JsonPath.compile(additionalSourcePath));
                } catch (PathNotFoundException ex) {
                    //throw new TransformationException(ex);
                    //any time we find a missing value in source document path
                    //we ignore the entire operation.
                }

            }
        }

        Operator op = OperatorRegistry.getOperator(operator);
        if (operator != null && op == null) {
            throw new TransformationException("operator '" + operator + "' is not registered/found");
        }

        // it's a unary so lets apply on the source
        if (additonalSourceValue != null || OperatorRegistry.isUnary(op)) {
            if (operator != null) {
                srcValue = applyAdditionalTransform(srcValue, additonalSourceValue, op);
            }
        }

        //here based on size of srcValue
        //validate destination has only single WildCard Array whereas source can have multiple
        //Create destination expressions for each SrcValue
        //example : $['reservation'][0]['originairportcode']
        //$['reservation'][1]['originairportcode']
        //use srcValue[0] and srcValue[1] to set the value
        if (configuration.jsonProvider().isArray(srcValue) && isArrayWildCard(pm.getTarget(), false)) {
            for (int i = 0; i < configuration.jsonProvider().length(srcValue); i++) {
                //first expand destination path using i in the wildcard
                //compile the destination path and use it
                String target = pm.getTarget();
                String updated = replaceWildCardWith(target, i);
                JsonPath dstPath = JsonPath.compile(updated);
                if (first) {
                    //srcValue is jsonarray
                    transformed = dstPath.set(configuration.jsonProvider().parse(inputObject),
                            configuration.jsonProvider().getArrayIndex(srcValue, i), configuration);
                    first = false;
                } else {
                    transformed = dstPath.set(transformed,
                            configuration.jsonProvider().getArrayIndex(srcValue, i), configuration);
                }

            }
        } else {
            //srcValue is not jsonarray
            if (first) {
                if (isArrayWildCard(pm.getTarget(), false)) {
                    //srcValue is not jsonarray but target path is an array
                    String target = pm.getTarget();
                    String updated = replaceWildCardWith(target, 0);
                    JsonPath dstPath = JsonPath.compile(updated);
                    transformed = dstPath.set(configuration.jsonProvider().parse(inputObject),
                            srcValue, configuration);
                } else {
                    transformed = compiledDstPath.set(configuration.jsonProvider().parse(inputObject),
                            srcValue, configuration);
                }
            } else {
                if (isArrayWildCard(pm.getTarget(), false)) {
                    //srcValue is not jsonarray but target path is an array
                    String target = pm.getTarget();
                    String updated = replaceWildCardWith(target, 0);
                    JsonPath dstPath = JsonPath.compile(updated);
                    transformed = dstPath.set(transformed, srcValue, configuration);
                } else {
                    transformed = compiledDstPath.set(transformed, srcValue, configuration);
                }
            }
        }

        return transformed;
    }

    private String replaceWildCardWith(String target, int i) {
        return target.replace("[*]", "[" + i + "]");
    }

    private void checkDataTypesAndOperator(
            Object srcValue, Object additionalSourceValue,
            Operator operator) {
        if (srcValue != null && additionalSourceValue != null && operator == null) {
            //throw Operator null while source operands non-null
            throw new TransformationException(
                    getStringFromBundle(MISSING_OPERATOR,
                            srcValue, additionalSourceValue));
        }
        if (OperatorRegistry.isUnary(operator)) {
            if (srcValue != null && additionalSourceValue != null) {
                //throw invalid Unary Operator NOT with Two Operands
                throw new TransformationException(
                        getStringFromBundle(INVALID_UNARY_OPERATOR,
                                srcValue, additionalSourceValue));
            }
        } else if (operator != null && anyOperandIsNull(srcValue, additionalSourceValue)) {
            //throw invalid Binary operator, one of the operands is null
            throw new TransformationException(
                    getStringFromBundle(INVALID_BINARY_OPERATOR, operator));
        }

        if (operator == null) {
            return;
        }
        if (srcValue instanceof Number && !(
                OperatorRegistry.NUMERIC.equals(operator.getType())
                        || operator.isUnary())) {
            //throw expected numeric operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "numeric", operator.getType()));
        } else if (srcValue instanceof String && !(
                OperatorRegistry.STRING.equals(operator.getType())
                        || operator.isUnary())) {
            //throw expected String operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "string", operator.getType()));
        } else if (srcValue instanceof Boolean &&
                !(OperatorRegistry.BOOLEAN.equals(operator.getType()) || operator.isUnary())) {
            //throw expected boolean operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "boolean/unary_boolean", operator.getType()));
        }

        if (additionalSourceValue instanceof Number && !OperatorRegistry.NUMERIC.equals(operator.getType())) {
            //throw expected numeric operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "numeric", operator.getType()));
        } else if (additionalSourceValue instanceof String && !OperatorRegistry.STRING.equals(operator.getType())) {
            //throw expected String operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "string", operator.getType()));
        } else if (additionalSourceValue instanceof Boolean && !OperatorRegistry.BOOLEAN.equals(operator.getType())) {
            //throw expected boolean operator but found
            throw new TransformationException(
                    getStringFromBundle(INVALID_OPERATOR_FOR_TYPE,
                            operator.name(), "boolean", operator.getType()));
        }

    }

    private boolean anyOperandIsNull(Object srcValue, Object additionalSourceValue) {
        if (srcValue == null || additionalSourceValue == null) {
            return true;
        }
        return false;
    }

    private Object lookup(Object srcValue, String lookupTable, LookupTable[] lookupTables) {
        for (LookupTable l : lookupTables) {
            if (l.getTableName().equals(lookupTable)) {
                if (!l.getTableData().containsKey(srcValue)) {
                    // TODO: log a warning and return the srcValue unchanged
                    return srcValue;
                }
                return l.getTableData().get(srcValue);
            }
        }
        // if the lookup table is not found
        throw new TransformationException(
                getStringFromBundle(INVALID_LOOKUP_TABLE_REF, lookupTable));
    }

    private List<PathMapping> computedExpandedPathForArrays(
            Object source, String srcPath, String dstPath, Configuration configuration) {

        //We support only a single wild-card to begin with.
        String srcpathTrimmed = srcPath.replaceAll("\\s", "");
        String dstpathTrimmed = dstPath.replaceAll("\\s", "");
        /*

        int firstIndex = srcpathTrimmed.indexOf("[*]");
        if (firstIndex == -1) {
            throw new TransformationException("c");
        }
        String pathTillArray = srcpathTrimmed.substring(0, firstIndex + 3);
        //query the source document to figure out how many entries exist in the  source array
        List<Object> items = JsonPath.read(source, pathTillArray);
        if (items == null) {
            throw new TransformationException(
                    getStringFromBundle(NULL_QUERY_RESULT, pathTillArray));

        }
        int size = items.size();
        String dstpathTrimmed = dstPath.replaceAll("\\s", "");
        firstIndex = dstpathTrimmed.indexOf("[*]");
        if (firstIndex == -1) {
            throw new TransformationException(getStringFromBundle(INTERNAL_ERROR));
        }
        */


        List<PathMapping> result = new ArrayList<PathMapping>();

        /*
        for (int i = 0; i < size; i++) {
            PathMapping p = new PathMapping();
            p.setSource(srcpathTrimmed.replace("[*]", "[" + i + "]"));
            p.setTarget(dstpathTrimmed.replace("[*]", "[" + i + "]"));
            result.add(p);
        }*/

        for (int i = 0; i < 1; i++) {
            PathMapping p = new PathMapping();
            p.setSource(srcpathTrimmed);
            p.setTarget(dstpathTrimmed);
            result.add(p);
        }

        return result;
    }


    private String stringifyErrors(List<ValidationError> errors) {

        if (errors == null) {
            throw new IllegalArgumentException(getStringFromBundle(NULL_PARAMETER, "errors"));
        }

        StringBuilder builder = new StringBuilder("\n{\n");
        for (int i = 0; i < errors.size(); i++) {
            builder = (i == (errors.size() - 1)) ? builder.append(errors.get(i)) :
                    builder.append(errors.get(i));
        }
        builder.append("}\n");
        return builder.toString();
    }

    private Object applyAdditionalTransform(
            Object srcValue, Object srcPostProcessingVal,
            Operator op) {
        return op.apply(srcValue, srcPostProcessingVal);
    }

}
