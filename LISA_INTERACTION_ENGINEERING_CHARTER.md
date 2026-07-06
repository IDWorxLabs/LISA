# THE LISA INTERACTION ENGINEERING CHARTER (LIEC)

**Draft Version 1**

---

## PREAMBLE

We establish this Charter as the subordinate engineering authority for the LISA Interaction Constitution.

The LISA Interaction Constitution defines constitutional interaction law—the enduring principles governing how human beings navigate, gesture, communicate, recover, and remain safe within LISA. This Charter translates that law into deterministic engineering requirements. It explains what engineering systems must guarantee; it does not redefine what interaction law is.

No engineering authority may contradict the Constitution. Where conflict exists, authority resolves in strict order:

**LIC → LIEC → Validation Authorities → Implementation**

Engineering charters, validation authorities, and implementations derive their legitimacy from constitutional law. They shall implement, verify, and maintain compliance; they shall not invent, amend, or bypass interaction law for convenience.

The requirements herein bind all present and future engineering work on LISA interaction, regardless of platform, language, device, or release cycle.

---

# PART I — ENGINEERING AUTHORITY

## TITLE I — SUBORDINATION TO THE CONSTITUTION

### CHAPTER I — Supremacy of the LIC

#### SECTION I — Source of Law

**Article 1.1.1.1.** The LISA Interaction Constitution is supreme. Every engineering requirement, validation rule, and implementation decision governing human interaction with LISA shall conform to the LIC.

**Article 1.1.1.2.** Where this Charter is silent, the LIC governs. Where this Charter appears to conflict with the LIC, the LIC prevails and this Charter is void to the extent of conflict.

**Article 1.1.1.3.** Engineering shall treat constitutional Articles, Principles, and the Founding Purpose as binding inputs to design, not as advisory commentary.

#### SECTION II — Non-Delegation of Constitutional Authority

**Article 1.1.1.4.** Engineering may not amend, suspend, or reinterpret constitutional interaction law through configuration, release policy, or undocumented convention.

**Article 1.1.1.5.** Temporary exceptions, feature flags, developer overrides, and experimental branches that violate constitutional interaction law in user-facing paths are prohibited.

**Article 1.1.1.6.** Subordinate documents—including validation specifications, design guides, and integration agreements—shall declare their subordination to the LIC and to this Charter.

---

## TITLE II — TRANSLATION OBLIGATION

### CHAPTER I — From Law to Requirement

#### SECTION I — Duty to Translate

**Article 1.2.1.1.** Engineering translates constitutional principles into deterministic requirements capable of verification. Translation shall preserve the meaning and priority of constitutional law.

**Article 1.2.1.2.** Every major engineering subsystem affecting interaction shall trace to one or more constitutional Articles or Principles. Untraceable behavior is presumptively non-compliant.

**Article 1.2.1.3.** Translation shall favor measurable guarantees—reachability, uniqueness, consistency, recoverability—over aspirational descriptions.

#### SECTION II — Constitutional Principles as Engineering Inputs

**Article 1.2.1.4.** User Sovereignty, Least Effort, Learnability, Accessibility First, Safety Above Convenience, and Consistency shall be treated as mandatory design constraints, not optional quality goals.

**Article 1.2.1.5.** Engineering shall resolve ambiguity in favor of the constitutional principle most protective of user authority, safety, and independence.

**Article 1.2.1.6.** The Founding Purpose—restoration of communication for persons who cannot speak conventionally—shall govern tradeoffs among otherwise equal engineering options.

---

## TITLE III — PROHIBITION ON INTERACTION LAW

### CHAPTER I — Engineering Does Not Legislate

#### SECTION I — Boundary of Authority

**Article 1.3.1.1.** Engineering cannot create interaction law. Assigning gesture meaning, altering global navigation reachability, redefining emergency authority, or establishing new user obligations requires constitutional amendment, not engineering discretion.

**Article 1.3.1.2.** Engineering may define internal mechanisms, data structures, and verification procedures only insofar as they implement constitutional law without substituting for it.

**Article 1.3.1.3.** Product preference, schedule pressure, and integration convenience shall not justify engineering rules that contradict the LIC.

#### SECTION II — Stable External Behavior

**Article 1.3.1.4.** The user-facing interaction language—navigation commands, global gestures, emergency access, and recovery paths—shall remain stable across internal refactors unless the LIC is formally amended.

**Article 1.3.1.5.** Internal engineering reorganizations shall not alter externally observable interaction behavior without constitutional authority or explicit user-facing notice where amendment permits.

**Article 1.3.1.6.** Parallel interaction schemes maintained outside the constitutional model are prohibited in production paths.

---

## TITLE IV — SUBORDINATE AUTHORITIES

### CHAPTER I — Implementations and Validators

#### SECTION I — Implementations

**Article 1.4.1.1.** Implementations are subordinate. No running system, device build, or integrated surface is authoritative over the LIC or this Charter.

**Article 1.4.1.2.** Implementations shall expose behavior that can be inspected, tested, and validated against constitutional and charter requirements.

**Article 1.4.1.3.** An implementation that passes internal review but violates interaction law remains invalid and shall not ship as user-facing LISA.

#### SECTION II — Validators

**Article 1.4.1.4.** Validators are subordinate. They detect violation; they do not define interaction policy.

**Article 1.4.1.5.** Validators derive their rules from this Charter and the LIC. A validator rule that contradicts either is void.

