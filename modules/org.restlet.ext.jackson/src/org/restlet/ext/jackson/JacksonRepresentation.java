/**
 * Copyright 2005-2013 Restlet S.A.S.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL
 * 1.0 (the "Licenses"). You can select the license that you prefer but you may
 * not use this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.restlet.com/products/restlet-framework
 * 
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.jackson;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

/**
 * Representation based on the Jackson library. It can serialize and deserialize
 * automatically in JSON, JSON binary (Smile), XML, YAML and CSV. <br>
 * <br>
 * SECURITY WARNING: Using XML parsers configured to not prevent nor limit
 * document type definition (DTD) entity resolution can expose the parser to an
 * XML Entity Expansion injection attack.
 * 
 * @see <a href="http://jackson.codehaus.org/">Jackson project</a>
 * @see <a
 *      href="https://github.com/restlet/restlet-framework-java/wiki/XEE-security-enhancements">XML
 *      Entity Expansion injection attack</a>
 * @author Jerome Louvel
 * @param <T>
 *            The type to wrap.
 */
public class JacksonRepresentation<T> extends OutputRepresentation {

    /**
     * Specifies that the parser will expand entity reference nodes. By default
     * the value of this is set to false.
     */
    private volatile boolean expandingEntityRefs;

    /** The (parsed) object to format. */
    private T object;

    /** The object class to instantiate. */
    private Class<T> objectClass;

    /** The modifiable Jackson object mapper. */
    private ObjectMapper objectMapper;

    /** The modifiable Jackson object reader. */
    private ObjectReader objectReader;

    /** The modifiable Jackson object writer. */
    private ObjectWriter objectWriter;

    /** The representation to parse. */
    private Representation representation;

    /**
     * Indicates the desire for validating this type of XML representations
     * against a DTD. Note that for XML schema or Relax NG validation, use the
     * "schema" property instead.
     * 
     * @see DocumentBuilderFactory#setValidating(boolean)
     */
    private volatile boolean validatingDtd;

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The target media type.
     * @param object
     *            The object to format.
     */
    @SuppressWarnings("unchecked")
    public JacksonRepresentation(MediaType mediaType, T object) {
        super(mediaType);
        this.object = object;
        this.objectClass = (Class<T>) ((object == null) ? null : object
                .getClass());
        this.representation = null;
        this.objectMapper = null;
        this.objectReader = null;
        this.objectWriter = null;
    }

    /**
     * Constructor.
     * 
     * @param representation
     *            The representation to parse.
     * @param objectClass
     *            The object class to instantiate.
     */
    public JacksonRepresentation(Representation representation,
            Class<T> objectClass) {
        super(representation.getMediaType());
        this.object = null;
        this.objectClass = objectClass;
        this.representation = representation;
        this.objectMapper = null;
        this.objectReader = null;
        this.objectWriter = null;
    }

    /**
     * Constructor for the JSON media type.
     * 
     * @param object
     *            The object to format.
     */
    public JacksonRepresentation(T object) {
        this(MediaType.APPLICATION_JSON, object);
    }

    /**
     * Creates a Jackson object mapper based on a media type. It supports JSON,
     * JSON Smile
     * 
     * @return The Jackson object mapper.
     */
    protected ObjectMapper createObjectMapper() {
        ObjectMapper result = null;

        if (MediaType.APPLICATION_JSON.isCompatible(getMediaType())) {
            JsonFactory jsonFactory = new JsonFactory();
            jsonFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
            result = new ObjectMapper(jsonFactory);
        } else if (MediaType.APPLICATION_JSON_SMILE
                .isCompatible(getMediaType())) {
            SmileFactory smileFactory = new SmileFactory();
            smileFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
            result = new ObjectMapper(smileFactory);
        } else {
            JsonFactory jsonFactory = new JsonFactory();
            jsonFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
            result = new ObjectMapper(jsonFactory);
        }

        return result;
    }

    /**
     * Creates a Jackson object reader based on a mapper. Has a special handling
     * for CSV media types.
     * 
     * @return The Jackson object reader.
     */
    protected ObjectReader createObjectReader() {
        ObjectReader result = null;
        result = getObjectMapper().reader(getObjectClass());
        return result;
    }

    /**
     * Creates a Jackson object writer based on a mapper. Has a special handling
     * for CSV media types.
     * 
     * @return The Jackson object writer.
     */
    protected ObjectWriter createObjectWriter() {
        ObjectWriter result = null;
        result = getObjectMapper().writerWithType(getObjectClass());
        return result;
    }

    /**
     * Returns the wrapped object, deserializing the representation with Jackson
     * if necessary.
     * 
     * @return The wrapped object.
     * @throws IOException
     */
    public T getObject() throws IOException {
        T result = null;

        if (this.object != null) {
            result = this.object;
        } else if (this.representation != null) {
            result = getObjectReader().readValue(
                    this.representation.getStream());
        }

        return result;
    }

    /**
     * Returns the object class to instantiate.
     * 
     * @return The object class to instantiate.
     */
    public Class<T> getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the modifiable Jackson object mapper. Useful to customize
     * mappings.
     * 
     * @return The modifiable Jackson object mapper.
     */
    public ObjectMapper getObjectMapper() {
        if (this.objectMapper == null) {
            this.objectMapper = createObjectMapper();
        }

        return this.objectMapper;
    }

    /**
     * Returns the modifiable Jackson object reader. Useful to customize
     * deserialization.
     * 
     * @return The modifiable Jackson object reader.
     */
    public ObjectReader getObjectReader() {
        if (this.objectReader == null) {
            this.objectReader = createObjectReader();
        }

        return this.objectReader;
    }

    /**
     * Returns the modifiable Jackson object writer. Useful to customize
     * serialization.
     * 
     * @return The modifiable Jackson object writer.
     */
    public ObjectWriter getObjectWriter() {
        if (this.objectWriter == null) {
            this.objectWriter = createObjectWriter();
        }

        return this.objectWriter;
    }

    /**
     * Indicates if the parser will expand entity reference nodes. By default
     * the value of this is set to true.
     * 
     * @param expandEntityRefs
     *            True if the parser will expand entity reference nodes.
     */
    public void setExpandingEntityRefs(boolean expandEntityRefs) {
        this.expandingEntityRefs = expandEntityRefs;
    }

    /**
     * Sets the object to format.
     * 
     * @param object
     *            The object to format.
     */
    public void setObject(T object) {
        this.object = object;
    }

    /**
     * Sets the object class to instantiate.
     * 
     * @param objectClass
     *            The object class to instantiate.
     */
    public void setObjectClass(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * Sets the Jackson object mapper.
     * 
     * @param objectMapper
     *            The Jackson object mapper.
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sets the Jackson object reader.
     * 
     * @param objectReader
     *            The Jackson object reader.
     */
    public void setObjectReader(ObjectReader objectReader) {
        this.objectReader = objectReader;
    }

    /**
     * Sets the Jackson object writer.
     * 
     * @param objectWriter
     *            The Jackson object writer.
     */
    public void setObjectWriter(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (representation != null) {
            representation.write(outputStream);
        } else if (object != null) {
            getObjectWriter().writeValue(outputStream, object);
        }
    }
}
