package com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model;

import java.util.HashMap;
import java.util.Map;

public class TypeSystem {

    private static final Map<String, String> inferredTypes = new HashMap<>();

    public static String get(String ab) {
        return inferredTypes.get(ab);
    }

    private static enum InferredTypesCombo {
        II_I,
        IS_I,
        IL_L,
        LL_L,
        SS_S,
        SI_I,
        LI_L,
        LS_L,
        SL_L,
        IF_F,
        FI_F,
        FS_F,
        FL_F,
        SF_F,
        LF_F,
        FF_F,
        FD_D,
        DF_D,
        ID_D,
        DI_D,
        DS_D,
        DD_D,
        SD_D,
        LD_D,
        DL_D
    }

    static {
        for (InferredTypesCombo s : InferredTypesCombo.values()) {
            String[] str = s.name().split("_");
            //System.out.println(str[0] + ":" + str[1]);
            inferredTypes.put(str[0], str[1]);

        }

    }

}
