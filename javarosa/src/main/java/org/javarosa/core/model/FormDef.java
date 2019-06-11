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

package org.javarosa.core.model;

import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.IDag.EventNotifierAccessor;
import org.javarosa.core.model.actions.Action;
import org.javarosa.core.model.actions.ActionController;
import org.javarosa.core.model.condition.Condition;
import org.javarosa.core.model.condition.Constraint;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.condition.Recalculate;
import org.javarosa.core.model.condition.Triggerable;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.MultipleItemsData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InvalidReferenceException;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.QuestionPreloader;
import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.debug.EvaluationResult;
import org.javarosa.debug.Event;
import org.javarosa.debug.EventNotifier;
import org.javarosa.debug.EventNotifierSilent;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.javarosa.xpath.XPathException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definition of a form. This has some meta data about the form definition and a
 * collection of groups together with question branching or skipping rules.
 *
 * @author Daniel Kayiwa, Drew Roos
 */
public class FormDef implements IFormElement, Localizable, Persistable, IMetaData,
    ActionController.ActionResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FormDef.class);

    public static final String STORAGE_KEY = "FORMDEF";
    public static final int TEMPLATING_RECURSION_LIMIT = 10;

    public enum EvalBehavior {
        Legacy,
        April_2014,
        Safe_2014,
        Fast_2014
    }

    public static final EvalBehavior recommendedMode = EvalBehavior.Safe_2014;

    /** used by FormDef() constructor */
    private static EvalBehavior defaultMode = recommendedMode;
    private static EventNotifier defaultEventNotifier = new EventNotifierSilent();

    /** Changes the mode used for evaluations */
    public static final void setEvalBehavior(EvalBehavior mode) {
        defaultMode = mode;
    }

    public static void setDefaultEventNotifier(EventNotifier eventNotifier) {
        defaultEventNotifier = eventNotifier;
    }

    /**
     * Takes a (possibly relative) reference, and makes it absolute based on its parent.
     * Moved from the parser to this class so it can be used more cleanly by ItemsetBinding.
     */
    public static IDataReference getAbsRef(IDataReference ref, TreeReference parentRef) {
        TreeReference tref;

        if (!parentRef.isAbsolute()) {
            throw new RuntimeException("XFormParser.getAbsRef: parentRef must be absolute");
        }

        if (ref != null) {
            tref = (TreeReference) ref.getReference();
        } else {
            tref = TreeReference.selfRef(); //only happens for <group>s with no binding
        }

        tref = tref.anchor(parentRef);
        if (tref == null) {
            throw new XFormParseException("Binding path [" + tref + "] not allowed with parent binding of [" + parentRef + "]");
        }

        return new XPathReference(tref);
    }

    /** A collection of group definitions */
    private List<IFormElement> children;
    /** The numeric unique identifier of the form definition on the local device */
    private int id;
    /** The display title of the form */
    private String title;

    private String name;

    private List<XFormExtension> extensions;

    /** A unique external name that is used to identify the form between machines */
    private Localizer localizer;

    /** IConditionExpr contents of output tags that serve as parameterized arguments to captions */
    private List<IConditionExpr> outputFragments;

    private IDag dagImpl;

    private EvaluationContext exprEvalContext;

    private QuestionPreloader preloader = new QuestionPreloader();

    // XML ID's cannot start with numbers, so this should never conflict
    private static String DEFAULT_SUBMISSION_PROFILE = "1";

    private HashMap<String, SubmissionProfile> submissionProfiles;

    private HashMap<String, DataInstance> formInstances;
    private FormInstance mainInstance = null;

    private ActionController actionController;

    private EventNotifier eventNotifier;

    private List<String> parseWarnings = new ArrayList<>();
    private List<String> parseErrors = new ArrayList<>();

    public FormDef() {
        this(defaultMode, defaultEventNotifier);
    }

    public FormDef(EvalBehavior mode, EventNotifier eventNotifier) {
        setID(-1);
        setChildren(null);
        final EventNotifierAccessor ia = new EventNotifierAccessor() {

            @Override
            public EventNotifier getEventNotifier() {
                return FormDef.this.getEventNotifier();
            }
        };

        switch (mode) {
            case Legacy:
                dagImpl = new LegacyDagImpl(ia);
                break;
            case April_2014:
                dagImpl = new April2014DagImpl(ia);
                break;
            case Safe_2014:
                dagImpl = new Safe2014DagImpl(ia);
                break;
            case Fast_2014:
                dagImpl = new Fast2014DagImpl(ia);
                break;
            default:
                throw new IllegalStateException("Unexpected mode: " + mode);
        }
        // This is kind of a wreck...
        resetEvaluationContext();
        outputFragments = new ArrayList<>();
        submissionProfiles = new HashMap<>();
        formInstances = new HashMap<>();
        extensions = new ArrayList<>();
        actionController = new ActionController();

        this.eventNotifier = eventNotifier;
    }

    public EventNotifier getEventNotifier() {
        return eventNotifier;
    }

    public void setEventNotifier(EventNotifier eventNotifier) {
        this.eventNotifier = eventNotifier;
    }

    /** Getters and setters for the lists */
    public void addNonMainInstance(DataInstance instance) {
        formInstances.put(instance.getName(), instance);
        resetEvaluationContext();
    }

    /**
     * Get an instance based on a name
     *
     * @param name string name
     * @return
     */
    public DataInstance getNonMainInstance(String name) {
        if (!formInstances.containsKey(name)) {
            return null;
        }

        return formInstances.get(name);
    }

    public Enumeration<DataInstance> getNonMainInstances() {
        return Collections.enumeration(formInstances.values());
    }

    public void setInstance(FormInstance fi) {
        mainInstance = fi;
        fi.setFormId(getID());
        resetEvaluationContext();

        // construct the references in all the question itemsets
        // now so that the entire main instance is available
        // for term resolution.
        updateItemsetReferences(getChildren());

        attachControlsToInstanceData();
    }

    public FormInstance getMainInstance() {
        return mainInstance;
    }

    public FormInstance getInstance() {
        return getMainInstance();
    }

    public void fireEvent() {

    }

    // ---------- child elements
    @Override
    public void addChild(IFormElement fe) {
        this.children.add(fe);
    }

    @Override
    public IFormElement getChild(int i) {
        if (i < this.children.size())
            return this.children.get(i);

        throw new ArrayIndexOutOfBoundsException("FormDef: invalid child index: " + i + " only "
                + children.size() + " children");
    }

    public IFormElement getChild(FormIndex index) {
        IFormElement element = this;
        while (index != null && index.isInForm()) {
            element = element.getChild(index.getLocalIndex());
            index = index.getNextLevel();
        }
        return element;
    }

    /**
     * Dereference the form index and return a List of all interstitial nodes
     * (top-level parent first; index target last)
     * <p/>
     * Ignore 'new-repeat' node for now; just return/stop at ref to
     * yet-to-be-created repeat node (similar to repeats that already exist)
     *
     * @param index
     * @return
     */
    public List<IFormElement> explodeIndex(FormIndex index) {
        List<Integer> indexes = new ArrayList<>();
        List<Integer> multiplicities = new ArrayList<>();
        List<IFormElement> elements = new ArrayList<>();

        collapseIndex(index, indexes, multiplicities, elements);
        return elements;
    }

    /** Finds the instance node a reference refers to (factoring in multiplicities) */
    public TreeReference getChildInstanceRef(FormIndex index) {
        List<Integer> indexes = new ArrayList<>();
        List<Integer> multiplicities = new ArrayList<>();
        List<IFormElement> elements = new ArrayList<>();

        collapseIndex(index, indexes, multiplicities, elements);
        return getChildInstanceRef(elements, multiplicities);
    }

    public TreeReference getChildInstanceRef(List<IFormElement> elements, List<Integer> multiplicities) {
        if (elements.size() == 0)
            return null;

        IFormElement element = elements.get(elements.size() - 1);
        // get reference for target element
        TreeReference ref = FormInstance.unpackReference(
                element.getBind()).clone();
        for (int i = 0; i < ref.size(); i++) {
            // There has to be a better way to encapsulate this
            if (ref.getMultiplicity(i) != TreeReference.INDEX_ATTRIBUTE) {
                ref.setMultiplicity(i, 0);
            }
        }

        // fill in multiplicities for repeats along the way
        for (int i = 0; i < elements.size(); i++) {
            IFormElement temp = elements.get(i);
            if (temp instanceof GroupDef && ((GroupDef) temp).getRepeat()) {
                TreeReference repRef = FormInstance.unpackReference(temp.getBind());
                if (repRef.isParentOf(ref, false)) {
                    int repMult = multiplicities.get(i);
                    ref.setMultiplicity(repRef.size() - 1, repMult);
                } else {
                    return null; // question/repeat hierarchy is not consistent
                    // with instance instance and bindings
                }
            }
        }

        return ref;
    }

    public void setLocalizer(Localizer l) {
        if (this.localizer != null) {
            this.localizer.unregisterLocalizable(this);
        }

        this.localizer = l;
        if (this.localizer != null) {
            this.localizer.registerLocalizable(this);
        }
    }

    // don't think this should ever be called(!)
    @Override
    public IDataReference getBind() {
        throw new RuntimeException("method not implemented");
    }

    public void setValue(IAnswerData data, TreeReference ref, boolean midSurvey) {
        setValue(data, ref, mainInstance.resolveReference(ref), midSurvey);
    }

    public void setValue(IAnswerData data, TreeReference ref, TreeElement node,
                         boolean midSurvey) {
        IAnswerData oldValue = node.getValue();
        IAnswerDataSerializer answerDataSerializer = new XFormAnswerDataSerializer();

        boolean valueChanged = !objectEquals(answerDataSerializer.serializeAnswerData(oldValue),
                answerDataSerializer.serializeAnswerData(data));

        if (midSurvey && dagImpl.shouldTrustPreviouslyCommittedAnswer() && !valueChanged) {
            return;
        }
        setAnswer(data, node);

        QuestionDef currentQuestion = findQuestionByRef(ref, this);
        if (valueChanged && currentQuestion != null) {
            currentQuestion.getActionController().triggerActionsFromEvent(Action.EVENT_QUESTION_VALUE_CHANGED, this,
                    ref.getParentRef(), null);
        }

        Collection<QuickTriggerable> qts = triggerTriggerables(ref, midSurvey);
        dagImpl.publishSummary("New value", ref, qts);
        // TODO: pre-populate fix-count repeats here?
    }

    /**
     * Copied from commons-lang 2.6: For reviewing purposes only.
     * <p/>
     * <p>
     * Compares two objects for equality, where either one or both objects may be
     * <code>null</code>.
     * </p>
     * <p/>
     * <pre>
     * ObjectUtils.equals(null, null)                  = true
     * ObjectUtils.equals(null, "")                    = false
     * ObjectUtils.equals("", null)                    = false
     * ObjectUtils.equals("", "")                      = true
     * ObjectUtils.equals(Boolean.TRUE, null)          = false
     * ObjectUtils.equals(Boolean.TRUE, "true")        = false
     * ObjectUtils.equals(Boolean.TRUE, Boolean.TRUE)  = true
     * ObjectUtils.equals(Boolean.TRUE, Boolean.FALSE) = false
     * </pre>
     *
     * @param object1 the first object, may be <code>null</code>
     * @param object2 the second object, may be <code>null</code>
     * @return <code>true</code> if the values of both objects are the same
     */
    public static boolean objectEquals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

    public void setAnswer(IAnswerData data, TreeReference ref) {
        setAnswer(data, mainInstance.resolveReference(ref));
    }

    public void setAnswer(IAnswerData data, TreeElement node) {
        node.setAnswer(data);
    }

    /**
     * Deletes the inner-most repeat that this node belongs to and returns the
     * corresponding FormIndex. Behavior is currently undefined if you call this
     * method on a node that is not contained within a repeat.
     *
     * @param index
     * @return
     */
    public FormIndex deleteRepeat(FormIndex index) {
        List<Integer> indexes = new ArrayList<>();
        List<Integer> multiplicities = new ArrayList<>();
        List<IFormElement> elements = new ArrayList<>();
        collapseIndex(index, indexes, multiplicities, elements);

        // loop backwards through the elements, removing objects from each
        // list, until we find a repeat
        // TODO: should probably check to make sure size > 0
        for (int i = elements.size() - 1; i >= 0; i--) {
            IFormElement e = elements.get(i);
            if (e instanceof GroupDef && ((GroupDef) e).getRepeat()) {
                break;
            } else {
                indexes.remove(i);
                multiplicities.remove(i);
                elements.remove(i);
            }
        }

        // build new formIndex which includes everything
        // up to the node we're going to remove
        FormIndex newIndex = buildIndex(indexes, multiplicities, elements);

        TreeReference deleteRef = getChildInstanceRef(newIndex);
        TreeElement deleteElement = mainInstance.resolveReference(deleteRef);
        TreeReference parentRef = deleteRef.getParentRef();
        TreeElement parentElement = mainInstance.resolveReference(parentRef);

        int childMult = deleteElement.getMult();
        parentElement.removeChild(deleteElement);

        // update multiplicities of other child nodes
        for (int i = 0; i < parentElement.getNumChildren(); i++) {
            TreeElement child = parentElement.getChildAt(i);
            if (child.getName().equals(deleteElement.getName()) && child.getMult() > childMult) {
                child.setMult(child.getMult() - 1);

                // When a group changes it multiplicity, then all caches of its
                // children must clear.
                child.clearChildrenCaches();
            }
        }

        dagImpl.deleteRepeatGroup(getMainInstance(), getEvaluationContext(), deleteRef, parentElement, deleteElement);

        return newIndex;
    }

    public void createNewRepeat(FormIndex index) throws InvalidReferenceException {
        TreeReference repeatContextRef = getChildInstanceRef(index);
        TreeElement template = mainInstance.getTemplate(repeatContextRef);

        mainInstance.copyNode(template, repeatContextRef);

        TreeElement newNode = mainInstance.resolveReference(repeatContextRef);
        preloadInstance(newNode);

        // 2013-05-14 - ctsims - Events should get fired _before_ calculate stuff
        // is fired, moved this above triggering triggerables
        actionController.triggerActionsFromEvent(Action.EVENT_JR_INSERT, this, repeatContextRef, this);

        // trigger conditions that depend on the creation of this new node
        triggerTriggerables(repeatContextRef, true);

        TreeReference parentRef = repeatContextRef.getParentRef();
        TreeElement parentElement = mainInstance.resolveReference(parentRef);
        dagImpl.createRepeatGroup(getMainInstance(), getEvaluationContext(), repeatContextRef, parentElement, newNode);
    }

    @Override
    public void processResultOfAction(TreeReference refSetByAction, String event) {
        if (Action.EVENT_JR_INSERT.equals(event)) {
            // CommCare has an implementation if needed
        }
    }

    public boolean isRepeatRelevant(TreeReference repeatRef) {
        boolean relev = true;

        QuickTriggerable qc = dagImpl.getTriggerableForRepeatGroup(repeatRef.genericize());
        if (qc != null) {
            Condition c = (Condition) qc.t;
            relev = c.evalBool(mainInstance, new EvaluationContext(exprEvalContext, repeatRef));
        }

        // check the relevancy of the immediate parent
        if (relev) {
            TreeElement templNode = mainInstance.getTemplate(repeatRef);
            TreeReference parentPath = templNode.getParent().getRef().genericize();
            TreeElement parentNode = mainInstance
                    .resolveReference(parentPath.contextualize(repeatRef));
            relev = parentNode.isRelevant();
        }

        return relev;
    }

    public boolean canCreateRepeat(TreeReference repeatRef, FormIndex repeatIndex) {
        GroupDef repeat = (GroupDef) this.getChild(repeatIndex);

        // Check to see if this repeat can have children added by the user
        if (repeat.noAddRemove) {
            // Check to see if there's a count to use to determine how many
            // children this repeat
            // should have
            if (repeat.getCountReference() != null) {
                int currentMultiplicity = repeatIndex.getElementMultiplicity();

                // Lu Gram: the count XPath needs to be contextualized for nested
                // repeat groups...
                TreeReference countRef = FormInstance.unpackReference(repeat.getCountReference());
                TreeElement countNode = this.getMainInstance().resolveReference(
                        countRef.contextualize(repeatRef));
                if (countNode == null) {
                    throw new RuntimeException("Could not locate the repeat count value expected at "
                            + repeat.getCountReference().getReference().toString());
                }
                // get the total multiplicity possible
                IAnswerData count = countNode.getValue();
                long fullcount = count == null ? 0 : (Integer) count.getValue();

                if (fullcount <= currentMultiplicity) {
                    return false;
                }
            } else {
                // Otherwise the user can never add repeat instances
                return false;
            }
        }

        // TODO: If we think the node is still relevant, we also need to figure
        // out a way to test that assumption against
        // the repeat's constraints.

        return true;
    }

    public void copyItemsetAnswer(QuestionDef q, TreeElement targetNode, IAnswerData data,
                                  boolean midSurvey) throws InvalidReferenceException {
        ItemsetBinding itemset = q.getDynamicChoices();
        TreeReference targetRef = targetNode.getRef();
        TreeReference destRef = itemset.getDestRef().contextualize(targetRef);

        List<Selection> selections = null;
        if (data instanceof MultipleItemsData) {
            selections = (List<Selection>) data.getValue();
        } else if (data instanceof SelectOneData) {
            selections = new ArrayList<>(1);
            selections.add((Selection) data.getValue());
        }
        List<String> selectedValues;
        if (itemset.valueRef != null) {
            selectedValues = new ArrayList<>(selections.size());
            for (Selection selection : selections) {
                selectedValues.add(selection.choice.getValue());
            }
        } else {
            selectedValues = new ArrayList<>(0);
        }

        // delete existing dest nodes that are not in the answer selection
        HashMap<String, TreeElement> existingValues = new HashMap<>();
        List<TreeReference> existingNodes = exprEvalContext.expandReference(destRef);
        for (TreeReference existingNode : existingNodes) {
            TreeElement node = getMainInstance().resolveReference(existingNode);

            if (itemset.valueRef != null) {
                String value = itemset.getRelativeValue().evalReadable(this.getMainInstance(),
                        new EvaluationContext(exprEvalContext, node.getRef()));
                if (selectedValues.contains(value)) {
                    existingValues.put(value, node); // cache node if in selection
                    // and already exists
                }
            }

            // delete from target
            targetNode.removeChild(node);
        }

        // copy in nodes for new answer; preserve ordering in answer
        for (int i = 0; i < selections.size(); i++) {
            Selection s = selections.get(i);
            SelectChoice ch = s.choice;

            TreeElement cachedNode = null;
            if (itemset.valueRef != null) {
                String value = ch.getValue();
                if (existingValues.containsKey(value)) {
                    cachedNode = existingValues.get(value);
                }
            }

            if (cachedNode != null) {
                cachedNode.setMult(i);
                targetNode.addChild(cachedNode);
            } else {
                getMainInstance().copyItemsetNode(ch.copyNode, destRef, this);
            }
        }
        dagImpl.copyItemsetAnswer(getMainInstance(), getEvaluationContext(), destRef, targetNode, midSurvey);
    }

    /**
     * Adds a Condition to the form's Collection.
     *
     * @param t the condition to be set
     */
    public Triggerable addTriggerable(Triggerable t) {
        return dagImpl.addTriggerable(t);
    }

    /**
     * Reports any dependency cycles based upon the triggerIndex array.
     * (Does not require that the DAG be finalized).
     *
     */
    public void reportDependencyCycles() {
        dagImpl.reportDependencyCycles();
    }

    /**
     * Finalizes the DAG associated with the form's triggered conditions. This
     * will create the appropriate ordering and dependencies to ensure the
     * conditions will be evaluated in the appropriate orders.
     *
     * @throws IllegalStateException - If the trigger ordering contains an illegal cycle and the
     *                               triggers can't be laid out appropriately
     */
    public void finalizeTriggerables() throws IllegalStateException {
        //
        // DAGify the triggerables based on dependencies and sort them so that
        // triggerables come only after the triggerables they depend on
        //
        dagImpl.finalizeTriggerables(getMainInstance(), getEvaluationContext());
    }

    /**
     * Walks the current set of conditions, and evaluates each of them with the
     * current context.
     */
    private Collection<QuickTriggerable> initializeTriggerables(TreeReference rootRef, boolean midSurvey) {

        return dagImpl.initializeTriggerables(getMainInstance(), getEvaluationContext(), rootRef, midSurvey);
    }

    /**
     * The entry point for the DAG cascade after a value is changed in the model.
     *
     * @param ref The full contextualized unambiguous reference of the value that
     *            was changed.
     */
    public Collection<QuickTriggerable> triggerTriggerables(TreeReference ref, boolean midSurvey) {
        return dagImpl.triggerTriggerables(getMainInstance(), getEvaluationContext(), ref, midSurvey);
    }

    public ValidateOutcome validate(boolean markCompleted) {

        FormEntryModel formEntryModelToBeValidated = new FormEntryModel(this);
        FormEntryController formEntryControllerToBeValidated = new FormEntryController(formEntryModelToBeValidated);

        return dagImpl.validate(formEntryControllerToBeValidated, markCompleted);
    }

    public boolean evaluateConstraint(TreeReference ref, IAnswerData data) {
        if (data == null) {
            return true;
        }

        TreeElement node = mainInstance.resolveReference(ref);
        Constraint c = node.getConstraint();

        if (c == null) {
            return true;
        }
        EvaluationContext ec = new EvaluationContext(exprEvalContext, ref);
        ec.isConstraint = true;
        ec.candidateValue = data;

        boolean result = c.constraint.eval(mainInstance, ec);

        getEventNotifier().publishEvent(new Event("Constraint", new EvaluationResult(ref, result)));

        return result;
    }

    private void resetEvaluationContext() {
        EvaluationContext ec = new EvaluationContext(null);
        ec = new EvaluationContext(mainInstance, formInstances, ec);
        initEvalContext(ec);
        this.exprEvalContext = ec;
    }

    public EvaluationContext getEvaluationContext() {
        return this.exprEvalContext;
    }

    /**
     * @param ec The new Evaluation Context
     */
    private void initEvalContext(EvaluationContext ec) {
        if (!ec.getFunctionHandlers().containsKey("jr:itext")) {
            final FormDef f = this;
            ec.addFunctionHandler(new IFunctionHandler() {
                @Override
                public String getName() {
                    return "jr:itext";
                }

                @Override
                public Object eval(Object[] args, EvaluationContext ec) {
                    String textID = (String) args[0];
                    try {
                        // SUUUUPER HACKY
                        String form = ec.getOutputTextForm();
                        if (form != null) {
                            textID = textID + ";" + form;
                            String result = f.getLocalizer().getRawText(f.getLocalizer().getLocale(),
                                    textID);
                            return result == null ? "" : result;
                        } else {
                            String text = f.getLocalizer().getText(textID);
                            return text == null ? "[itext:" + textID + "]" : text;
                        }
                    } catch (NoSuchElementException nsee) {
                        return "[nolocale]";
                    }
                }

                @Override
                public List<Class[]> getPrototypes() {
                    Class[] proto = {String.class};
                    List<Class[]> v = new ArrayList<>(1);
                    v.add(proto);
                    return v;
                }

                @Override
                public boolean rawArgs() {
                    return false;
                }

                @Override
                public boolean realTime() {
                    return false;
                }
            });
        }

        /*
         * function to reverse a select value into the display label for that
         * choice in the question it came from
         *
         * arg 1: select value arg 2: string xpath referring to origin question;
         * must be absolute path
         *
         * this won't work at all if the original label needed to be
         * processed/calculated in some way (<output>s, etc.) (is this even
         * allowed?) likely won't work with multi-media labels _might_ work for
         * itemsets, but probably not very well or at all; could potentially work
         * better if we had some context info DOES work with localization
         *
         * it's mainly intended for the simple case of reversing a question with
         * compile-time-static fields, for use inside an <output>
         */
        if (!ec.getFunctionHandlers().containsKey("jr:choice-name")) {
            final FormDef f = this;
            ec.addFunctionHandler(new IFunctionHandler() {
                @Override
                public String getName() {
                    return "jr:choice-name";
                }

                @Override
                public Object eval(Object[] args, EvaluationContext ec) {
                    try {
                        String value = (String) args[0];
                        String questionXpath = (String) args[1];
                        TreeReference ref = RestoreUtils.xfFact.ref(questionXpath);

                        QuestionDef q = findQuestionByRef(ref, f);
                        if (q == null
                            || (q.getControlType() != Constants.CONTROL_SELECT_ONE
                            && q.getControlType() != Constants.CONTROL_SELECT_MULTI
                            && q.getControlType() != Constants.CONTROL_RANK)) {
                            return "";
                        }

                        // NOTE: this is highly suspect. We have no context against
                        // which to evaluate
                        // a dynamic selection list. This will generally cause that
                        // evaluation to break
                        // if any filtering is done, or, worst case, give unexpected
                        // results.
                        //
                        // We should hook into the existing code (FormEntryPrompt) for
                        // pulling
                        // display text for select choices. however, it's hard,
                        // because we don't really have
                        // any context to work with, and all the situations where that
                        // context would be used
                        // don't make sense for trying to reverse a select value back
                        // to a label in an unrelated
                        // expression

                        List<SelectChoice> choices;
                        ItemsetBinding itemset = q.getDynamicChoices();
                        if (itemset != null) {
                            if (itemset.getChoices() == null) {
                                // NOTE: this will return incorrect results if the list
                                // is filtered.
                                // fortunately, they are ignored by FormEntryPrompt

                                if (ref.isAmbiguous()) {
                                    // SurveyCTO: We need a absolute "ref" to populate the dynamic choices,
                                    // like we do when we populate those at FormEntryPrompt (line 251).
                                    // The "ref" here is ambiguous, so we need to make it concrete first.
                                    ref = ref.contextualize(ec.getContextRef());
                                }

                                f.populateDynamicChoices(itemset, ref);
                            }
                            choices = itemset.getChoices();
                        } else { // static choices
                            choices = q.getChoices();
                        }
                        if (choices != null) {
                            for (SelectChoice ch : choices) {
                                if (ch.getValue().equals(value)) {
                                    // this is really not ideal. we should hook into the
                                    // existing code (FormEntryPrompt) for pulling
                                    // display text for select choices. however, it's
                                    // hard, because we don't really have
                                    // any context to work with, and all the situations
                                    // where that context would be used
                                    // don't make sense for trying to reverse a select
                                    // value back to a label in an unrelated
                                    // expression

                                    String textID = ch.getTextID();
                                    String templateStr;
                                    if (textID != null) {
                                        templateStr = f.getLocalizer().getText(textID);
                                    } else {
                                        templateStr = ch.getLabelInnerText();
                                    }
                                    return fillTemplateString(templateStr, ref);
                                }
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        throw new WrappedException("error in evaluation of xpath function [choice-name]",
                                e);
                    }
                }

                @Override
                public List<Class[]> getPrototypes() {
                    Class[] proto = {String.class, String.class};
                    List<Class[]> v = new ArrayList<>(1);
                    v.add(proto);
                    return v;
                }

                @Override
                public boolean rawArgs() {
                    return false;
                }

                @Override
                public boolean realTime() {
                    return false;
                }
            });
        }
    }

    public String fillTemplateString(String template, TreeReference contextRef) {
        return fillTemplateString(template, contextRef, new HashMap<>());
    }

    public String fillTemplateString(String template, TreeReference contextRef,
                                     HashMap<String, ?> variables) {
        HashMap<String, String> args = new HashMap<>();

        int depth = 0;
        List<String> outstandingArgs = Localizer.getArgs(template);
        while (outstandingArgs.size() > 0) {
            for (String argName : outstandingArgs) {
                if (!args.containsKey(argName)) {
                    int ix = -1;
                    try {
                        ix = Integer.parseInt(argName);
                    } catch (NumberFormatException nfe) {
                        logger.warn("expect arguments to be numeric [{}]", argName);
                    }

                    if (ix < 0 || ix >= outputFragments.size())
                        continue;

                    IConditionExpr expr = outputFragments.get(ix);
                    EvaluationContext ec = new EvaluationContext(exprEvalContext, contextRef);
                    ec.setOriginalContext(contextRef);
                    ec.setVariables(variables);
                    String value = expr.evalReadable(this.getMainInstance(), ec);
                    args.put(argName, value);
                }
            }

            template = Localizer.processArguments(template, args);
            outstandingArgs = Localizer.getArgs(template);

            depth++;
            if (depth >= TEMPLATING_RECURSION_LIMIT) {
                throw new RuntimeException("Dependency cycle in <output>s; recursion limit exceeded!!");
            }
        }

        return template;
    }

    /**
     * Identifies the <code>itemset</code> in the backend model, and creates a set of
     * <code>SelectChoice</code> objects at the current question reference based on the data
     * in the model.
     * <p/>
     * Modifies the <code>itemset</code> object passed by clearing any choices currently on it, and populating with
     * choices based on the question reference <code>curQRef</code> that was passed.
     *
     * @param itemset the binding for an itemset, where the choices will be populated
     * @param curQRef a reference to the current question's element, which will be
     *                used to determine the values to be chosen from
     */
    public void populateDynamicChoices(ItemsetBinding itemset, TreeReference curQRef) {
        getEventNotifier().publishEvent(new Event("Dynamic choices", new EvaluationResult(curQRef, null)));

        List<SelectChoice> choices = new ArrayList<>();

        List<TreeReference> matches = itemset.nodesetExpr.evalNodeset(this.getMainInstance(),
                new EvaluationContext(exprEvalContext, itemset.contextRef.contextualize(curQRef)));

        DataInstance fi = null;
        if (itemset.nodesetRef.getInstanceName() != null) // We're not dealing
        // with the default
        // instance
        {
            fi = getNonMainInstance(itemset.nodesetRef.getInstanceName());
            if (fi == null) {
                throw new XPathException("Instance " + itemset.nodesetRef.getInstanceName()
                        + " not found");
            }
        } else {
            fi = getMainInstance();
        }

        if (matches == null) {
            throw new XPathException("Could not find references depended on by"
                    + itemset.nodesetRef.getInstanceName());
        }

        for (int i = 0; i < matches.size(); i++) {
            TreeReference item = matches.get(i);

            // String label =
            // itemset.labelExpr.evalReadable(this.getMainInstance(), new
            // EvaluationContext(exprEvalContext, item));
            String label = itemset.labelExpr.evalReadable(fi, new EvaluationContext(exprEvalContext,
                    item));
            String value = null;
            TreeElement copyNode = null;
            if (itemset.copyMode) {
                copyNode = this.getMainInstance().resolveReference(itemset.copyRef.contextualize(item));
            }
            if (itemset.valueRef != null) {
                // value = itemset.valueExpr.evalReadable(this.getMainInstance(),
                // new EvaluationContext(exprEvalContext, item));
                value = itemset.valueExpr
                        .evalReadable(fi, new EvaluationContext(exprEvalContext, item));
            }
            // SelectChoice choice = new
            // SelectChoice(labelID,labelInnerText,value,isLocalizable);
            SelectChoice choice = new SelectChoice(label, value != null ? value : "dynamic:" + i,
                    itemset.labelIsItext);
            choice.setIndex(i);
            if (itemset.copyMode)
                choice.copyNode = copyNode;

            choices.add(choice);
        }

        if (choices.size() == 0) {
            // throw new
            // RuntimeException("dynamic select question has no choices! [" +
            // itemset.nodesetRef + "]");
            // When you exit a survey mid way through and want to save it, it seems
            // that Collect wants to
            // go through all the questions. Well of course not all the questions
            // are going to have answers
            // to chose from if the user hasn't filled them out. So I'm just going
            // to make a note of this
            // and not throw an exception.
            logger.info("Dynamic select question has no choices! [{}]. If this occurs while " +
                "filling out a form (and not while saving an incomplete form), the filter " +
                "condition may have eliminated all the choices. Is that what you intended?"
                , itemset.nodesetRef);

        }

        itemset.clearChoices();
        itemset.setChoices(choices, getMainInstance(), exprEvalContext, this.getLocalizer());
    }

    public QuestionPreloader getPreloader() {
        return preloader;
    }

    public void setPreloader(QuestionPreloader preloads) {
        this.preloader = preloads;
    }

    @Override
    public void localeChanged(String locale, Localizer localizer) {
        for (IFormElement child : children) {
            child.localeChanged(locale, localizer);
        }
    }

    public String toString() {
        return getTitle();
    }

    /**
     * Preloads the Data Model with the preload values that are enumerated in the
     * data bindings.
     */
    public void preloadInstance(TreeElement node) {
        // if (node.isLeaf()) {
        IAnswerData preload = null;
        if (node.getPreloadHandler() != null) {
            preload = preloader.getQuestionPreload(node.getPreloadHandler(), node.getPreloadParams());
        }
        if (preload != null) { // what if we want to wipe out a value in the
            // instance?
            node.setAnswer(preload);
        }
        // } else {
        if (!node.isLeaf()) {
            for (int i = 0; i < node.getNumChildren(); i++) {
                TreeElement child = node.getChildAt(i);
                if (child.getMult() != TreeReference.INDEX_TEMPLATE)
                    // don't preload templates; new repeats are preloaded as they're
                    // created
                    preloadInstance(child);
            }
        }
        // }
    }

    public boolean postProcessInstance() {
        actionController.triggerActionsFromEvent(Action.EVENT_XFORMS_REVALIDATE, this);
        return postProcessInstance(mainInstance.getRoot());
    }

    /**
     * Iterates over the form's data bindings, and evaluates all post-processing calls.
     *
     * @return true if the instance was modified in any way, otherwise false
     */
    private boolean postProcessInstance(TreeElement node) {
        // we might have issues with ordering, for example, a handler that writes
        // a value to a node,
        // and a handler that does something external with the node. if both
        // handlers are bound to the
        // same node, we need to make sure the one that alters the node executes
        // first. deal with that later.
        // can we even bind multiple handlers to the same node currently?

        // also have issues with conditions. it is hard to detect what conditions
        // are affected by the actions
        // of the post-processor. normally, it wouldn't matter because we only
        // post-process when we are exiting
        // the form, so the result of any triggered conditions is irrelevant.
        // however, if we save a form in the
        // interim, post-processing occurs, and then we continue to edit the form.
        // it seems like having conditions
        // dependent on data written during post-processing is a bad practice
        // anyway, and maybe we shouldn't support it.

        if (node.isLeaf()) {
            if (node.getPreloadHandler() != null) {
                return preloader.questionPostProcess(node, node.getPreloadHandler(),
                        node.getPreloadParams());
            } else {
                return false;
            }
        } else {
            boolean instanceModified = false;
            for (int i = 0; i < node.getNumChildren(); i++) {
                TreeElement child = node.getChildAt(i);
                if (child.getMult() != TreeReference.INDEX_TEMPLATE)
                    instanceModified |= postProcessInstance(child);
            }
            return instanceModified;
        }
    }

    /**
     * Reads the form definition object from the supplied stream.
     * <p/>
     * Requires that the instance has been set to a prototype of the instance
     * that should be used for deserialization.
     *
     * @param dis - the stream to read from
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    public void readExternal(DataInputStream dis, PrototypeFactory pf) throws IOException,
            DeserializationException {
        setID(ExtUtil.readInt(dis));
        setName(ExtUtil.nullIfEmpty(ExtUtil.readString(dis)));
        setTitle((String) ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setChildren((List<IFormElement>) ExtUtil.read(dis, new ExtWrapListPoly(), pf));
        setInstance((FormInstance) ExtUtil.read(dis, FormInstance.class, pf));

        setLocalizer((Localizer) ExtUtil.read(dis, new ExtWrapNullable(Localizer.class), pf));

        List<Condition> vcond = (List<Condition>) ExtUtil.read(dis, new ExtWrapList(Condition.class), pf);
        for (Condition condition : vcond) {
            addTriggerable(condition);
        }
        List<Recalculate> vcalc = (List<Recalculate>) ExtUtil.read(dis, new ExtWrapList(Recalculate.class), pf);
        for (Recalculate recalculate : vcalc) {
            addTriggerable(recalculate);
        }
        finalizeTriggerables();

        outputFragments = (List<IConditionExpr>) ExtUtil.read(dis, new ExtWrapListPoly(), pf);

        submissionProfiles = (HashMap<String, SubmissionProfile>) ExtUtil.read(dis, new ExtWrapMap(
                String.class, SubmissionProfile.class));

        formInstances = (HashMap<String, DataInstance>) ExtUtil.read(dis, new ExtWrapMap(
                String.class, new ExtWrapTagged()), pf);

        extensions = (List<XFormExtension>) ExtUtil.read(dis, new ExtWrapListPoly(), pf);

        resetEvaluationContext();
        actionController = (ActionController) ExtUtil.read(dis, new ExtWrapNullable(ActionController.class), pf);
    }

    /**
     * meant to be called after deserialization and initialization of handlers
     *
     * @param newInstance true if the form is to be used for a new entry interaction,
     *                    false if it is using an existing IDataModel
     */
    public void initialize(boolean newInstance, InstanceInitializationFactory factory) {
        for (String instanceId : formInstances.keySet()) {
            DataInstance instance = formInstances.get(instanceId);
            instance.initialize(factory, instanceId);
        }
        if (newInstance) {// only preload new forms (we may have to revisit
            // this)
            preloadInstance(mainInstance.getRoot());
        }

        if (getLocalizer() != null && getLocalizer().getLocale() == null) {
            getLocalizer().setToDefault();
        }

        if (newInstance) {
            actionController.triggerActionsFromEvent(Action.EVENT_ODK_INSTANCE_FIRST_LOAD, this);

            // xforms-ready is marked as deprecated as of JavaRosa 2.14.0 but is still dispatched for compatibility with
            // old form definitions
            actionController.triggerActionsFromEvent(Action.EVENT_XFORMS_READY, this);
        }

        Collection<QuickTriggerable> qts = initializeTriggerables(TreeReference.rootRef(), false);
        dagImpl.publishSummary("Form initialized", qts);
    }

    /**
     * Writes the form definition object to the supplied stream.
     *
     * @param dos - the stream to write to
     * @throws IOException
     */
    @Override
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, getID());
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(getName()));
        ExtUtil.write(dos, new ExtWrapNullable(getTitle()));
        ExtUtil.write(dos, new ExtWrapListPoly(getChildren()));
        ExtUtil.write(dos, getMainInstance());
        ExtUtil.write(dos, new ExtWrapNullable(localizer));

        List<Condition> conditions = dagImpl.getConditions();
        List<Recalculate> recalcs = dagImpl.getRecalculates();

        ExtUtil.write(dos, new ExtWrapList(conditions));
        ExtUtil.write(dos, new ExtWrapList(recalcs));

        ExtUtil.write(dos, new ExtWrapListPoly(outputFragments));
        ExtUtil.write(dos, new ExtWrapMap(submissionProfiles));

        // for support of multi-instance forms

        ExtUtil.write(dos, new ExtWrapMap(formInstances, new ExtWrapTagged()));
        ExtUtil.write(dos, new ExtWrapListPoly(extensions));
        ExtUtil.write(dos, new ExtWrapNullable(actionController));
    }

    public void collapseIndex(FormIndex index, List<Integer> indexes, List<Integer> multiplicities, List<IFormElement> elements) {
        if (!index.isInForm()) {
            return;
        }

        IFormElement element = this;
        while (index != null) {
            int i = index.getLocalIndex();
            element = element.getChild(i);

            indexes.add(i);
            multiplicities.add(index.getInstanceIndex() == -1 ? 0 : index
                    .getInstanceIndex());
            elements.add(element);

            index = index.getNextLevel();
        }
    }

    public FormIndex buildIndex(List<Integer> indexes, List<Integer> multiplicities, List<IFormElement> elements) {
        FormIndex cur = null;
        List<Integer> curMultiplicities = new ArrayList<>();
        curMultiplicities.addAll(multiplicities);

        List<IFormElement> curElements = new ArrayList<>();
        curElements.addAll(elements);

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int ix = indexes.get(i);
            int mult = multiplicities.get(i);

            if (!(elements.get(i) instanceof GroupDef && ((GroupDef) elements.get(i))
                    .getRepeat())) {
                mult = -1;
            }

            cur = new FormIndex(cur, ix, mult, getChildInstanceRef(curElements, curMultiplicities));
            curMultiplicities.remove(curMultiplicities.size() - 1);
            curElements.remove(curElements.size() - 1);
        }
        return cur;
    }

    public int getNumRepetitions(FormIndex index) {
        List<Integer> indexes = new ArrayList<>();
        List<Integer> multiplicities = new ArrayList<>();
        List<IFormElement> elements = new ArrayList<>();

        if (!index.isInForm()) {
            throw new RuntimeException("not an in-form index");
        }

        collapseIndex(index, indexes, multiplicities, elements);

        if (!(elements.get(elements.size() - 1) instanceof GroupDef)
                || !((GroupDef) elements.get(elements.size() - 1)).getRepeat()) {
            throw new RuntimeException("current element not a repeat");
        }

        // so painful
        TreeElement templNode = mainInstance.getTemplate(index.getReference());
        TreeReference parentPath = templNode.getParent().getRef().genericize();
        TreeElement parentNode = mainInstance.resolveReference(parentPath.contextualize(index
                .getReference()));
        return parentNode.getChildMultiplicity(templNode.getName());
    }

    // repIndex == -1 => next repetition about to be created
    public FormIndex descendIntoRepeat(FormIndex index, int repIndex) {
        int numRepetitions = getNumRepetitions(index);

        List<Integer> indexes = new ArrayList<>();
        List<Integer> multiplicities = new ArrayList<>();
        List<IFormElement> elements = new ArrayList<>();
        collapseIndex(index, indexes, multiplicities, elements);

        if (repIndex == -1) {
            repIndex = numRepetitions;
        } else {
            if (repIndex < 0 || repIndex >= numRepetitions) {
                throw new RuntimeException("selection exceeds current number of repetitions");
            }
        }

        multiplicities.set(multiplicities.size() - 1, repIndex);

        return buildIndex(indexes, multiplicities, elements);
    }

    @Override
    public int getDeepChildCount() {
        int total = 0;
        for (IFormElement child : children) {
            total += child.getDeepChildCount();
        }
        return total;
    }

    @Override
    public void registerStateObserver(FormElementStateListener qsl) {
        // NO. (Or at least not yet).
    }

    @Override
    public void unregisterStateObserver(FormElementStateListener qsl) {
        // NO. (Or at least not yet).
    }

    @Override
    public List<IFormElement> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<IFormElement> children) {
        this.children = (children == null ? new ArrayList<>() : children);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Localizer getLocalizer() {
        return localizer;
    }

    public List<IConditionExpr> getOutputFragments() {
        return outputFragments;
    }

    public void setOutputFragments(List<IConditionExpr> outputFragments) {
        this.outputFragments = outputFragments;
    }

    @Override
    public HashMap<String, Object> getMetaData() {
        HashMap<String, Object> metadata = new HashMap<>();
        String[] fields = getMetaDataFields();

        for (String field : fields) {
            try {
                metadata.put(field, getMetaData(field));
            } catch (NullPointerException npe) {
                if (getMetaData(field) == null) {
                    logger.error("ERROR! XFORM MUST HAVE A NAME!", npe);
                }
            }
        }

        return metadata;
    }

    @Override
    public String getMetaData(String fieldName) {
        if (fieldName.equals("DESCRIPTOR")) {
            return name;
        }
        if (fieldName.equals("XMLNS")) {
            return ExtUtil.emptyIfNull(mainInstance.schema);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{"DESCRIPTOR", "XMLNS"};
    }

    /**
     * Recursively traverses the main instance and initializes any questions with
     * dynamic ItemsetBindings.  We need to do this late in the process so that
     * we have the entire main instance assembled.
     */
    public static void updateItemsetReferences(List<IFormElement> children) {
        if (children != null) {
            for (IFormElement child : children) {
                if (child instanceof QuestionDef) {
                    QuestionDef q = (QuestionDef) child;
                    ItemsetBinding itemset = q.getDynamicChoices();
                    if (itemset != null) {
                        itemset.initReferences(q);
                    }
                } else {
                    updateItemsetReferences(child.getChildren());
                }
            }
        }
    }

    /**
     * Links a deserialized instance back up with its parent FormDef. This allows
     * select/select1 questions to be internationalizable in chatterbox, and (if
     * using CHOICE_INDEX mode) allows the instance to be serialized to XML.
     */
    public void attachControlsToInstanceData() {
        attachControlsToInstanceData(getMainInstance().getRoot());
    }

    private void attachControlsToInstanceData(TreeElement node) {
        for (int i = 0; i < node.getNumChildren(); i++) {
            attachControlsToInstanceData(node.getChildAt(i));
        }

        IAnswerData val = node.getValue();
        List<Selection> selections = null;
        if (val instanceof SelectOneData) {
            selections = new ArrayList<>();
            selections.add((Selection) val.getValue());
        } else if (val instanceof MultipleItemsData) {
            selections = (List<Selection>) val.getValue();
        }

        if (selections != null) {
            QuestionDef q = findQuestionByRef(node.getRef(), this);
            if (q == null) {
                throw new RuntimeException(
                        "FormDef.attachControlsToInstanceData: can't find question to link");
            }

            if (q.getDynamicChoices() != null) {
                // droos: i think we should do something like initializing the
                // itemset here, so that default answers
                // can be linked to the selectchoices. however, there are
                // complications. for example, the itemset might
                // not be ready to be evaluated at form initialization; it may
                // require certain questions to be answered
                // first. e.g., if we evaluate an itemset and it has no choices, the
                // xform engine will throw an error
                // itemset TODO
            }

            for (Selection s : selections) {
                s.attachChoice(q);
            }
        }
    }

    /**
    * Traverses recursively the given {@link IFormElement} node tree and returns
    * the first {@link QuestionDef} that matches the binding defined in the
    * given {@link TreeReference}.
    *
    * @param ref reference to a form binding
    * @param fe  the recursive {@link IFormElement}. The starting call would
    *            typically receive a {@link FormDef}
    * @return the question definition, {@code null} if no {@link QuestionDef}
    *         matches the given binding
    */
    public static QuestionDef findQuestionByRef(TreeReference ref, IFormElement fe) {
        if (fe instanceof FormDef) {
            ref = ref.genericize();
        }

        if (fe instanceof QuestionDef) {
            QuestionDef q = (QuestionDef) fe;
            TreeReference bind = FormInstance.unpackReference(q.getBind());
            return (ref.equals(bind) ? q : null);
        } else {
            for (int i = 0; i < fe.getChildren().size(); i++) {
                QuestionDef ret = findQuestionByRef(ref, fe.getChild(i));
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }

    /**
     * Appearance isn't a valid attribute for form, but this method must be
     * included as a result of conforming to the IFormElement interface.
     */
    @Override
    public String getAppearanceAttr() {
        throw new RuntimeException(
                "This method call is not relevant for FormDefs getAppearanceAttr ()");
    }

    @Override
    public ActionController getActionController() {
        return actionController;
    }

    /**
     * Appearance isn't a valid attribute for form, but this method must be
     * included as a result of conforming to the IFormElement interface.
     */
    @Override
    public void setAppearanceAttr(String appearanceAttr) {
        throw new RuntimeException(
                "This method call is not relevant for FormDefs setAppearanceAttr()");
    }

    /** Not applicable here. */
    @Override
    public String getLabelInnerText() {
        return null;
    }

    /** Not applicable */
    @Override
    public String getTextID() {
        return null;
    }

    /** Not applicable */
    @Override
    public void setTextID(String textID) {
        throw new RuntimeException("This method call is not relevant for FormDefs [setTextID()]");
    }

    public void setDefaultSubmission(SubmissionProfile profile) {
        submissionProfiles.put(DEFAULT_SUBMISSION_PROFILE, profile);
    }

    public void addSubmissionProfile(String submissionId, SubmissionProfile profile) {
        submissionProfiles.put(submissionId, profile);
    }

    public SubmissionProfile getSubmissionProfile() {
        // At some point these profiles will be set by the <submit> control in the
        // form.
        // In the mean time, though, we can only promise that the default one will
        // be used.

        return submissionProfiles.get(DEFAULT_SUBMISSION_PROFILE);
    }

    @Override
    public void setAdditionalAttribute(String namespace, String name, String value) {
        // Do nothing. Not supported.
    }

    @Override
    public String getAdditionalAttribute(String namespace, String name) {
        // Not supported.
        return null;
    }

    @Override
    public List<TreeElement> getAdditionalAttributes() {
        // Not supported.
        return Collections.emptyList();
    }

    public <X extends XFormExtension> X getExtension(Class<X> extension) {
        for (XFormExtension ex : extensions) {
            if (ex.getClass().isAssignableFrom(extension)) {
                return (X) ex;
            }
        }
        X newEx;
        try {
            newEx = extension.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Illegally Structured XForm Extension " + extension.getName());
        }
        extensions.add(newEx);
        return newEx;
    }

    /**
     * Frees all of the components of this form which are no longer needed once
     * it is completed.
     * <p/>
     * Once this is called, the form is no longer capable of functioning, but all
     * data should be retained.
     */
    public void seal() {
        dagImpl = null;
        // We may need ths one, actually
        exprEvalContext = null;
    }

    /**
     * Pull this in from FormOverview so that we can make fields private.
     *
     * @param instanceNode
     * @param action
     * @return
     */
    public IConditionExpr getConditionExpressionForTrueAction(TreeElement instanceNode, int action) {
        return dagImpl.getConditionExpressionForTrueAction(getMainInstance(), instanceNode, action);
    }

    /**
     * For debugging
     */
    public final void printTriggerables(String path) {
        dagImpl.printTriggerables(path);
    }

    public List<String> getParseWarnings() {
        return parseWarnings;
    }

    public List<String> getParseErrors() {
        return parseErrors;
    }

    public void addParseWarning(String warning) {
        parseWarnings.add(warning);
    }

    public void addParseError(String error) {
        parseErrors.add(error);
    }
}