**Article 1.4.1.6.** Absence of automated validation does not excuse non-compliance. The obligation to conform exists independent of proof machinery.

---

# PART II — NAVIGATION ENGINEERING

## TITLE I — NAVIGATION CONTEXT

### CHAPTER I — Single Active Context

#### SECTION I — Context Integrity

**Article 2.1.1.1.** Engineering shall maintain exactly one active navigation context at any moment, comprising the user's present mode, category, page, and overlay state as applicable.

**Article 2.1.1.2.** The active navigation context shall be determinable at all times by engineering systems and visibly reflected to the user.

**Article 2.1.1.3.** Concurrent or ambiguous navigation contexts that could cause divergent gesture or command interpretation are prohibited.

#### SECTION II — Context Transitions

**Article 2.1.1.4.** Mode transitions shall be deterministic. Given identical starting context and identical user navigation acts, engineering systems shall arrive at identical resulting contexts.

**Article 2.1.1.5.** Context transitions shall be logged or inspectable for validation and recovery analysis without exposing private user communication content beyond what verification requires.

**Article 2.1.1.6.** Silent context mutation—where internal state changes without a user-attributable navigation act—is prohibited except where constitutional emergency authority explicitly governs.

---

## TITLE II — GLOBAL NAVIGATION ENGINEERING

### CHAPTER I — Permanent Global Commands

#### SECTION I — Availability

**Article 2.2.1.1.** Engineering shall guarantee permanent global navigation: Scroll Up / Previous, Scroll Down / Next, Select / Save, Back / Cancel, Categories, and Emergency shall remain registered, reachable, and functional in all production interaction states.

**Article 2.2.1.2.** Global navigation shall not be mode-gated, category-gated, or page-gated in ways that conceal or disable constitutional reachability.

**Article 2.2.1.3.** Engineering systems shall treat Categories, Back, Cancel, and Emergency as invariant reachability requirements, not best-effort features.

#### SECTION II — Labelling and Presentation

**Article 2.2.1.4.** Every global navigation command presented to the user shall carry an explicit, stable label aligned with constitutional terminology.

**Article 2.2.1.5.** Engineering shall not present unlabelled global navigation affordances in production paths.

**Article 2.2.1.6.** Global navigation labels and command identifiers shall remain consistent across modes unless the LIC is amended.

---

## TITLE III — RECOVERY AND DEAD-END PREVENTION

### CHAPTER I — Guaranteed Recovery Routes

#### SECTION I — Recovery Engineering

**Article 2.3.1.1.** Engineering shall guarantee recovery routes from every navigation state. At minimum, a labelled path to Back, Cancel, Categories, or equivalent constitutional recovery shall exist at all times.

**Article 2.3.1.2.** No navigation state machine shall contain dead ends. States without outbound recovery transitions are prohibited.

**Article 2.3.1.3.** Recovery transitions shall require no more user effort than the navigation act that produced the error or undesired state, and shall prefer less effort where safety permits.

#### SECTION II — State Machine Integrity

**Article 2.3.1.4.** Navigation state machines shall be complete, explicit, and subject to integrity verification. Undefined transitions and implicit states are prohibited.

**Article 2.3.1.5.** Overlays, dialogs, temporary prompts, and integration surfaces shall preserve recovery and global reachability unless constitutionally superseded by active emergency handling.

**Article 2.3.1.6.** Engineering shall verify that every reachable state possesses at least one recovery path to a less restrictive, labelled navigation state.

---

## TITLE IV — MODE AND CATEGORY ENGINEERING

### CHAPTER I — Hierarchy Enforcement

#### SECTION I — Categories

**Article 2.4.1.1.** Engineering shall ensure categories are always reachable through global navigation from every mode and production interaction state.

**Article 2.4.1.2.** Category hierarchy shall be represented in engineering systems as a navigable structure consistent with constitutional organization, not as an opaque internal catalog.

**Article 2.4.1.3.** Category shortcuts may exist but shall not replace guaranteed hierarchical reachability for essential communication domains.

#### SECTION II — Modes

**Article 2.4.1.4.** Mode entry and exit shall be deterministic and reversible. Modes shall declare identity to the user through engineering-visible state.

**Article 2.4.1.5.** Modes may alter local vocabulary and pages but shall not alter global navigation registration, gesture assignment, or emergency supremacy.

**Article 2.4.1.6.** Mode stacking shall remain shallow enough that the user's location and available recovery paths are never obscured.

---

# PART III — GESTURE ENGINEERING

## TITLE I — GESTURE REGISTRIES

### CHAPTER I — Reserved Gesture Registry

#### SECTION I — Constitutional Reservation

**Article 3.1.1.1.** Engineering shall maintain a reserved gesture registry listing all gestures assigned to global navigation, emergency, and other constitutional functions that may not carry vocabulary meaning.

**Article 3.1.1.2.** Reserved gestures shall not appear as selectable vocabulary on any page or in any local gesture registry.

**Article 3.1.1.3.** The reserved gesture registry shall be authoritative for collision prevention and validation.

#### SECTION II — Change Control

**Article 3.1.1.4.** Changes to the reserved gesture registry require constitutional amendment or explicit charter revision traceable to amendment. Engineering shall not repurpose reserved gestures unilaterally.

**Article 3.1.1.5.** Experimental gesture assignments shall remain outside production registries until constitutionally adopted.

