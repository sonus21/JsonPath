package com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model;

import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.spi.transformer.TransformationException;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;


public class OperatorRegistry {
    private static final Map<String, Operator> operators = new HashMap<>();
    public static final String NUMERIC = "numeric";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";

    public static Operator getOperator(String operator) {
        if (KnownOperator.allowedOperations.contains(operator)) {
            return KnownOperator.valueOf(operator);
        }
        if (operators.containsKey(operator)) {
            return operators.get(operator);
        }
        return null;
    }

    public void register(Operator op) {
        operators.put(op.name(), op);
    }

    public static boolean isUnary(Operator op) {
        if (op == null) {
            return false;
        }
        return op.isUnary();
    }


    public enum KnownOperator implements Operator {
        RHS_STRING_CONCAT(STRING) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof String) {
                    return (String) srcValue + additionalValue;
                }
                if (additionalValue instanceof String) {
                    return srcValue + (String) additionalValue;
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        LHS_STRING_CONCAT(STRING) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                return RHS_STRING_CONCAT.apply(additionalValue, srcValue);
            }
        },

        ADD(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Number && additionalValue instanceof Number) {
                    Number a = (Number) srcValue;
                    Number b = (Number) additionalValue;
                    String inferredType = KnownOperator.getInferredType(a, b);
                    switch (inferredType) {
                        case "I": {
                            return a.intValue() + b.intValue();
                        }
                        case "L": {
                            return a.longValue() + b.longValue();
                        }
                        case "F": {
                            return a.floatValue() + b.floatValue();
                        }
                        case "D": {
                            return a.doubleValue() + b.doubleValue();
                        }
                        default: {
                            //same as double
                            return a.doubleValue() + b.doubleValue();
                        }
                    }
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        LHS_SUB(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                return RHS_SUB.apply(additionalValue, srcValue);
            }
        },
        RHS_SUB(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Number && additionalValue instanceof Number) {
                    Number a = (Number) srcValue;
                    Number b = (Number) additionalValue;
                    String inferredType = KnownOperator.getInferredType(a, b);
                    switch (inferredType) {
                        case "I":
                            return a.intValue() - b.intValue();
                        case "L":
                            return a.longValue() - b.longValue();
                        case "F":
                            return a.floatValue() - b.floatValue();
                        case "D":
                            return a.doubleValue() - b.doubleValue();
                        case "S":
                        default:
                            //same as double
                            return a.doubleValue() - b.doubleValue();
                    }
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        MUL(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Number && additionalValue instanceof Number) {
                    Number a = (Number) srcValue;
                    Number b = (Number) additionalValue;
                    String inferredType = KnownOperator.getInferredType(a, b);
                    switch (inferredType) {
                        case "I":
                            return a.intValue() * b.intValue();
                        case "L":
                            return a.longValue() * b.longValue();
                        case "F":
                            return a.floatValue() * b.floatValue();
                        case "D":
                            return a.doubleValue() * b.doubleValue();
                        case "S":
                        default:
                            //same as double
                            return a.doubleValue() * b.doubleValue();
                    }
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        LHS_DIV(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                return RHS_DIV.apply(additionalValue, srcValue);
            }
        },
        RHS_DIV(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Number && additionalValue instanceof Number) {
                    Number a = (Number) srcValue;
                    Number b = (Number) additionalValue;
                    String inferredType = KnownOperator.getInferredType(a, b);
                    switch (inferredType) {
                        case "I":
                            return a.intValue() / b.intValue();
                        case "L":
                            return a.longValue() / b.longValue();
                        case "F":
                            return a.floatValue() / b.floatValue();
                        case "D":
                            return a.doubleValue() / b.doubleValue();
                        case "S":
                        default:
                            //same as double
                            return a.doubleValue() / b.doubleValue();
                    }
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        AND(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean && additionalValue instanceof Boolean) {
                    return (Boolean) srcValue && (Boolean) additionalValue;
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        OR(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean && additionalValue instanceof Boolean) {
                    return (Boolean) srcValue || (Boolean) additionalValue;
                }
                return throwError(srcValue, additionalValue, isUnary());
            }

        },
        NOT(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean) {
                    return !(Boolean) srcValue;
                }
                return throwError(srcValue, additionalValue, isUnary());
            }

            @Override
            public boolean isUnary() {
                return true;
            }
        },
        XOR(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean && additionalValue instanceof Boolean) {
                    return (Boolean) srcValue ^ (Boolean) additionalValue;
                }
                return throwError(srcValue, additionalValue, isUnary());
            }
        },
        TO_EPOCHMILLIS(STRING) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                //parsing date from ISO 8601
                Instant strToDate = Instant.parse((String) srcValue);
                return strToDate.toEpochMilli();
            }

            @Override
            public boolean isUnary() {
                return true;
            }

        },
        TO_ISO8601(NUMERIC) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                Long epochMillis = (Long) srcValue;
                String format = "yyyy-MM-dd HH:mm:ss z";
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getDefault());
                return sdf.format(new Date(epochMillis));
            }

            @Override
            public boolean isUnary() {
                return true;
            }
        },

        TO_SECONDS_FROM_DURATION(STRING) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                String duration = (String) srcValue;
                Duration d = Duration.parse(duration);
                return d.toSeconds();
            }

            @Override
            public boolean isUnary() {
                return true;
            }
        },

        ;


        private static final Set<String> allowedOperations;
        private final String type;

        KnownOperator(String type) {
            this.type = type;
        }

        static {
            allowedOperations = new HashSet<>();
            for (KnownOperator op : KnownOperator.values()) {
                allowedOperations.add(op.name());
            }
        }

        @Override
        public boolean isUnary() {
            return false;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public boolean supports(Class<?> type) {
            if(type == null) {
                return false;
            }
            if(type.isAssignableFrom(Number.class) && getType().equals(NUMERIC)) {
                return true;
            }
            if(type.isAssignableFrom(Boolean.class) && getType().equals(BOOLEAN)) {
                return true;
            }
            if(type.isAssignableFrom(String.class) && getType().equals(STRING)) {
                return true;
            }
            return false;
        }

        private static String getInferredType(Number a, Number b) {
            String ab = (a.getClass().getName().substring(10, 11).toUpperCase())
                    + (b.getClass().getName().substring(10, 11).toUpperCase());
            return TypeSystem.get(ab);
        }

        private String getClassName(Object srcValue) {
            if (srcValue == null) {
                return "null";
            }
            return srcValue.getClass().getSimpleName();
        }

        protected Object throwError(Object srcValue,
                                    Object additionalValue,
                                    boolean isUnary) throws RuntimeException {
            String message;
            if (isUnary) {
                message = String.format("operator: %s has invalid operand type: %s", name(), getClassName(srcValue));
            } else {
                message = String.format("operator: %s has invalid operand types a is '%s', b is '%s'", name(), getClassName(srcValue), getClassName(additionalValue));
            }
            throw new JsonPathException(message);
        }
    }
}
