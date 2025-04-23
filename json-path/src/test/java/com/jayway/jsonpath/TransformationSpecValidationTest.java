package com.jayway.jsonpath;

import com.jayway.jsonpath.spi.transformer.TransformationSpec;
import com.jayway.jsonpath.spi.transformer.TransformationSpecValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransformationSpecValidationTest {

    InputStream sourceStream;
    InputStream transformSpec;
    Configuration configuration;
    TransformationSpec spec;
    Object sourceJson;

    @BeforeEach
    public void setup() {
        configuration = Configuration.builder()
                .options(Option.CREATE_MISSING_PROPERTIES_ON_DEFINITE_PATH).build();
        sourceStream = this.getClass().getClassLoader().getResourceAsStream("transforms/transformsource.json");
        sourceJson = configuration.jsonProvider().parse(sourceStream, Charset.defaultCharset().name());

        DocumentContext jsonContext = JsonPath.parse(sourceJson);
        System.out.println("Document Input :" + jsonContext.jsonString());

        transformSpec = this.getClass().getClassLoader().getResourceAsStream(
                "transforms/transformspec_with_errors.json");

    }

    @Test
    public void transform_spec_validation_test() {
        try {
            spec = configuration.transformationProvider().spec(transformSpec, configuration);

        } catch (TransformationSpecValidationException ex) {
            System.out.println("ValidationErrors :" + ex.getMessage());
            String message = ex.getMessage();
            assertEquals(14, count(message, "errorCode"));
        }
    }

    private static int count(String str, String target) {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }

}