**Article 3.1.1.6.** Third-party integrations shall register against the reserved gesture registry before activation.

---

## TITLE II — GLOBAL GESTURE REGISTRY

### CHAPTER I — Constitutional Global Assignments

#### SECTION I — Registration

**Article 3.2.1.1.** Engineering shall maintain a global gesture registry implementing the constitutional assignments:

| Gesture Identifier | Command |
|--------------------|---------|
| L2 R0 | Scroll Up / Previous |
| L0 R2 | Scroll Down / Next |
| L1 R1 | Select / Save |
| L2 R2 | Back / Cancel |
| L4 R4 | Categories |
| L6 R0 | Emergency |

**Article 3.2.1.2.** Global gestures shall be registered independently of mode, category, and page. Their meaning shall not vary by context.

**Article 3.2.1.3.** Global gesture registration shall take precedence over all local registrations when both could apply.

#### SECTION II — Global Gesture Presentation

**Article 3.2.1.4.** Engineering shall ensure global gestures are explained in visible, labelled form wherever gesture input is accepted in production paths.

**Article 3.2.1.5.** Global gesture identifiers shall remain stable across hardware translations and software revisions unless the LIC is amended.

**Article 3.2.1.6.** Engineering shall treat ambiguity at the global gesture layer as a hard failure condition.

---

## TITLE III — LOCAL GESTURE REGISTRY

### CHAPTER I — Contextual Vocabulary Gestures

#### SECTION I — Local Registration

**Article 3.3.1.1.** Engineering shall maintain local gesture registries scoped to category, page, and mode context. Local gestures apply only within their registered scope.

**Article 3.3.1.2.** The same local gesture identifier may register different meanings in different scopes, provided that no two meanings are active simultaneously within the same mode on the same page.

**Article 3.3.1.3.** Local registries shall publish present meaning to validation systems and to user-visible labelling mechanisms for the active page.

#### SECTION II — Current-Context-Only Processing

**Article 3.3.1.4.** Engineering shall process vocabulary gestures for the current page and active navigation context only, unless the user has explicitly navigated to a broader context through deliberate navigation acts.

**Article 3.3.1.5.** Inactive pages, dormant categories, and background contexts shall not receive or interpret local gesture input in production paths.

**Article 3.3.1.6.** Context resolution for gesture processing shall use the same active navigation context defined for navigation engineering.

---

## TITLE IV — GESTURE NAMESPACE AND CONFLICT

### CHAPTER I — Namespace Separation

#### SECTION I — Logical Separation

**Article 3.4.1.1.** Engineering shall enforce gesture namespace separation between global, reserved, and local registries. Classification of a gesture shall be unambiguous.

**Article 3.4.1.2.** Local gesture identifiers that resemble global identifiers shall be flagged as high-risk and resolved before production release.

**Article 3.4.1.3.** Namespace collision across registries is prohibited in production paths.

#### SECTION II — One Active Meaning

**Article 3.4.1.4.** Engineering shall enforce one active meaning per gesture within any given navigation context. Dual interpretation in the same context is prohibited.

**Article 3.4.1.5.** Conditional gesture branching that the user cannot discern from visible presentation is prohibited.

**Article 3.4.1.6.** Gesture conflict detection shall run before release and upon registry change.

---

## TITLE V — GESTURE RESOLUTION ORDER

### CHAPTER I — Deterministic Precedence

#### SECTION I — Resolution Sequence

**Article 3.5.1.1.** Engineering shall implement deterministic gesture resolution order: Emergency and reserved global functions first; remaining global navigation commands second; local vocabulary gestures last within the active page context.

**Article 3.5.1.2.** Resolution order shall be documented, testable, and identical across platforms and devices unless physical constraints are explicitly declared and constitutionally compliant.

**Article 3.5.1.3.** When conflict is detected, global and emergency assignments shall be preserved; local assignments shall yield.

#### SECTION II — Accidental Activation Prevention

**Article 3.5.1.4.** Engineering shall ensure a single accidental gesture cannot trigger communication output. Deliberate-commit mechanisms shall protect Select, Save, speak, and send paths.

**Article 3.5.1.5.** Debounce, dwell, confirmation, or equivalent mechanisms shall be tunable within safe bounds without disabling constitutional global gestures.

**Article 3.5.1.6.** Accidental activation prevention shall not introduce dead ends or obscure recovery.

---

## TITLE VI — GESTURE FATIGUE AND INPUT TUNING

### CHAPTER I — Fatigue Engineering

#### SECTION I — Physical Burden

**Article 3.6.1.1.** Gesture fatigue engineering shall minimize repetitive motion required for ordinary navigation and frequent communication, consistent with the Principle of Least Effort.

**Article 3.6.1.2.** Engineering shall measure and limit gesture sequence length for high-frequency actions, preferring short reusable gestures over long chains.

**Article 3.6.1.3.** Elevated user abort rates, error rates, or session truncation attributable to gesture burden shall trigger engineering review and remediation.

#### SECTION II — Response Time and Sensitivity

**Article 3.6.1.4.** Response-time engineering shall provide predictable feedback for dwell, selection, and confirmation without introducing unsafe delay to Emergency or recovery paths.

**Article 3.6.1.5.** Sensitivity engineering shall expose user-adjustable parameters for input thresholds within bounds that preserve accidental-activation protection and global gesture reliability.

