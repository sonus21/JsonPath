package com.jayway.jsonpath;

import com.jayway.jsonpath.spi.transformer.TransformationSpec;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class EntryArrayTest {

    InputStream sourceStream;
    InputStream transformSpec;
    Configuration configuration;
    TransformationSpec spec;
    Object sourceJson;

    Object arraySourceJson;

    @Before
    public void setup() {
        configuration = Configuration.builder()
                .options(Option.CREATE_MISSING_PROPERTIES_ON_DEFINITE_PATH).build();
        sourceStream = this.getClass().getClassLoader().getResourceAsStream("transforms/single_entry_array_source.json");
        sourceJson = configuration.jsonProvider().parse(sourceStream, Charset.defaultCharset().name());

        sourceStream = this.getClass().getClassLoader().getResourceAsStream("transforms/multiple_entry_array_source.json");
        arraySourceJson = configuration.jsonProvider().parse(sourceStream, Charset.defaultCharset().name());

        DocumentContext jsonContext = JsonPath.parse(sourceJson);
        System.out.println("Document Input :" + jsonContext.jsonString());

        transformSpec = this.getClass().getClassLoader().getResourceAsStream("transforms/entry_array_spec.json");
        spec = configuration.transformationProvider().spec(transformSpec, configuration);

    }

    @Test
    public void single_entry_target_array_test() {
        Object transformed = configuration.transformationProvider().transform(sourceJson,spec, configuration);
        DocumentContext jsonContext = JsonPath.parse(transformed);
        String path = "$.data[0].value";
        assertEquals("aa", jsonContext.read(path));
        System.out.println("Document Created by Transformation:" + jsonContext.jsonString());
    }

    @Test
    public void multi_entry_target_array_test() {
        Object transformed = configuration.transformationProvider().transform(arraySourceJson,spec, configuration);
        DocumentContext jsonContext = JsonPath.parse(transformed);
        String path = "$.data[1].value";
        assertEquals("bb", jsonContext.read(path));
        System.out.println("Document Created by Transformation:" + jsonContext.jsonString());
    }

}
