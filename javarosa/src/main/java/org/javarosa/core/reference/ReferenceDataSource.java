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

package org.javarosa.core.reference;

import org.javarosa.core.services.locale.LocaleDataSource;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.util.OrderedMap;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ReferenceDataSource is a source of locale data which
 * is located at a location which is defined by a ReferenceURI.
 *
 * @author Clayton Sims
 * @date Jun 1, 2009
 */
public class ReferenceDataSource implements LocaleDataSource {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceDataSource.class);

    String referenceURI;

    /**
     * NOTE: FOR SERIALIZATION ONLY!
     */
    public ReferenceDataSource() {

    }

    /**
     * Creates a new Data Source for Locale data with the given resource URI.
     *
     * @param referenceURI a URI to the resource file from which data should be loaded
     * @throws NullPointerException if resourceURI is null
     */
    public ReferenceDataSource(String referenceURI) {
        if (referenceURI == null) {
            throw new NullPointerException("Reference URI cannot be null when creating a Resource File Data Source");
        }
        this.referenceURI = referenceURI;
    }

    @Override
    public OrderedMap<String, String> getLocalizedText() {
        try {
            InputStream is = ReferenceManager.instance().deriveReference(referenceURI).getStream();
            return LocalizationUtils.parseLocaleInput(is);
        } catch (IOException e) {
            logger.error("Error", e);
            throw new RuntimeException("IOException while getting localized text at reference " + referenceURI);
        } catch (InvalidReferenceException e) {
            logger.error("Error", e);
            throw new RuntimeException("Invalid Reference! " + referenceURI);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
        throws IOException, DeserializationException {
        referenceURI = in.readUTF();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeUTF(referenceURI);
    }
}