**Article 3.6.1.6.** Default response-time and sensitivity values shall be safe, not merely convenient for laboratory conditions.

---

# PART IV — COMMUNICATION ENGINEERING

## TITLE I — CATEGORY ENGINE

### CHAPTER I — Category System Requirements

#### SECTION I — Organization

**Article 4.1.1.1.** Engineering shall implement a category engine that organizes vocabulary into meaningful, reachable domains reducing scroll burden and cognitive load.

**Article 4.1.1.2.** The category engine shall reflect human communication needs in its structure and labeling, not internal storage convenience.

**Article 4.1.1.3.** Category depth, nesting, and shortcuts shall be inspectable for Least Effort and reachability validation.

#### SECTION II — Integrity

**Article 4.1.1.4.** Categories shall not impersonate emergency authority or global navigation functions.

**Article 4.1.1.5.** Category removal or reorganization shall preserve access to essential communication or provide explicit migration paths.

**Article 4.1.1.6.** The category engine shall integrate with navigation context so that active category is always known to communication processing.

---

## TITLE II — VOCABULARY ENGINE

### CHAPTER I — Page-Local Processing

#### SECTION I — Vocabulary Scope

**Article 4.2.1.1.** Engineering shall implement a vocabulary engine that serves page-local vocabulary sets aligned with the active category and mode.

**Article 4.2.1.2.** Vocabulary processing shall be page-local: only vocabulary registered to the active page shall be eligible for selection, composition, and output in the current context.

**Article 4.2.1.3.** Cross-page vocabulary effects require explicit navigation acts recorded in navigation context.

#### SECTION II — Scroll and Selection Integration

**Article 4.2.1.4.** Vocabulary navigation shall operate through constitutional global scroll and selection commands without breaking category or mode integrity.

**Article 4.2.1.5.** Empty, placeholder, or incomplete vocabulary sets shall not be presented as production-complete without explicit non-production marking.

**Article 4.2.1.6.** Vocabulary engine state shall reset or reconcile predictably on page, category, and mode transitions.

---

## TITLE III — PHRASE REGISTRY

### CHAPTER I — Phrase Design Enforcement

#### SECTION I — Completeness and Voice

**Article 4.3.1.1.** Engineering shall maintain a phrase registry—or equivalent authoritative phrase source—capable of enforcing first-person phrase availability and preference rules.

**Article 4.3.1.2.** Phrase completeness engineering shall prefer full meaningful phrases over isolated words where practical, while permitting brief selections where constitutional brevity is required.

**Article 4.3.1.3.** Phrases shall be registrable as grammatically and socially complete utterances suitable for display and speech to listeners.

#### SECTION II — Naturalness and Listener Clarity

**Article 4.3.1.4.** Phrase registry content and presentation rules shall favor natural human utterance over mechanical fragments.

**Article 4.3.1.5.** Engineering shall support output that reduces listener and caregiver ambiguity by tying phrase meaning to visible active context.

**Article 4.3.1.6.** Stock phrases shall support personalization, editing, or dismissal where required for accurate self-expression.

---

## TITLE IV — CONTEXT RESOLUTION

### CHAPTER I — Communication Context Engine

#### SECTION I — Active Context Binding

**Article 4.4.1.1.** Engineering shall implement context resolution binding communication output to active category, page, and mode as reflected in navigation context.

**Article 4.4.1.2.** Phrase meaning shall be resolved from active context before output is finalized. Distant or inactive contexts shall not govern present output.

**Article 4.4.1.3.** Context switches shall be attributable to user navigation acts or clearly signalled system transitions, never silent.

#### SECTION II — Output Discipline

**Article 4.4.1.4.** Communication output shall reflect explicit user selection on the active page unless the user has confirmed a composed message built from multiple deliberate acts.

**Article 4.4.1.5.** Engineering shall log or inspect context binding sufficient to verify current-page-only processing without unnecessary retention of message content.

**Article 4.4.1.6.** Listeners and caregivers shall be able to rely on engineering guarantees that output corresponds to deliberate user choices within active context.

---

## TITLE V — AI ASSISTANCE BOUNDARIES

### CHAPTER I — Non-Usurpation Engineering

#### SECTION I — Assistance Limits

**Article 4.5.1.1.** AI assistance engineering shall rank, suggest, and prepare vocabulary or phrases without speaking, sending, or finalizing communication without user confirmation.

**Article 4.5.1.2.** Predictive or generated utterances shall never be transmitted as though user-selected.

**Article 4.5.1.3.** AI systems shall not alter navigation context, suppress global commands, or hide recovery paths.

#### SECTION II — Visible Influence

**Article 4.5.1.4.** When AI influences ordering, visibility, or default focus of vocabulary, that influence shall be visibly distinguishable from user selections where practical.

**Article 4.5.1.5.** The user shall retain reject, edit, and ignore capability without navigation penalty.

**Article 4.5.1.6.** AI assistance shall remain subordinate to User Sovereignty and the Founding Purpose in all engineering designs.

---

## TITLE VI — MULTILINGUAL ARCHITECTURE

### CHAPTER I — Future Language Support

#### SECTION I — Extensibility Without Fragmentation

**Article 4.6.1.1.** Engineering shall architect communication systems so that future languages may be added without breaking category structure, navigation reachability, or global gesture law.

**Article 4.6.1.2.** Multilingual phrase registries shall preserve first-person voice, completeness preferences, and context binding independently per language.

