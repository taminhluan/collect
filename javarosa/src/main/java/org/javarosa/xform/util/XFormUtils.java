/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xform.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xform.parse.IXFormParserFactory;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.parse.XFormParserFactory;
import org.kxml2.kdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static Utility methods pertaining to XForms.
 *
 * @author Clayton Sims
 *
 */
public class XFormUtils {
    private static final Logger logger = LoggerFactory.getLogger(XFormUtils.class);

    private static IXFormParserFactory _factory = new XFormParserFactory();

    public static IXFormParserFactory setXFormParserFactory(IXFormParserFactory factory) {
        IXFormParserFactory oldFactory = _factory;
        _factory = factory;
        return oldFactory;
    }

    public static FormDef getFormFromResource (String resource) {
        InputStream is = System.class.getResourceAsStream(resource);
        if (is == null) {
            logger.error("Can't find form resource {}. Is it in the JAR?", resource);
            return null;
        }

        return getFormFromInputStream(is);
    }


    public static FormDef getFormRaw(InputStreamReader isr) throws XFormParseException, IOException{
        return _factory.getXFormParser(isr).parse();
    }

    /**
     * Parses a form with an external secondary instance, and returns a FormDef.
     *
     * @param is                         the InputStream containing the form
     * @return a FormDef for the parsed form
     * @throws XFormParseException if the form can’t be parsed
     */
    public static FormDef getFormFromInputStream(InputStream is) throws XFormParseException {
        return getFormFromInputStream(is, null);
    }

    /**
     * @see #getFormFromInputStream(InputStream)
     *
     * @param lastSavedSrc The src of the last-saved instance of this form (for auto-filling). If null,
     *                     no data will be loaded and the instance will be blank.
     */
    public static FormDef getFormFromInputStream(InputStream is, String lastSavedSrc) throws XFormParseException {
        InputStreamReader isr = null;
        try {
            try {
                isr = new InputStreamReader(is, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new XFormParseException("IO Exception during parse! " + uee.getMessage());
            }

            XFormParser xFormParser = _factory.getXFormParser(isr);
            return xFormParser.parse(lastSavedSrc);
        } catch(IOException e) {
            throw new XFormParseException("IO Exception during parse! " + e.getMessage());
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException e) {
                logger.error("IO Exception while closing stream.", e);
            }
        }
    }

    public static FormDef getFormFromSerializedResource(String resource) {
        FormDef returnForm = null;
        InputStream is = System.class.getResourceAsStream(resource);
        DataInputStream dis = null;
        try {
            if (is != null) {
                dis = new DataInputStream(is);
                returnForm = (FormDef) ExtUtil.read(dis, FormDef.class);
            } else {
                logger.info("ResourceStream NULL");
            }
        } catch (IOException e) {
            logger.error("Error", e);
        } catch (DeserializationException e) {
            logger.error("Error", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("Error", e);
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    logger.error("Error", e);
                }
            }
        }
        return returnForm;
    }


    /////Parser Attribute warning stuff

    public static List<String> getAttributeList(Element e){
        List<String> atts = new ArrayList<String>(e.getAttributeCount());
        for(int i=0;i<e.getAttributeCount();i++){
            atts.add(e.getAttributeName(i));
        }

        return atts;
    }

    public static List<String> getUnusedAttributes(Element e,List<String> usedAtts){
      List<String> unusedAtts = getAttributeList(e);
        for(int i=0;i<usedAtts.size();i++){
            if(unusedAtts.contains(usedAtts.get(i))){
                unusedAtts.remove(usedAtts.get(i));
            }
        }

        return unusedAtts;
    }

    public static String unusedAttWarning(Element e, List<String> usedAtts) {
        String warning = "Warning: ";
        List<String> unusedAttributes = getUnusedAttributes(e, usedAtts);
        warning += unusedAttributes.size() + " Unrecognized attributes found in Element [" + e.getName() +
                "] and will be ignored: ";
        warning += "[";
        for (int i=0; i < unusedAttributes.size(); i++) {
            warning += unusedAttributes.get(i);
            if (i != unusedAttributes.size() - 1) warning += ",";
        }
        warning += "] ";

        return warning;
    }

    public static boolean showUnusedAttributeWarning(Element e, List<String> usedAtts){
        return getUnusedAttributes(e,usedAtts).size()>0;
    }

    /**
     * Is this element an Output tag?
     * @param e
     * @return
     */
    public static boolean isOutput(Element e){
        if(e.getName().toLowerCase().equals("output")) return true;
        else return false;
    }

}
