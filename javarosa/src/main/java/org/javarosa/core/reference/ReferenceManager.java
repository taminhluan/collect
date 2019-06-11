package org.javarosa.core.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>The reference manager is a singleton class which is responsible for deriving reference URI's into
 * references at runtime.</p>
 *
 * <p>Raw reference factories (which are capable of actually creating fully
 * qualified reference objects) are added with the addFactory() method. The most common method
 * of doing so is to implement the PrefixedRootFactory as either a full class, or an anonymous inner class,
 * providing the roots available in the current environment and the code for constructing a reference from them.</p>
 *
 * <p>RootTranslators (which rely on other factories) are used to describe that a particular reference style (generally
 * a high level reference like "jr://media/" or "jr://images/") should be translated to another available reference in this
 * environment like "jr://file/". Root Translators do not directly derive references, but rather translate them to what
 * the reference should look like in the current circumstances.</p>
 *
 * @author ctsims
 */
public class ReferenceManager {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceManager.class.getSimpleName());
    private static ReferenceManager instance;

    private List<RootTranslator> translators;
    private List<ReferenceFactory> factories;
    private List<RootTranslator> sessionTranslators;

    private ReferenceManager() {
        logger.debug("created");
        translators         = new ArrayList<>();
        factories           = new ArrayList<>();
        sessionTranslators  = new ArrayList<>();
    }

    public void reset() {
        int t = translators.size();
        int f = factories.size();
        int st = sessionTranslators.size();

        if (t + f + st > 0) {
            translators.clear();
            factories.clear();
            sessionTranslators.clear();
            logger.debug("reset translators ({}), session translators ({}), and factories ({})", t, st, f);
        }
    }

    /**
     * @return Singleton accessor to the global
     * ReferenceManager.
     */
    public static ReferenceManager instance() {
        if (instance == null) {
            instance = new ReferenceManager();
        }
        return instance;
    }

    /**
     * @see ReferenceManager#instance
     * @deprecated use instance() instead
     */
    @Deprecated
    public static ReferenceManager __() {
        return instance();
    }

    /**
     * @return The available reference factories
     */
    public ReferenceFactory[] getFactories() {
        return translators.toArray(new ReferenceFactory[translators.size()]);
    }

    /**
     * Adds a new Translator to the current environment.
     */
    public void addRootTranslator(RootTranslator translator) {
        if (!translators.contains(translator)) {
            translators.add(translator);
            logger.debug("added root translator {}", translator);
        } else logger.debug("skipped adding already-present root translator {}", translator);
    }

    /**
     * Adds a factory for deriving reference URI's into references
     *
     * @param factory A raw ReferenceFactory capable of creating
     *                a reference.
     */
    public void addReferenceFactory(ReferenceFactory factory) {
        if (!factories.contains(factory)) {
            factories.add(factory);
            logger.debug("added reference factory {}", factory);
        } else logger.debug("skipped adding already-present reference factory {}", factory);
    }

    public boolean removeReferenceFactory(ReferenceFactory factory) {
        boolean removed = factories.remove(factory);
        logger.debug("factory {} was " + (removed ? "removed" : "not removed because it was not present"), factory);
        return removed;
    }

    /**
     * Derives a global reference from a URI in the current environment.
     *
     * @param uri The URI representing a global reference.
     * @return A reference which is identified by the provided URI.
     * @throws InvalidReferenceException If the current reference could
     *                                   not be derived by the current environment
     */
    public Reference deriveReference(String uri) throws InvalidReferenceException {
        return deriveReference(uri, (String) null);
    }

    /**
     * Derives a reference from a URI in the current environment.
     *
     * @param uri     The URI representing a reference.
     * @param context A reference which provides context for any
     *                relative reference accessors.
     * @return A reference which is identified by the provided URI.
     * @throws InvalidReferenceException If the current reference could
     *                                   not be derived by the current environment
     */
    public Reference deriveReference(String uri, Reference context) throws InvalidReferenceException {
        return deriveReference(uri, context.getURI());
    }

    /**
     * Derives a reference from a URI in the current environment.
     *
     * @param uri     The URI representing a reference.
     * @param context A reference URI which provides context for any
     *                relative reference accessors.
     * @return A reference which is identified by the provided URI.
     * @throws InvalidReferenceException If the current reference could
     *                                   not be derived by the current environment, or if the context URI
     *                                   is not valid in the current environment.
     */
    public Reference deriveReference(String uri, String context) throws InvalidReferenceException {
        if (uri == null) {
            throw new InvalidReferenceException("Null references aren't valid", uri);
        }

        //Relative URI's need to determine their context first.
        if (isRelative(uri)) {
            //Clean up the relative reference to lack any leading separators.
            if (uri.startsWith("./")) {
                uri = uri.substring(2);
            }

            if (context == null) {
                throw new RuntimeException("Attempted to retrieve local reference with no context");
            } else {
                Reference reference = derivingRoot(context).derive(uri, context);
                logger.debug("{} was derived from {}", reference.getLocalURI(), uri);
                return reference;
            }
        } else {
            Reference reference = derivingRoot(uri).derive(uri);
            logger.debug("{} was derived from {}", reference.getLocalURI(), uri);
            return reference;
        }
    }

    /**
     * Adds a root translator that is maintained over the course of a session. It will be globally
     * available until the session is cleared using the "clearSession" method.
     *
     * @param translator A Root Translator that will be added to the current session
     */
    public void addSessionRootTranslator(RootTranslator translator) {
        sessionTranslators.add(translator);
        logger.debug("added session root translator {}", translator);
    }

    /**
     * Wipes out all of the translators being maintained in the current session (IE: Any translators
     * added via "addSessionRootTranslator". Used to manage a temporary set of translations for a limited
     * amount of time.
     */
    public void clearSession() {
        sessionTranslators.clear();
        logger.debug("cleared all session translators");
    }

    /**
     * Returns the first deriving factory in the three ReferenceFactory collections.
     */
    private ReferenceFactory derivingRoot(String uri) throws InvalidReferenceException {
        // See https://github.com/opendatakit/javarosa/pull/394#discussion_r245902058 for why `? extends` is needed
        for (List<? extends ReferenceFactory> rfs : Arrays.asList(sessionTranslators, translators, factories))
            for (ReferenceFactory rf : rfs)
                if (rf.derives(uri))
                    return rf;

        throw new InvalidReferenceException(getPrettyPrintException(uri), uri);
    }

    private String getPrettyPrintException(String uri) {
        if (uri == null || uri.length() == 0) {
            return "Attempt to derive a blank reference";
        }
        try {
            String uriRoot = uri;
            String jrRefMessagePortion = "reference type";
            if (uri.contains("jr://")) {
                uriRoot = uri.substring("jr://".length());
                jrRefMessagePortion = "javarosa jr:// reference root";
            }
            //For http:// style uri's
            int endOfRoot = uriRoot.indexOf("://") + "://".length();
            if (endOfRoot == "://".length() - 1) {
                endOfRoot = uriRoot.indexOf("/");
            }
            if (endOfRoot != -1) {
                uriRoot = uriRoot.substring(0, endOfRoot);
            }
            String message = "The reference \"" + uri + "\" was invalid and couldn't be understood. The " + jrRefMessagePortion + " \"" + uriRoot +
                "\" is not available on this system and may have been mis-typed. Some available roots: ";
            for (RootTranslator root : sessionTranslators) {
                message += "\n" + root.prefix;
            }

            //Now, try any/all roots referenced at runtime.
            for (RootTranslator root : translators) {
                message += "\n" + root.prefix;
            }

            //Now try all of the raw connectors available
            for (ReferenceFactory root : factories) {

                //TODO: Skeeeeeeeeeeeeetch
                try {

                    if (root instanceof PrefixedRootFactory) {
                        for (String rootName : ((PrefixedRootFactory) root).roots) {
                            message += "\n" + rootName;
                        }
                    } else {
                        message += "\n" + root.derive("").getURI();
                    }
                } catch (Exception e) {

                }
            }
            return message;
        } catch (Exception e) {
            return "Couldn't process the reference " + uri + " . It may have been entered incorrectly. " +
                "Note that this doesn't mean that this doesn't mean the file or location referenced " +
                "couldn't be found, the reference itself was not understood.";
        }
    }

    /**
     * @return Whether the provided URI describes a relative reference.
     */
    public static boolean isRelative(String URI) {
        return URI.startsWith("./");
    }

    /** @deprecated use deriveReference instead */
    @Deprecated
    public Reference DeriveReference(String uri) throws InvalidReferenceException {
        return deriveReference(uri);
    }

    /** @deprecated use deriveReference instead */
    @Deprecated
    public Reference DeriveReference(String uri, Reference context) throws InvalidReferenceException {
        return deriveReference(uri, context);
    }

    /** @deprecated use deriveReference instead */
    @Deprecated
    public Reference DeriveReference(String uri, String context) throws InvalidReferenceException {
        return deriveReference(uri, context);
    }
}