**Article 4.6.1.3.** Language selection shall not trap the user or obscure global navigation, emergency access, or recovery.

#### SECTION II — Consistency Across Languages

**Article 4.6.1.4.** Interaction behavior shall remain consistent across languages for identical navigation contexts, subject only to legitimate linguistic difference in labels and phrases.

**Article 4.6.1.5.** Engineering shall not maintain parallel interaction schemes per language that bypass constitutional navigation or gesture law.

**Article 4.6.1.6.** Future multilingual capability shall extend the communication engine; it shall not replace constitutional interaction language.

---

# PART V — ACCESSIBILITY ENGINEERING

## TITLE I — EYE-CONTROL ENGINE

### CHAPTER I — Eye-Only Operation

#### SECTION I — Complete Path Engineering

**Article 5.1.1.1.** Engineering shall implement an eye-control engine capable of achieving every navigation, communication, and emergency action required for independent use without reliance on other input modalities.

**Article 5.1.1.2.** Eye-only operation shall not be a degraded feature subset. Engineering completeness shall be verified against full constitutional reachability requirements.

**Article 5.1.1.3.** Every eye action available in production paths shall be visibly explained before commitment.

#### SECTION II — Feedback and Tuning

**Article 5.1.1.4.** Eye-control engineering shall provide perceptible dwell, selection, and confirmation feedback without reliance on color alone.

**Article 5.1.1.5.** Eye-control paths to Back, Cancel, Categories, and Emergency shall remain unobstructed in all production states.

**Article 5.1.1.6.** Eye-control failure shall degrade to recoverable states with emergency preservation where physically possible.

---

## TITLE II — HUMAN TOUCH PARITY

### CHAPTER I — Equivalent Access

#### SECTION I — Parity Requirements

**Article 5.2.1.1.** Engineering shall implement human touch parity so that caregivers, helpers, testers, and authorized operators may invoke the same logical actions as eye input through touch where touch input exists.

**Article 5.2.1.2.** Every visible action shall be touch-accessible when touch input exists, except where constitutional safety confirmation deliberately distinguishes modalities—and even then, recovery and emergency parity shall remain.

**Article 5.2.1.3.** Touch and eye input shall produce equivalent outcomes for identical actions on identical context.

#### SECTION II — Assisted Operation Boundaries

**Article 5.2.1.4.** Caregiver or helper touch that overrides user authority shall require an explicit, visible assisted-operation mode.

**Article 5.2.1.5.** Touch parity shall not silently bypass confirmation, safety, or User Sovereignty rules applicable to eye input.

**Article 5.2.1.6.** When modality distinction matters for safety, engineering shall indicate which input path governs the pending action.

---

## TITLE III — VISUAL ACCESSIBILITY

### CHAPTER I — Adaptive Presentation

#### SECTION I — Scaling and Contrast

**Article 5.3.1.1.** Engineering shall implement adaptive presentation scaling so that text and controls remain readable across user needs and display conditions.

**Article 5.3.1.2.** High-contrast presentation shall be available and shall preserve label legibility and distinction between available and unavailable actions.

**Article 5.3.1.3.** Readable typography engineering shall govern minimum text size, spacing, and contrast relationships for sustained use.

#### SECTION II — Labelling Integrity

**Article 5.3.1.4.** Icons and arrows shall not appear without labels in production paths. Symbol alone shall not carry actionable meaning.

**Article 5.3.1.5.** Decorative elements shall not mimic actionable controls.

**Article 5.3.1.6.** Status essential to orientation—mode, category, page, pending confirmation—shall remain visually accessible during interaction.

---

## TITLE IV — SELF-EXPLAINING AND GUIDED LEARNING

### CHAPTER I — Learnability Engineering

#### SECTION I — Self-Explaining Interface

**Article 5.4.1.1.** Engineering shall implement self-explaining interface behavior: every visible action shall disclose its effect through label, contextual help, or progressive on-surface explanation.

**Article 5.4.1.2.** Users shall not be required to memorize invisible interaction rules. Rules governing behavior shall be discoverable from the interface or guided learning systems.

**Article 5.4.1.3.** The interface shall continuously teach the interaction language through consistent placement, labelling, and contextual reinforcement.

#### SECTION II — Guided Learning Systems

**Article 5.4.1.4.** Engineering shall provide guided learning systems that reduce dependence on caregivers for interaction knowledge, consistent with the Principle of Learnability.

**Article 5.4.1.5.** Guided learning shall support independence: assistance may diminish as stable patterns are mastered, without withholding safety or recovery information.

**Article 5.4.1.6.** Learning systems shall not gate emergency, recovery, or global navigation behind lesson completion.

---

## TITLE V — USER SETTINGS AND FATIGUE

### CHAPTER I — Adjustable Parameters

#### SECTION I — User-Controllable Settings

**Article 5.5.1.1.** Engineering shall expose user-adjustable settings for sensitivity, response time, dwell duration, scroll step, and feedback delay within safe bounds.

**Article 5.5.1.2.** Settings shall persist per user where identity or profile systems exist, without breaking constitutional guarantees when restored.

**Article 5.5.1.3.** Safe defaults shall apply when user settings are absent, unset, or reset.

#### SECTION II — Fatigue Reduction Engineering

**Article 5.5.1.4.** Fatigue reduction engineering shall minimize repetitive motion, excessive depth, and unnecessary confirmation while preserving required safety confirmations.

