package com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model;

import java.text.SimpleDateFormat;
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
                throw new IllegalArgumentException("source is of type " + srcValue.getClass().getName() + " and " + additionalValue.getClass().getName() + " not supported");
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
                throw new IllegalArgumentException("source is of type " + srcValue.getClass().getName() + " and " + additionalValue.getClass().getName() + " not supported");
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
                throw new IllegalArgumentException("source is of type " + srcValue.getClass().getName() + " and " + additionalValue.getClass().getName() + " not supported");
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
                throw new IllegalArgumentException("source is of type " + srcValue + " and " + additionalValue.getClass().getName() + " not supported");
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
                throw new IllegalArgumentException("source is of type " + srcValue + " and " + additionalValue.getClass().getName() + " not supported");
            }
        },
        AND(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean && additionalValue instanceof Boolean) {
                    return (Boolean) srcValue && (Boolean) additionalValue;
                }
                throw new IllegalArgumentException("source is of type " + srcValue + " and " + additionalValue.getClass().getName() + " not supported");
            }
        },
        OR(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean && additionalValue instanceof Boolean) {
                    return (Boolean) srcValue || (Boolean) additionalValue;
                }
                throw new IllegalArgumentException("source is of type " + srcValue + " and " + additionalValue.getClass().getName() + " not supported");
            }

        },
        NOT(BOOLEAN) {
            @Override
            public Object apply(Object srcValue, Object additionalValue) {
                if (srcValue instanceof Boolean) {
                    return !(Boolean) srcValue;
                }
                throw new IllegalArgumentException("source is of type " + srcValue);
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
                throw new IllegalArgumentException("source is of type " + srcValue + " and " + additionalValue.getClass().getName() + " not supported");
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
        };

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

        public static Set<String> getAllowedOperations() {
            return allowedOperations;
        }

        @Override
        public boolean isUnary() {
            return false;
        }

        public String getType() {
            return type;
        }

        @Override
        public Object apply(Object srcValue, Object additionalValue) {
            throw new UnsupportedOperationException();
        }

        private static String getInferredType(Number a, Number b) {
            String ab = (a.getClass().getName().substring(10, 11).toUpperCase())
                    + (b.getClass().getName().substring(10, 11).toUpperCase());
            return TypeSystem.get(ab);
        }
    }
}
