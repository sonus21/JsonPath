package com.jayway.jsonpath;

import com.jayway.jsonpath.spi.transformer.TransformationSpec;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ArrayTest {

    InputStream sourceStream;
    InputStream transformSpec;
    Configuration configuration;
    TransformationSpec spec;

    TransformationSpec flipped_spec;
    Object sourceJson;

    Object arraySourceJson;

    @Before
    public void setup() {
        configuration = Configuration.builder()
                .options(Option.CREATE_MISSING_PROPERTIES_ON_DEFINITE_PATH).build();
        sourceStream = this.getClass().getClassLoader().getResourceAsStream("transforms/array_source.json");
        sourceJson = configuration.jsonProvider().parse(sourceStream, Charset.defaultCharset().name());

        DocumentContext jsonContext = JsonPath.parse(sourceJson);
        System.out.println("Document Input :" + jsonContext.jsonString());

        transformSpec = this.getClass().getClassLoader().getResourceAsStream("transforms/array_spec.json");
        spec = configuration.transformationProvider().spec(transformSpec, configuration);

        transformSpec = this.getClass().getClassLoader().getResourceAsStream("transforms/array_flipped_spec.json");
        flipped_spec = configuration.transformationProvider().spec(transformSpec, configuration);

    }

    @Test
    public void array_test() {
        Object transformed = configuration.transformationProvider().transform(sourceJson,spec, configuration);
        DocumentContext jsonContext = JsonPath.parse(transformed);
        String path = "$.recordsTotal";
        assertEquals(10, (int)jsonContext.read(path));
        System.out.println("Document Created by Transformation:" + jsonContext.jsonString());
    }

    @Test
    public void array_flipped_test() {
        Object transformed = configuration.transformationProvider().transform(sourceJson,flipped_spec, configuration);
        DocumentContext jsonContext = JsonPath.parse(transformed);
        String path = "$.recordsTotal";
        assertEquals(10, (int)jsonContext.read(path));
        System.out.println("Document Created by Transformation:" + jsonContext.jsonString());
    }

}