**Article 5.5.1.5.** Frequent communication paths shall be engineering-optimized for Least Effort relative to rare paths.

**Article 5.5.1.6.** Sustained error, abort, or session abandonment rates shall be treated as engineering signals requiring design response.

---

# PART VI — SAFETY ENGINEERING

## TITLE I — EMERGENCY ENGINE

### CHAPTER I — Emergency System Requirements

#### SECTION I — Authority and Registration

**Article 6.1.1.1.** Engineering shall implement an emergency engine distinct from vocabulary and category systems. Emergency is system authority, not communicative content.

**Article 6.1.1.2.** Emergency shall register to constitutional gesture L6 R0 and shall override mode, local gesture, and non-critical prompt behavior when invoked.

**Article 6.1.1.3.** Emergency shall be visible and reachable in all production interaction states.

#### SECTION II — Invocation and Delay

**Article 6.1.1.4.** Engineering shall not interpose confirmation layers that materially delay emergency invocation beyond what is strictly necessary to distinguish emergency from accidental adjacent input, favoring rapid access.

**Article 6.1.1.5.** Emergency handling states shall themselves provide recovery paths consistent with constitutional law once immediate emergency response permits.

**Article 6.1.1.6.** Emergency engine behavior shall be inspectable and subject to dedicated validation authority.

---

## TITLE II — RECOVERY ENGINE

### CHAPTER I — Recovery System Requirements

#### SECTION I — Universal Recovery

**Article 6.2.1.1.** Engineering shall implement a recovery engine responsible for guaranteeing escape from error, partial completion, timeout, and undesired navigation states.

**Article 6.2.1.2.** Recovery shall always be simpler than the action that caused the error state, measured in user effort and cognitive steps.

**Article 6.2.1.3.** Repeated Back or Cancel invocation shall be interpreted as escalating demand for retreat; engineering shall not respond with increased commitment requirements.

#### SECTION II — Recovery Validation

**Article 6.2.1.4.** Recovery paths shall be validated for every navigation state before release.

**Article 6.2.1.5.** Recovery engine shall prefer return to Categories or equivalent globally labelled navigation home when local context is lost.

**Article 6.2.1.6.** Recovery shall not depend on completing non-emergency steps or on caregiver intervention in production design.

---

## TITLE III — NAVIGATION AND GESTURE SAFETY ENGINES

### CHAPTER I — Conflict Prevention

#### SECTION I — Navigation Safety Engine

**Article 6.3.1.1.** Engineering shall implement navigation safety mechanisms that detect and prevent navigation conflicts, dead ends, and unreachable global commands before release.

**Article 6.3.1.2.** Navigation safety engineering shall treat reachability of Categories, Back, Cancel, and Emergency as invariants.

**Article 6.3.1.3.** Hidden navigation state—where location or available commands cannot be determined—is a hard failure.

#### SECTION II — Gesture Safety Engine

**Article 6.3.1.4.** Engineering shall implement gesture safety mechanisms that detect ambiguous assignments, registry collisions, and single-gesture accidental communication triggers.

**Article 6.3.1.5.** Gesture safety engineering shall enforce resolution order and reserved registry integrity continuously upon registry change.

**Article 6.3.1.6.** Ambiguous gestures and navigation-gesture conflicts shall block release until resolved.

---

## TITLE IV — STATE CONSISTENCY AND SAFE DEFAULTS

### CHAPTER I — Deterministic Safety Posture

#### SECTION I — State Consistency

**Article 6.4.1.1.** Engineering shall maintain state consistency between navigation context, gesture registries, vocabulary scope, and presentation layer at all times during operation.

**Article 6.4.1.2.** If two situations appear identical to the user, engineering systems shall produce identical behavior unless visible distinction is presented before action is required.

**Article 6.4.1.3.** State inconsistency detected at runtime shall trigger safe recovery defaults rather than silent continuation.

#### SECTION II — Safe Defaults

**Article 6.4.1.4.** Safe defaults shall preserve recovery, emergency reachability, and user sovereignty on startup, reset, crash recovery, and settings loss.

**Article 6.4.1.5.** Engineering shall not default to aggressive automation, silent sending, or irreversible navigation advancement.

**Article 6.4.1.6.** Destructive or high-consequence actions shall require deliberate confirmation distinct from ordinary Select or Save engineering paths.

---

# PART VII — VALIDATION AUTHORITIES

## TITLE I — PURPOSE OF VALIDATION

### CHAPTER I — Validation in the Hierarchy

#### SECTION I — Subordinate Proof

**Article 7.1.1.1.** Validation Authorities exist to detect violation of the LIC and this Charter. They do not create interaction law.

**Article 7.1.1.2.** Each Validation Authority shall have defined scope, inputs, failure criteria, and traceability to constitutional and charter Articles.

**Article 7.1.1.3.** Validation failure in a mandatory authority shall block release of affected production interaction paths until remedied or constitutionally exempted by amendment.

#### SECTION II — Coverage Obligation

**Article 7.1.1.4.** Validation shall cover all production interaction paths, including integrations and mode-specific surfaces.

**Article 7.1.1.5.** Validation Authorities shall be revised upon LIC or LIEC amendment before amended rules govern production.

**Article 7.1.1.6.** This Part describes responsibilities only. It does not itself implement validators.

---

