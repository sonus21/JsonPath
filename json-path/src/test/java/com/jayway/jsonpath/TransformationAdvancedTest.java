package com.jayway.jsonpath;

import com.jayway.jsonpath.spi.transformer.TransformationSpec;
import com.jayway.jsonpath.spi.transformer.TransformationSpecValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TransformationAdvancedTest {

    InputStream sourceStream;
    InputStream sourceStream_1;
    InputStream transformSpec;
    Configuration configuration;
    TransformationSpec spec;
    Object sourceJson;
    Object sourceJson_1;
    DocumentContext jsonContext;

    @BeforeEach
    public void setup() {

        configuration = Configuration.builder().options(Option.CREATE_MISSING_PROPERTIES_ON_DEFINITE_PATH).build();
        sourceStream = this.getClass().getClassLoader().getResourceAsStream("transforms/shipment.json");
        sourceStream_1 = this.getClass().getClassLoader().getResourceAsStream("transforms/shipment_1.json");
        sourceJson = configuration.jsonProvider().parse(sourceStream, Charset.defaultCharset().name());
        sourceJson_1 = configuration.jsonProvider().parse(sourceStream_1, Charset.defaultCharset().name());

        jsonContext = JsonPath.parse(sourceJson);
        System.out.println("Document Input :" + jsonContext.jsonString());

        transformSpec = this.getClass().getClassLoader().getResourceAsStream("transforms/shipment_transform_spec.json");

        spec = configuration.transformationProvider().spec(transformSpec, configuration);

    }

    @Test
    public void simple_transform_spec_test() {
        Object transformed = configuration.transformationProvider().transform(sourceJson, spec, configuration);
        DocumentContext tgtJsonContext = JsonPath.parse(transformed);
        System.out.println("Document Created by Transformation:" + tgtJsonContext.jsonString());

        //Assertions about correctness of transformations
        //$.earliestStartTime +  $.plannedDriveDurationSeconds == $.testingAdditionalTransform.destinationSTAComputed
        long earliestStartTime = jsonContext.read("$.earliestStartTime");
        int plannedDriveDurationSeconds = jsonContext.read("$.plannedDriveDurationSeconds");
        long destinationSTAComputed = tgtJsonContext.read("$.testingAdditionalTransform.destinationSTAComputed");
        assertEquals((earliestStartTime + plannedDriveDurationSeconds), destinationSTAComputed);

        //! $.isTPLManaged == $.testingAdditionalTransform.isNotTPLManaged
        boolean isTPLManaged = jsonContext.read("$.isTPLManaged");
        boolean isNotTPLManaged = tgtJsonContext.read("$.testingAdditionalTransform.isNotTPLManaged");
        assertEquals(!isTPLManaged, isNotTPLManaged);

        //$.cost + 100 == $.testingAdditionalTransform.totalCost
        double cost = jsonContext.read("$.cost");
        double totalCost = tgtJsonContext.read("$.testingAdditionalTransform.totalCost");
        assertEquals(cost + 100, totalCost, 0);

        //$.weight / 1000 == $.testingAdditionalTransform.weightKGS
        double weight = jsonContext.read("$.weight");
        double weightKGS = tgtJsonContext.read("$.testingAdditionalTransform.weightKGS");
        assertEquals(weight / 1000, weightKGS, 0.01);

        //1 - $.weightUtilization == $.testingAdditionalTransform.unUtilizedWeight
        double weightUtilization = jsonContext.read("$.weightUtilization");
        double unUtilizedWeight = tgtJsonContext.read("$.testingAdditionalTransform.unUtilizedWeight");
        assertEquals(1 - weightUtilization, unUtilizedWeight, 0.01);
    }

    @Test
    public void simple_transform_spec_with_missing_source_fields() {
        System.out.println(spec);
        Object transformed = configuration.transformationProvider().transform(sourceJson_1, spec, configuration);
        DocumentContext tgtJsonContext = JsonPath.parse(transformed);
        System.out.println("Document Created by Transformation:" + tgtJsonContext.jsonString());
    }

    @Test
    public void multiple_wild_card() {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("transforms/airlines.json");
        Object json = configuration.jsonProvider().parse(stream, Charset.defaultCharset().name());
        DocumentContext cntxt = JsonPath.parse(json);

        InputStream specStream = this.getClass().getClassLoader().getResourceAsStream("transforms/multiwildcard_spec.json");
        TransformationSpec tspec = configuration.transformationProvider().spec(specStream, configuration);

        Object transformed = configuration.transformationProvider().transform(json, tspec, configuration);
        DocumentContext tgtJsonContext = JsonPath.parse(transformed);
        System.out.println("Document Created by Transformation:" + tgtJsonContext.jsonString());
        String atl = tgtJsonContext.read("$.reservation.originAirportCode[0]");
        assertEquals(atl, "ATL");
        String dtw = tgtJsonContext.read("$.reservation.originAirportCode[1]");
        assertEquals(dtw, "DTW");
    }

    @Test
    public void wild_card_target() {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("transforms/airlines.json");
        Object json = configuration.jsonProvider().parse(stream, Charset.defaultCharset().name());
        DocumentContext cntxt = JsonPath.parse(json);

        InputStream specStream = this.getClass().getClassLoader().getResourceAsStream("transforms/wildcard_target_spec.json");
        TransformationSpec tspec = configuration.transformationProvider().spec(specStream, configuration);

        Object transformed = configuration.transformationProvider().transform(json, tspec, configuration);
        DocumentContext tgtJsonContext = JsonPath.parse(transformed);
        String atl = tgtJsonContext.read("$.reservation[0].originAirportCode");
        assertEquals(atl, "ATL");
        String dtw = tgtJsonContext.read("$.reservation[1].originAirportCode");
        assertEquals(dtw, "DTW");
        atl = tgtJsonContext.read("$.reservation[1].destinationAirportCode");
        assertEquals(atl, "ATL");
        dtw = tgtJsonContext.read("$.reservation[0].destinationAirportCode");
        assertEquals(dtw, "DTW");
        System.out.println("Document Created by Transformation:" + tgtJsonContext.jsonString());
    }

    @Test
    public void invalid_multi_wild_card_target() {
        InputStream specStream = this.getClass().getClassLoader().getResourceAsStream("transforms/invalid_wildcard_target_spec.json");
        assertThrows(TransformationSpecValidationException.class, () -> configuration.transformationProvider().spec(specStream, configuration));
    }

}
