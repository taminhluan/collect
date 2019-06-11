package org.javarosa.xform.parse;

import java.util.HashMap;
import java.util.Map;

import static org.javarosa.core.model.Constants.DATATYPE_BARCODE;
import static org.javarosa.core.model.Constants.DATATYPE_BINARY;
import static org.javarosa.core.model.Constants.DATATYPE_BOOLEAN;
import static org.javarosa.core.model.Constants.DATATYPE_CHOICE;
import static org.javarosa.core.model.Constants.DATATYPE_MULTIPLE_ITEMS;
import static org.javarosa.core.model.Constants.DATATYPE_DATE;
import static org.javarosa.core.model.Constants.DATATYPE_DATE_TIME;
import static org.javarosa.core.model.Constants.DATATYPE_DECIMAL;
import static org.javarosa.core.model.Constants.DATATYPE_GEOPOINT;
import static org.javarosa.core.model.Constants.DATATYPE_GEOSHAPE;
import static org.javarosa.core.model.Constants.DATATYPE_GEOTRACE;
import static org.javarosa.core.model.Constants.DATATYPE_INTEGER;
import static org.javarosa.core.model.Constants.DATATYPE_LONG;
import static org.javarosa.core.model.Constants.DATATYPE_TEXT;
import static org.javarosa.core.model.Constants.DATATYPE_TIME;
import static org.javarosa.core.model.Constants.DATATYPE_UNSUPPORTED;
import static org.javarosa.xform.parse.Constants.RANK;
import static org.javarosa.xform.parse.Constants.SELECT;
import static org.javarosa.xform.parse.Constants.SELECTONE;

class TypeMappings {

    private static HashMap<String, Integer> typeMappings = new HashMap<String, Integer>() {{

        //xsd
        put("string",       DATATYPE_TEXT);
        put("integer",      DATATYPE_INTEGER);
        put("long",         DATATYPE_LONG);
        put("int",          DATATYPE_INTEGER);
        put("decimal",      DATATYPE_DECIMAL);
        put("double",       DATATYPE_DECIMAL);
        put("float",        DATATYPE_DECIMAL);
        put("dateTime",     DATATYPE_DATE_TIME);
        put("date",         DATATYPE_DATE);
        put("time",         DATATYPE_TIME);
        put("gYear",        DATATYPE_UNSUPPORTED);
        put("gMonth",       DATATYPE_UNSUPPORTED);
        put("gDay",         DATATYPE_UNSUPPORTED);
        put("gYearMonth",   DATATYPE_UNSUPPORTED);
        put("gMonthDay",    DATATYPE_UNSUPPORTED);
        put("boolean",      DATATYPE_BOOLEAN);
        put("base64Binary", DATATYPE_UNSUPPORTED);
        put("hexBinary",    DATATYPE_UNSUPPORTED);
        put("anyURI",       DATATYPE_UNSUPPORTED);

        //xforms
        put("listItem",     DATATYPE_CHOICE);
        put("listItems",    DATATYPE_MULTIPLE_ITEMS);

        //non-standard
        put(SELECTONE,      DATATYPE_CHOICE);
        put(SELECT,         DATATYPE_MULTIPLE_ITEMS);
        put(RANK,           DATATYPE_MULTIPLE_ITEMS);
        put("geopoint",     DATATYPE_GEOPOINT);
        put("geoshape",     DATATYPE_GEOSHAPE);
        put("geotrace",     DATATYPE_GEOTRACE);
        put("barcode",      DATATYPE_BARCODE);
        put("binary",       DATATYPE_BINARY);
    }};

    /** Returns a modifiable map of type strings to org.javarosa.core.model DATATYPE_ values */
    public static Map<String, Integer> getMap() {
        return new HashMap<>(typeMappings); // Return a copy, because it may be modified
    }
}