## TITLE II — GUIDED NAVIGATION AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.2.1.1.** The Guided Navigation Authority shall verify that navigation context is singular, deterministic, and visibly reflected.

**Article 7.2.1.2.** It shall verify mode transitions, category reachability, and state machine integrity against charter requirements.

**Article 7.2.1.3.** It shall detect navigation dead ends, undefined transitions, and silent context mutation in production paths.

---

## TITLE III — GESTURE CONFLICT AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.3.1.1.** The Gesture Conflict Authority shall verify reserved, global, and local registries for collision, duplication of active meaning, and namespace violation.

**Article 7.3.1.2.** It shall verify deterministic resolution order and prohibition of single-gesture accidental communication.

**Article 7.3.1.3.** It shall verify that reserved gestures do not appear as vocabulary selections.

---

## TITLE IV — ACCESSIBILITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.4.1.1.** The Accessibility Authority shall verify eye-only completeness for navigation, communication, and emergency actions.

**Article 7.4.1.2.** It shall verify labelling integrity, readable typography thresholds, high-contrast availability, and self-explaining interface requirements.

**Article 7.4.1.3.** It shall verify that icons and arrows do not appear without labels in production paths.

---

## TITLE V — COMMUNICATION AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.5.1.1.** The Communication Authority shall verify page-local vocabulary processing and context-bound output discipline.

**Article 7.5.1.2.** It shall verify first-person phrase availability, completeness preferences, and naturalness requirements within registered phrase sources.

**Article 7.5.1.3.** It shall verify that AI assistance does not speak, send, or finalize output without user confirmation.

---

## TITLE VI — EMERGENCY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.6.1.1.** The Emergency Authority shall verify emergency registration, visibility, reachability, and override behavior in all production states.

**Article 7.6.1.2.** It shall verify that emergency is not represented as vocabulary or category content.

**Article 7.6.1.3.** It shall verify that material delay or confirmation burdens on emergency invocation are absent beyond constitutionally permitted distinction minima.

---

## TITLE VII — NAVIGATION REACHABILITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.7.1.1.** The Navigation Reachability Authority shall verify invariant reachability of Categories, Back, Cancel, and Emergency from every production navigation state.

**Article 7.7.1.2.** It shall verify global navigation permanence and labelling across modes and integrations.

**Article 7.7.1.3.** It shall verify that overlays and third-party surfaces preserve constitutional reachability while active.

---

## TITLE VIII — HUMAN TOUCH PARITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.8.1.1.** The Human Touch Parity Authority shall verify that visible actions are touch-accessible when touch input exists.

**Article 7.8.1.2.** It shall verify equivalent outcomes between touch and eye input for identical actions and context.

**Article 7.8.1.3.** It shall verify that assisted-operation override requires explicit visible mode and does not bypass safety or sovereignty rules silently.

---

## TITLE IX — RECOVERY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.9.1.1.** The Recovery Authority shall verify labelled recovery paths from every navigation state.

**Article 7.9.1.2.** It shall verify proportional recovery effort and prohibition of dead ends.

**Article 7.9.1.3.** It shall verify correct interpretation of repeated Back or Cancel as retreat demand without commitment escalation.

---

## TITLE X — CONTEXT AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Scope

**Article 7.10.1.1.** The Context Authority shall verify alignment among navigation context, gesture processing scope, and vocabulary eligibility.

**Article 7.10.1.2.** It shall verify current-page-only processing and prohibition of silent context switches during communication.

**Article 7.10.1.3.** It shall verify that identical apparent user situations produce identical interaction behavior unless visibly distinguished beforehand.

---

# PART VIII — FUTURE ENGINEERING

## TITLE I — EVOLUTION UNDER CONSTITUTIONAL LAW

### CHAPTER I — Non-Violation Requirement

#### SECTION I — Binding Future Work

**Article 8.1.1.1.** Engineering evolution must never violate the LIC. Future capability is valid only insofar as it extends constitutional interaction language without replacing it.

**Article 8.1.1.2.** Features that cannot comply shall await constitutional amendment or redesign, not informal production exception.

**Article 8.1.1.3.** Experimental systems shall remain isolated from production interaction paths until validated for full compliance.

---

## TITLE II — FUTURE HARDWARE AND SENSORS

### CHAPTER I — Translation Requirement

#### SECTION I — Device Independence

**Article 8.2.1.1.** Future hardware, new interaction devices, and additional sensors shall translate physical input into existing constitutional navigation commands and gesture identifiers before meaning is applied.

**Article 8.2.1.2.** Hardware shall not redefine global gestures at the device layer or introduce parallel gesture namespaces in production.

**Article 8.2.1.3.** Hardware failure shall degrade gracefully, preserving recovery and emergency where physically possible.

#### SECTION II — Eye-Tracking Improvements

**Article 8.2.1.4.** Eye-tracking improvements shall increase accuracy, comfort, and reliability without reducing labelling, recovery, or emergency reachability.

**Article 8.2.1.5.** Improved eye-tracking shall remain subordinate to user-adjustable sensitivity and response-time settings within safe bounds.

**Article 8.2.1.6.** Eye-tracking innovation shall not introduce invisible rules or modality-exclusive paths that violate touch parity requirements where touch exists.

---

## TITLE III — AI INTEGRATION

### CHAPTER I — Assistive Extension

#### SECTION I — Boundaries

**Article 8.3.1.1.** AI integration shall extend vocabulary discovery, ranking, and preparation within constitutional boundaries; it shall not override User Sovereignty.

**Article 8.3.1.2.** Future AI capabilities shall be subject to Communication Authority and Context Authority requirements before production release.

**Article 8.3.1.3.** AI that alters navigation, suppresses global commands, or hides recovery remains prohibited regardless of model generation.

---

## TITLE IV — INTEGRATIONS

### CHAPTER I — Smart Home, Video, and Mobility

#### SECTION I — Constitutional Fit

**Article 8.4.1.1.** Smart home integration, video calling, wheelchair interfaces, and similar extensions shall fit the constitutional interaction model: global navigation, labelled recovery, emergency supremacy, and escapable modes.

**Article 8.4.1.2.** Integrations shall not introduce vocabulary masquerading as emergency or global navigation.

**Article 8.4.1.3.** Third-party surfaces embedded within LISA remain subject to validation of reachability, context integrity, and Human Touch Parity while active.

#### SECTION II — Least Effort Across Integrations

**Article 8.4.1.4.** Integrations shall minimize depth and gesture burden required to enter, use, and exit integrated functions.

**Article 8.4.1.5.** Integration exit shall always return the user to a labelled LISA navigation state without trap or hidden state.

**Article 8.4.1.6.** Integration engineering shall register with navigation and context systems so validators can verify compliance.

---

# PART IX — COMPLIANCE

## TITLE I — CONSTITUTIONAL COMPLIANCE

### CHAPTER I — Primary Obligation

#### SECTION I — Conformance

**Article 9.1.1.1.** Constitutional compliance is the primary release obligation for all user-facing LISA interaction. Feature completeness without constitutional conformance is invalid.

**Article 9.1.1.2.** Compliance shall be assessed against the LIC, interpreted through the Constitutional Principles and Founding Purpose.

**Article 9.1.1.3.** Partial compliance is insufficient where safety invariants—emergency, recovery, reachability, and User Sovereignty—are at stake.

---

## TITLE II — ENGINEERING COMPLIANCE

### CHAPTER I — Charter Conformance

#### SECTION I — LIEC Requirements

**Article 9.2.1.1.** Engineering compliance requires satisfaction of this Charter's deterministic requirements traceable to constitutional law.

**Article 9.2.1.2.** Engineering subsystems shall document their charter Article traceability for review and validation design.

**Article 9.2.1.3.** Engineering compliance failures shall be remedied before release, not documented as accepted risk in production interaction paths.

---

## TITLE III — VALIDATION COMPLIANCE

### CHAPTER I — Proof Before Release

#### SECTION I — Mandatory Authorities

**Article 9.3.1.1.** Validation compliance requires successful execution of all Validation Authorities mandated for the affected release scope.

**Article 9.3.1.2.** Validation results shall be retained sufficient to demonstrate due diligence without unnecessary retention of private user communication.

**Article 9.3.1.3.** Waivers of mandatory validation in production paths are prohibited except where constitutional amendment explicitly permits a defined exception.

---

## TITLE IV — RELEASE AND REGRESSION COMPLIANCE

### CHAPTER I — Continuous Conformance

#### SECTION I — Release Gate

**Article 9.4.1.1.** Release compliance shall block shipment of user-facing interaction that fails constitutional, engineering, or mandatory validation requirements.

**Article 9.4.1.2.** Release review shall include reachability, recovery, emergency, gesture registry, and context integrity checks appropriate to the change scope.

**Article 9.4.1.3.** No release channel—stable, preview, or integrated deployment—is exempt from compliance obligations in user-facing paths.

#### SECTION II — Regression

**Article 9.4.1.4.** Regression compliance requires that previously satisfied constitutional and charter guarantees remain satisfied after change.

**Article 9.4.1.5.** Regression of safety invariants shall be treated with priority equal to or greater than functional regression.

**Article 9.4.1.6.** Refactors, optimizations, and integration upgrades re-open full compliance obligation for affected interaction surfaces.

---

## TITLE V — AMENDMENT PROPAGATION

### CHAPTER I — Change Propagation

#### SECTION I — Order of Update

**Article 9.5.1.1.** Upon LIC amendment, this Charter shall be revised as needed to preserve accurate translation of constitutional law before amended rules govern production.

**Article 9.5.1.2.** Validation Authorities shall be updated to reflect charter and constitutional changes before release under amended law.

**Article 9.5.1.3.** Implementations shall not activate behavior governed by amended law until charter and validation layers reflect the amendment.

#### SECTION II — User Notice

**Article 9.5.1.4.** Material changes to gestures, navigation, emergency behavior, or recovery shall include user-facing notice as required by the LIC.

**Article 9.5.1.5.** Amendment propagation shall preserve safe defaults during transition periods.

**Article 9.5.1.6.** Informal team communication shall not substitute for documented amendment propagation.

---

## ENGINEERING HIERARCHY

```
LIC
(LISA Interaction Constitution — supreme interaction law)
        ↓
LIEC
(LISA Interaction Engineering Charter — deterministic engineering requirements)
        ↓
Validation Authorities
(Guided Navigation, Gesture Conflict, Accessibility, Communication,
 Emergency, Navigation Reachability, Human Touch Parity, Recovery, Context)
        ↓
Implementation
(Running systems that must conform — lowest authority)
```

---

*End of LISA Interaction Engineering Charter, Draft Version 1.*
