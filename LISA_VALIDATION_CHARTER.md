# THE LISA VALIDATION CHARTER (LVC)

**Draft Version 1**

---

## PREAMBLE

We establish this Charter as the supreme authority governing all validation systems within LISA.

The LISA Interaction Constitution defines constitutional interaction law. The LISA Interaction Engineering Charter translates that law into deterministic engineering requirements. This Validation Charter defines how engineering compliance shall be verified. It governs every current and future Validation Authority, every validation procedure, and every official Validation Report.

This Charter does not define interaction law. It does not define engineering requirements. It defines validation governance—the standards by which compliance is proved, recorded, interpreted, and enforced.

No validation system may contradict the Constitution or the Engineering Charter. Where authority is in question, it resolves in strict order:

**LIC → LIEC → LVC → Validation Authorities → Validation Reports → Implementation**

Validation Authorities derive their legitimacy exclusively from this Charter, the LIEC, and the LIC. They verify compliance; they do not legislate. Validation Reports record the outcome of that verification; they do not substitute for remediation.

The provisions herein bind all present and future validation within LISA, regardless of platform, deployment model, automation method, or release channel.

---

# PART I — VALIDATION AUTHORITY

## TITLE I — SUBORDINATION TO SUPERIOR LAW

### CHAPTER I — Source of Validation Authority

#### SECTION I — Derivation

**Article 1.1.1.1.** Validation derives authority exclusively from the LISA Interaction Constitution and the LISA Interaction Engineering Charter, as interpreted through this Charter.

**Article 1.1.1.2.** Validation Authorities may verify only what superior law requires. They shall not expand, narrow, or reinterpret constitutional or engineering obligations without amendment to the governing instrument.

**Article 1.1.1.3.** This Charter is subordinate to the LIC and the LIEC. Where this Charter conflicts with either, the superior document prevails.

#### SECTION II — Limits of Validation Power

**Article 1.1.1.4.** Validators cannot invent constitutional law. No validation rule, criterion, or report outcome shall create interaction obligations not found in the LIC.

**Article 1.1.1.5.** Validators cannot invent engineering law. No validation rule, criterion, or report outcome shall create engineering obligations not found in the LIEC, except as this Charter governs validation process itself.

**Article 1.1.1.6.** Validators verify compliance only. Verification is proof, not policy.

---

## TITLE II — PURPOSE OF VALIDATION

### CHAPTER I — Verification Mission

#### SECTION I — Compliance Proof

**Article 1.2.1.1.** The purpose of validation is to produce reliable, evidence-driven proof that implementations conform to the LIC and LIEC, or to identify and document non-conformance with sufficient clarity to support remediation and release decisions.

**Article 1.2.1.2.** Validation shall serve the Founding Purpose: ensuring that persons who depend on LISA receive systems that restore dignified, user-directed communication without entrapment, ambiguity, or usurpation of user authority.

**Article 1.2.1.3.** Validation success does not imply perfection of user experience; it implies demonstrated satisfaction of governing law within declared scope.

#### SECTION II — Non-Substitution

**Article 1.2.1.4.** Validation shall not substitute for engineering judgment in lawful design, nor for constitutional amendment where law must change.

**Article 1.2.1.5.** Absence of validation does not excuse absence of compliance. The obligation to conform exists independent of proof machinery.

**Article 1.2.1.6.** Presence of validation does not excuse invalid implementations. A passing report obtained by improper scope reduction, suppressed evidence, or waived mandatory authority is void.

---

## TITLE III — VALIDATION INDEPENDENCE

### CHAPTER I — Impartial Verification

#### SECTION I — Independence Requirement

**Article 1.3.1.1.** Validation shall be conducted with independence sufficient to resist undue influence from schedule pressure, product preference, integration convenience, or implementation team self-interest.

**Article 1.3.1.2.** Validation Authorities shall apply criteria derived from governing law, not from undocumented team consensus or informal exception.

**Article 1.3.1.3.** Persons or processes responsible for proving compliance should be separable from persons or processes solely responsible for shipping non-compliant convenience, where organizational structure permits.

#### SECTION II — Prohibited Influence

**Article 1.3.1.4.** Release desire shall not alter validation criteria, suppress mandatory authorities, or reclassify failures without evidence and charter authority.

**Article 1.3.1.5.** Waivers of mandatory validation in user-facing production paths are prohibited except where constitutional amendment explicitly permits a defined, documented exception.

**Article 1.3.1.6.** Validation independence is a governance requirement, not an organizational chart detail. Its substance matters more than its label.

---

## TITLE IV — DETERMINISTIC AND REPEATABLE VALIDATION

### CHAPTER I — Objectivity Standards

#### SECTION I — Deterministic Results

**Article 1.4.1.1.** Validation shall be deterministic. Given identical scope, identical implementation state, and identical governing law, validation shall reach the same official outcome.

**Article 1.4.1.2.** Subjective approval without recorded criteria, evidence, and reasoning is not validation within the meaning of this Charter.

**Article 1.4.1.3.** Discretionary override of an official validation outcome requires documented authority, evidence, and traceability under this Charter.

#### SECTION II — Repeatability

**Article 1.4.1.4.** Validation shall be repeatable. Another qualified validator, or the same authority at a later time, shall be able to reproduce the result from preserved evidence and declared criteria.

**Article 1.4.1.5.** Validation procedures shall minimize dependence on unrecorded operator knowledge sometimes called tribal knowledge.

**Article 1.4.1.6.** Non-repeatable validation may inform engineering investigation but shall not alone satisfy mandatory release proof requirements.

---

# PART II — VALIDATION PRINCIPLES

## TITLE I — EVIDENCE AND REASONING

### CHAPTER I — Evidence Over Opinion

#### SECTION I — Proof Standard

**Article 2.1.1.1.** Evidence over opinion is a governing validation principle. Official outcomes shall rest on recorded evidence, not on undocumented belief, reputation, or intent.

**Article 2.1.1.2.** Validation reasoning shall be explicit enough that a reviewer can understand why an outcome was reached without guessing the validator's unstated assumptions.

**Article 2.1.1.3.** Assertions of compliance without supporting evidence are insufficient for PASS in mandatory authorities.

#### SECTION II — Traceability

**Article 2.1.1.4.** Traceability is a governing validation principle. Every official finding shall trace to affected constitutional Articles, affected engineering Articles, or explicit charter provisions governing validation scope.

**Article 2.1.1.5.** Findings shall identify the subsystem or interaction surface examined, so that remediation can be directed without ambiguity.

**Article 2.1.1.6.** Untraceable findings shall not block or clear release in mandatory authorities.

---

## TITLE II — COMPLETENESS AND TRANSPARENCY

### CHAPTER I — Scope Integrity

#### SECTION I — Completeness

**Article 2.2.1.1.** Completeness is a governing validation principle. Validation scope shall cover all interaction paths affected by a change, including integrations, mode-specific surfaces, and recovery paths, as required by the LIEC and this Charter.

**Article 2.2.1.2.** Scope reduction that excludes safety invariants, emergency reachability, or recovery paths to obtain passage is prohibited.

**Article 2.2.1.3.** Completeness shall be declared before execution and recorded in the Validation Report.

#### SECTION II — Transparency

**Article 2.2.1.4.** Transparency is a governing validation principle. Validation Reports shall disclose scope, criteria, evidence summary, outcome, and known limitations.

**Article 2.2.1.5.** Material limitations of proof—partial coverage, simulated environments, or deferred authorities—shall be visible to release decision-makers.

**Article 2.2.1.6.** Concealed validation failure, suppressed evidence, or undisclosed scope exclusion voids official reliance on the report.

---

## TITLE III — PROPORTIONALITY AND ERROR DISCIPLINE

### CHAPTER I — Balanced Proof

#### SECTION I — Proportionality

**Article 2.3.1.1.** Proportionality is a governing validation principle. Validation effort shall match the risk and scope of the change under review.

**Article 2.3.1.2.** Minor changes shall not require full-system proof unless they touch safety invariants, global navigation, emergency, gesture registries, or context integrity.

**Article 2.3.1.3.** Major changes affecting interaction law surfaces shall receive commensurately complete validation regardless of schedule pressure.

#### SECTION II — False Positive and False Negative Discipline

**Article 2.3.1.4.** Least false positives is a governing validation principle. Validation criteria shall minimize incorrect failure findings that waste remediation effort without protecting users, consistent with safety priority.

**Article 2.3.1.5.** Least false negatives is a governing validation principle. Validation criteria shall minimize incorrect pass findings that permit constitutional or engineering violation to ship, especially for safety invariants.

**Article 2.3.1.6.** Where false positive and false negative incentives conflict, protection against false negatives in safety invariants shall prevail.

---

## TITLE IV — CONSISTENCY OF VALIDATION

### CHAPTER I — Uniform Application

#### SECTION I — Deterministic Principles Applied

**Article 2.4.1.1.** The principles of this Part shall apply to all Validation Authorities, manual or automated, local or remote, present or future.

**Article 2.4.1.2.** Identical conditions across validation runs shall produce identical official outcomes unless governing law or declared scope has changed.

**Article 2.4.1.3.** Validation Authorities shall not maintain private criteria that contradict published charter responsibilities.

#### SECTION II — Independence Reaffirmed

**Article 2.4.1.4.** Independence, repeatability, and evidence over opinion apply equally whether validation is performed by human reviewers, automated systems, or combined methods.

**Article 2.4.1.5.** Automation shall not obscure reasoning. Automated validation shall produce evidence and reasoning suitable for human audit.

**Article 2.4.1.6.** Human validation shall not bypass evidence requirements through informal sign-off.

---

# PART III — VALIDATION AUTHORITIES

## TITLE I — AUTHORITY GOVERNANCE

### CHAPTER I — Nature of Validation Authorities

#### SECTION I — Definition

**Article 3.1.1.1.** A Validation Authority is a governed verification body—human, automated, or combined—charged with examining a defined domain of LISA interaction for compliance with the LIC and LIEC under this Charter.

**Article 3.1.1.2.** Every Validation Authority shall have declared scope, inputs, failure criteria, evidence requirements, and traceability to governing Articles.

**Article 3.1.1.3.** Future Validation Authorities shall derive authority exclusively from this Charter and shall be registered before their reports govern release decisions.

#### SECTION II — Authority Boundaries

**Article 3.1.1.4.** Validation Authorities describe responsibilities in this Part. This Charter does not implement validators.

**Article 3.1.1.5.** No Validation Authority may expand its scope unilaterally into domains reserved to another authority without charter amendment or explicit delegated scope declaration under this Charter.

**Article 3.1.1.6.** Overlapping authority is permitted where safety requires redundant proof; contradictory authority criteria are prohibited.

---

## TITLE II — GUIDED NAVIGATION AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.2.1.1.** The Guided Navigation Authority shall verify that navigation context is singular, deterministic, and visibly reflected to the user.

**Article 3.2.1.2.** It shall verify mode transitions, category reachability within navigation structure, and navigation state machine integrity against LIEC requirements.

**Article 3.2.1.3.** It shall detect navigation dead ends, undefined transitions, and silent context mutation in production interaction paths within its declared scope.

#### SECTION II — Reporting Obligations

**Article 3.2.1.4.** It shall record evidence sufficient to identify affected navigation states, transitions, and constitutional or engineering Articles implicated by any finding.

**Article 3.2.1.5.** It shall distinguish failures of global navigation consistency from failures of local mode presentation where both are present.

**Article 3.2.1.6.** It is mandatory for releases affecting navigation context, mode structure, or navigation state machines.

---

## TITLE III — GESTURE CONFLICT AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.3.1.1.** The Gesture Conflict Authority shall verify reserved, global, and local gesture registries for collision, duplication of active meaning, and namespace violation.

**Article 3.3.1.2.** It shall verify deterministic gesture resolution order and prohibition of single-gesture accidental communication.

**Article 3.3.1.3.** It shall verify that reserved gestures do not appear as vocabulary selections on any production page.

#### SECTION II — Reporting Obligations

**Article 3.3.1.4.** It shall record conflicting gesture identifiers, affected scopes, and implicated registry entries with enough detail to support remediation.

**Article 3.3.1.5.** It shall treat global and emergency gesture conflict as highest-severity findings within its domain.

**Article 3.3.1.6.** It is mandatory for releases affecting gesture registries, gesture processing, or input resolution order.

---

## TITLE IV — ACCESSIBILITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.4.1.1.** The Accessibility Authority shall verify eye-only completeness for navigation, communication, and emergency actions required for independent use.

**Article 3.4.1.2.** It shall verify labelling integrity, readable typography thresholds, high-contrast availability, and self-explaining interface requirements.

**Article 3.4.1.3.** It shall verify that icons and arrows do not appear without labels in production paths.

#### SECTION II — Reporting Obligations

**Article 3.4.1.4.** It shall record whether failures affect eye-only operation, visual clarity, or learnability surfaces distinctly.

**Article 3.4.1.5.** It shall verify user-adjustable accessibility-related settings behave within declared safe bounds where such settings exist.

**Article 3.4.1.6.** It is mandatory for releases affecting presentation, eye input, labelling, or accessibility settings.

---

## TITLE V — COMMUNICATION AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.5.1.1.** The Communication Authority shall verify page-local vocabulary processing and context-bound output discipline.

**Article 3.5.1.2.** It shall verify first-person phrase availability, completeness preferences, naturalness requirements, and listener-clarity obligations within registered phrase sources.

**Article 3.5.1.3.** It shall verify that assistive or predictive systems do not speak, send, or finalize output without user confirmation.

#### SECTION II — Reporting Obligations

**Article 3.5.1.4.** It shall record whether failures arise from vocabulary scope, phrase registry content, context binding, or output finalization paths.

**Article 3.5.1.5.** It shall trace communication findings to active category and page context where relevant.

**Article 3.5.1.6.** It is mandatory for releases affecting vocabulary, phrase registries, output paths, or AI-assisted communication.

---

## TITLE VI — EMERGENCY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.6.1.1.** The Emergency Authority shall verify emergency registration, visibility, reachability, and override behavior in all production interaction states within scope.

**Article 3.6.1.2.** It shall verify that emergency is not represented as vocabulary, category content, or ordinary communicative selection.

**Article 3.6.1.3.** It shall verify that material delay or confirmation burdens on emergency invocation are absent beyond constitutionally permitted distinction minima.

#### SECTION II — Reporting Obligations

**Article 3.6.1.4.** It shall record each production state examined and any state where emergency reachability or override fails.

**Article 3.6.1.5.** Emergency findings shall be classified as constitutional safety findings regardless of other domain overlap.

**Article 3.6.1.6.** It is mandatory for all releases affecting user-facing interaction paths, without exception.

---

## TITLE VII — NAVIGATION REACHABILITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.7.1.1.** The Navigation Reachability Authority shall verify invariant reachability of Categories, Back, Cancel, and Emergency from every production navigation state within scope.

**Article 3.7.1.2.** It shall verify global navigation permanence and labelling across modes and integrations.

**Article 3.7.1.3.** It shall verify that overlays, dialogs, and third-party surfaces preserve constitutional reachability while active.

#### SECTION II — Reporting Obligations

**Article 3.7.1.4.** It shall enumerate unreachable or mislabeled global commands by state, mode, and surface.

**Article 3.7.1.5.** Reachability failures shall include remediation guidance identifying minimum recovery required for compliance.

**Article 3.7.1.6.** It is mandatory for all releases affecting user-facing interaction paths, without exception.

---

## TITLE VIII — RECOVERY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.8.1.1.** The Recovery Authority shall verify labelled recovery paths from every navigation state within scope.

**Article 3.8.1.2.** It shall verify proportional recovery effort and prohibition of dead ends.

**Article 3.8.1.3.** It shall verify correct handling of repeated Back or Cancel as retreat demand without commitment escalation.

#### SECTION II — Reporting Obligations

**Article 3.8.1.4.** It shall identify any state from which recovery requires more effort than the act that produced the error state, where measurable.

**Article 3.8.1.5.** Recovery findings shall be correlated with Guided Navigation and Navigation Reachability findings when states overlap.

**Article 3.8.1.6.** It is mandatory for releases affecting navigation state machines, overlays, confirmations, or error handling paths.

---

## TITLE IX — CONTEXT AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.9.1.1.** The Context Authority shall verify alignment among navigation context, gesture processing scope, and vocabulary eligibility.

**Article 3.9.1.2.** It shall verify current-page-only processing and prohibition of silent context switches during communication.

**Article 3.9.1.3.** It shall verify that identical apparent user situations produce identical interaction behavior unless visibly distinguished beforehand.

#### SECTION II — Reporting Obligations

**Article 3.9.1.4.** It shall record context mismatches with enough detail to identify which subsystems held divergent active context.

**Article 3.9.1.5.** Context findings that affect output attribution shall be classified as User Sovereignty risks.

**Article 3.9.1.6.** It is mandatory for releases affecting context resolution, page transitions, or multi-subsystem interaction coordination.

---

## TITLE X — HUMAN TOUCH PARITY AUTHORITY

### CHAPTER I — Responsibilities

#### SECTION I — Verification Scope

**Article 3.10.1.1.** The Human Touch Parity Authority shall verify that visible actions are touch-accessible when touch input exists on the device or surface under test.

**Article 3.10.1.2.** It shall verify equivalent outcomes between touch and eye input for identical actions and identical context.

**Article 3.10.1.3.** It shall verify that assisted-operation override requires explicit visible mode and does not bypass safety or sovereignty rules silently.

#### SECTION II — Reporting Obligations

**Article 3.10.1.4.** It shall distinguish parity failures of missing touch targets from failures of divergent outcome between modalities.

**Article 3.10.1.5.** It shall declare when touch input is absent on the examined surface and limit findings accordingly.

**Article 3.10.1.6.** It is mandatory for releases affecting input handling, visible controls, or assisted-operation modes on touch-capable surfaces.

---

## TITLE XI — FUTURE AUTHORITIES

### CHAPTER I — Registration and Subordination

#### SECTION I — Future Validator Governance

**Article 3.11.1.1.** Future Validation Authorities—including those governing artificial intelligence behavior, eye-tracking quality, hardware translation, multilingual presentation, cloud-hosted proof, and remote device examination—shall be registered under this Charter before their reports govern release.

**Article 3.11.1.2.** Future authorities shall declare scope, mandatory or optional status, evidence requirements, and traceability to LIC and LIEC Articles.

**Article 3.11.1.3.** Future authorities shall remain subordinate to this Charter and shall not invent interaction or engineering law.

#### SECTION II — Interim Proof

**Article 3.11.1.4.** Until a future authority is registered, its domain remains subject to manual or provisional proof only if such proof satisfies evidence and traceability requirements of this Charter.

**Article 3.11.1.5.** Provisional proof shall not waive mandatory authorities already in force for overlapping domains.

**Article 3.11.1.6.** Registration of a future authority shall include its relationship to existing authorities to prevent gaps and contradictions.

---

# PART IV — VALIDATION EVIDENCE

## TITLE I — EVIDENCE REQUIREMENTS

### CHAPTER I — Mandatory Evidence Elements

#### SECTION I — Core Record

**Article 4.1.1.1.** Validation must always be evidence-driven. No official outcome shall be issued without a evidence record sufficient for audit and remediation.

**Article 4.1.1.2.** Engineering evidence accompanying a finding shall include, at minimum: affected constitutional Articles, affected engineering Articles, affected subsystem or interaction surface, evidence summary, root cause where determinable, validation reasoning, and remediation guidance.

**Article 4.1.1.3.** Missing mandatory evidence elements shall downgrade confidence in the outcome and may require BLOCKED status until corrected.

#### SECTION II — Evidence Quality

**Article 4.1.1.4.** Evidence shall be factual, specific, and reproducible. Vague characterizations such as seems fine or probably compliant are insufficient.

**Article 4.1.1.5.** Screenshots, logs, state enumerations, registry snapshots, transition maps, and structured test results are examples of acceptable evidence forms; the form shall fit the finding.

**Article 4.1.1.6.** Evidence shall minimize retention of private user communication content beyond what verification requires.

---

## TITLE II — TRACEABILITY AND REASONING

### CHAPTER I — Legal and Engineering Trace

#### SECTION I — Article Trace

**Article 4.2.1.1.** Each finding shall cite affected constitutional Articles or Principles where constitutional violation or risk is identified.

**Article 4.2.1.2.** Each finding shall cite affected engineering Articles where engineering non-conformance is identified.

**Article 4.2.1.3.** Findings citing neither constitutional nor engineering authority shall not support FAIL or release-blocking outcomes in mandatory authorities.

#### SECTION II — Reasoning and Remediation

**Article 4.2.1.4.** Validation reasoning shall explain the logical path from evidence to outcome in terms understandable to engineering and governance reviewers.

**Article 4.2.1.5.** Remediation guidance shall indicate what must change to achieve compliance without prescribing implementation technology.

**Article 4.2.1.6.** Root cause shall be stated when known; when unknown, the report shall say so explicitly and identify further investigation required.

---

## TITLE III — EVIDENCE INTEGRITY

### CHAPTER I — Preservation and Audit

#### SECTION I — Retention

**Article 4.3.1.1.** Validation evidence shall be preserved for historical traceability for a period sufficient to support regression analysis and release audit, without unnecessary hoarding of sensitive content.

**Article 4.3.1.2.** Alteration of evidence after official outcome issuance shall be documented. Undocumented alteration voids reliance on the report.

**Article 4.3.1.3.** Validation Reports shall reference evidence identifiers or locations sufficient for independent review.

#### SECTION II — Auditability

**Article 4.3.1.4.** Automated validation shall emit machine-readable evidence where practical, human-auditable summaries always.

**Article 4.3.1.5.** Manual validation shall follow the same evidence minima as automated validation.

**Article 4.3.1.6.** Evidence gaps shall be listed as limitations, not hidden.

---

# PART V — VALIDATION RESULTS

## TITLE I — OFFICIAL OUTCOMES

### CHAPTER I — Result Taxonomy

#### SECTION I — Permitted Results

**Article 5.1.1.1.** Every Validation Authority shall issue exactly one official outcome per declared validation run from the set defined by this Charter: PASS, PASS WITH OBSERVATIONS, FAIL, BLOCKED, or NOT APPLICABLE.

**Article 5.1.1.2.** Unofficial labels shall not govern release. Colloquial approvals do not substitute for official outcomes.

**Article 5.1.1.3.** Composite release decisions shall derive from the collective official outcomes of all mandatory authorities within scope.

---

## TITLE II — MEANING OF OUTCOMES

### CHAPTER I — Constitutional Significance

#### SECTION I — PASS

**Article 5.2.1.1.** PASS means that, within declared scope, the examined implementation satisfies all criteria of the authority with evidence sufficient under this Charter.

**Article 5.2.1.2.** PASS does not waive other mandatory authorities, does not imply unchanged scope elsewhere, and does not certify future builds.

**Article 5.2.1.3.** PASS with incomplete evidence record is invalid and shall be reissued or downgraded.

#### SECTION II — PASS WITH OBSERVATIONS

**Article 5.2.1.4.** PASS WITH OBSERVATIONS means compliance within scope is demonstrated, but non-blocking issues, limitations, or improvement opportunities are recorded with evidence.

**Article 5.2.1.5.** Observations shall not be used to smuggle blocking defects out of FAIL classification.

**Article 5.2.1.6.** Observations that reveal safety invariant risk shall be escalated to FAIL or BLOCKED under mandatory authority rules.

#### SECTION III — FAIL

**Article 5.2.1.7.** FAIL means a confirmed constitutional or engineering violation, or confirmed failure to meet authority criteria, within declared scope.

**Article 5.2.1.8.** FAIL in a mandatory authority blocks release of affected user-facing interaction paths until remediated and revalidated, unless a documented constitutional exception applies.

**Article 5.2.1.9.** FAIL shall include remediation guidance and traceability as required by Part IV.

#### SECTION IV — BLOCKED

**Article 5.2.1.10.** BLOCKED means validation could not complete or could not be relied upon—for example, insufficient scope declaration, missing evidence, unavailable test surface, or validator malfunction.

**Article 5.2.1.11.** BLOCKED in a mandatory authority blocks release until the blocking condition is resolved and validation re-executed.

**Article 5.2.1.12.** BLOCKED is not a substitute for FAIL and shall not be used to avoid recording a known violation.

#### SECTION V — NOT APPLICABLE

**Article 5.2.1.13.** NOT APPLICABLE means the authority's domain is outside the declared change scope and no proof is required from this authority for the release under review.

**Article 5.2.1.14.** NOT APPLICABLE shall be justified by explicit scope reasoning, not assumed by default.

**Article 5.2.1.15.** NOT APPLICABLE is prohibited for Emergency Authority and Navigation Reachability Authority in any release affecting user-facing interaction paths.

---

## TITLE III — VALIDATION REPORTS

### CHAPTER I — Official Record

#### SECTION I — Report Requirements

**Article 5.3.1.1.** A Validation Report is the official record of validation outcomes, evidence, scope, and reasoning for a defined release or change set.

**Article 5.3.1.2.** Validation Reports shall aggregate authority outcomes without altering their individual official meanings.

**Article 5.3.1.3.** Release decision-makers shall rely on Validation Reports, not on informal summaries alone.

#### SECTION II — Report Integrity

**Article 5.3.1.4.** Validation Reports shall be dated, scoped, and attributable to named authorities or registered automated systems.

**Article 5.3.1.5.** Superseded reports shall be marked as such; the current report for a release candidate shall be identifiable.

**Article 5.3.1.6.** Validation Reports are subordinate to law. They record compliance; they do not create it.

---

# PART VI — RELEASE AUTHORITY

## TITLE I — RELEASE GOVERNANCE

### CHAPTER I — When Validation Blocks Release

#### SECTION I — Blocking Conditions

**Article 6.1.1.1.** Validation blocks release when any mandatory Validation Authority within applicable scope issues FAIL or BLOCKED.

**Article 6.1.1.2.** Validation blocks release when required evidence minima of Part IV are unmet for mandatory authorities.

**Article 6.1.1.3.** Validation blocks release when constitutional violations or safety invariant failures are known, even if formal validation has not yet completed, until proof of remediation exists.

#### SECTION II — Non-Blocking Conditions

**Article 6.1.1.4.** PASS WITH OBSERVATIONS does not, by itself, block release unless an observation is escalated under Article 5.2.1.6.

**Article 6.1.1.5.** NOT APPLICABLE in optional authorities does not, by itself, clear mandatory domains from proof obligation.

**Article 6.1.1.6.** Partial PASS across some authorities does not constitute release readiness if any mandatory authority within scope has not passed.

---

## TITLE II — MANDATORY AND OPTIONAL AUTHORITIES

### CHAPTER I — Mandatory Proof

#### SECTION I — Always Mandatory

**Article 6.2.1.1.** Emergency Authority and Navigation Reachability Authority are mandatory for every release affecting user-facing interaction paths, without exception.

**Article 6.2.1.2.** Additional authorities become mandatory when the change touches their domain, as declared in Part III reporting obligations.

**Article 6.2.1.3.** Mandatory status shall not be waived by schedule, channel, or audience.

#### SECTION II — Optional and Conditional Authorities

**Article 6.2.1.4.** Optional authorities are those not triggered by change scope but available for expanded proof at governance discretion.

**Article 6.2.1.5.** Declaring an authority NOT APPLICABLE requires explicit scope justification traceable to the change under review.

**Article 6.2.1.6.** Optional authorities may issue findings that inform engineering priority without blocking release unless findings reveal mandatory-domain violation.

---

## TITLE III — RELEASE READINESS

### CHAPTER I — Readiness Standard

#### SECTION I — Definition

**Article 6.3.1.1.** Release readiness means all mandatory Validation Authorities within applicable scope have issued PASS or PASS WITH OBSERVATIONS without escalated safety observation, and Validation Reports are complete under Part IV and Part V.

**Article 6.3.1.2.** Release readiness is a validation governance conclusion, not a moral guarantee of perfection.

**Article 6.3.1.3.** Release without readiness is a governance violation subject to remediation and retrospective review.

#### SECTION II — Violation Classification

**Article 6.3.1.4.** Constitutional violations are failures against the LIC or its Principles. They are the highest severity class and block release until remediated or constitutionally excepted.

**Article 6.3.1.5.** Engineering violations are failures against the LIEC without necessarily rising to constitutional breach. They block release when within mandatory authority scope and criteria.

**Article 6.3.1.6.** Validation process violations are failures against this Charter—such as missing evidence or invalid NOT APPLICABLE declarations. They block release until proof can be relied upon.

---

## TITLE IV — REGRESSION AT RELEASE

### CHAPTER I — Release-Time Regression

#### SECTION I — Regression Handling

**Article 6.4.1.1.** Release validation shall include regression proof appropriate to change scope so that previously validated guarantees are not silently lost.

**Article 6.4.1.2.** Regression failures in safety invariants shall block release with the same force as new failures.

**Article 6.4.1.3.** Known regression shipped without proof constitutes engineering and validation governance failure.

#### SECTION II — Channel Parity

**Article 6.4.1.4.** No release channel—stable, preview, field trial, or integrated deployment—is exempt from mandatory validation in user-facing paths.

**Article 6.4.1.5.** Reduced validation in non-user-facing experimental sandboxes is permitted only where production interaction paths are unreachable from the sandbox.

**Article 6.4.1.6.** Channel labels shall not redefine user-facing for the purpose of avoiding mandatory authorities.

---

# PART VII — REGRESSION AUTHORITY

## TITLE I — NON-REGRESSION GUARANTEE

### CHAPTER I — Silent Regression Prohibited

#### SECTION I — Validated Guarantees

**Article 7.1.1.1.** No validated guarantee may silently regress. What has been proved compliant and shipped to users shall remain subject to proof upon relevant change.

**Article 7.1.1.2.** Regression is a change that causes previously passing criteria to fail without corresponding amendment to governing law.

**Article 7.1.1.3.** Silent regression—failure without detection because validation was skipped, scope was narrowed, or reports were ignored—is a charter violation.

#### SECTION II — Priority

**Article 7.1.1.4.** Regression of emergency reachability, recovery paths, global navigation, or User Sovereignty guarantees shall be treated with priority equal to or greater than new feature failure.

**Article 7.1.1.5.** Regression remediation shall precede unrelated feature expansion where resources force choice and safety invariants are affected.

**Article 7.1.1.6.** Documentation of regression without remediation is not compliance.

---

## TITLE II — REGRESSION TESTING RESPONSIBILITIES

### CHAPTER I — Ongoing Proof

#### SECTION I — Continuous Validation

**Article 7.2.1.1.** Continuous validation is the obligation to re-execute relevant authorities as interaction surfaces evolve, not only at initial feature introduction.

**Article 7.2.1.2.** Regression testing responsibilities shall be proportional to touched domains and safety invariants under Part II proportionality principles.

**Article 7.2.1.3.** Refactors, dependency updates, and integration upgrades re-open regression proof for affected interaction paths.

#### SECTION II — Historical Traceability

**Article 7.2.1.4.** Historical traceability requires that prior Validation Reports, evidence identifiers, and outcomes remain discoverable sufficient to compare current and past compliance states.

**Article 7.2.1.5.** Regression analysis shall reference prior official outcomes when determining whether a failure is new or regressed.

**Article 7.2.1.6.** Loss of historical validation records shall be treated as governance risk and remedied without assuming past compliance.

---

## TITLE III — REGRESSION REPORTING

### CHAPTER I — Official Regression Findings

#### SECTION I — Reporting Distinction

**Article 7.3.1.1.** Validation Reports shall distinguish new failures from regression failures where determinable.

**Article 7.3.1.2.** Regression findings shall cite the prior pass reference or evidence identifier when available.

**Article 7.3.1.3.** Regression FAIL in mandatory authorities blocks release until remediated or governing law is amended.

#### SECTION II — Trend Governance

**Article 7.3.1.4.** Repeated PASS WITH OBSERVATIONS in the same domain shall trigger governance review for latent regression or criteria drift.

**Article 7.3.1.5.** Criteria drift—where validators become lenient without charter amendment—is a validation governance failure.

**Article 7.3.1.6.** Trend review shall not reduce safety invariant criteria to improve pass rates.

---

# PART VIII — FUTURE VALIDATION

## TITLE I — FUTURE VALIDATOR GOVERNANCE

### CHAPTER I — Subordination of Innovation

#### SECTION I — Binding Future Proof

**Article 8.1.1.1.** Future validators—including AI validators, eye-tracking validators, hardware validators, multilingual validators, cloud validation, and remote validation—must remain subordinate to this Charter, the LIEC, and the LIC.

**Article 8.1.1.2.** Future validators shall not invent criteria that substitute for governing law or bypass mandatory authorities.

**Article 8.1.1.3.** Registration under Part III Title XI is required before future validators govern release.

---

## TITLE II — AI VALIDATORS

### CHAPTER I — Governed Automation

#### SECTION I — Requirements

**Article 8.2.1.1.** AI validators shall produce evidence and reasoning auditable by humans. Probabilistic scores alone are insufficient for mandatory PASS without structured supporting evidence.

**Article 8.2.1.2.** AI validators examining communication or context shall respect privacy minima of Part IV and shall not retain user message content beyond verification need.

**Article 8.2.1.3.** AI validators shall not usurp User Sovereignty by auto-clearing releases that failed human-meaningful safety criteria.

---

## TITLE III — EYE-TRACKING AND HARDWARE VALIDATORS

### CHAPTER I — Physical Input Proof

#### SECTION I — Requirements

**Article 8.3.1.1.** Eye-tracking validators shall verify accuracy, feedback, and settings behavior within safe bounds without reducing emergency, recovery, or labelling guarantees.

**Article 8.3.1.2.** Hardware validators shall verify translation of physical input to constitutional gesture identifiers and navigation commands without device-layer redefinition of global gestures.

**Article 8.3.1.3.** Hardware and eye-tracking proof shall declare device or sensor preconditions so NOT APPLICABLE is justified where appropriate.

---

## TITLE IV — MULTILINGUAL VALIDATORS

### CHAPTER I — Language Expansion Proof

#### SECTION I — Requirements

**Article 8.4.1.1.** Multilingual validators shall verify that added languages preserve navigation reachability, emergency access, context binding, and first-person communication requirements independently per language.

**Article 8.4.1.2.** Multilingual validators shall detect parallel interaction schemes that bypass constitutional law in any language.

**Article 8.4.1.3.** Language-specific FAIL shall block release for that language's user-facing paths until remediated.

---

## TITLE V — CLOUD AND REMOTE VALIDATION

### CHAPTER I — Distributed Proof

#### SECTION I — Requirements

**Article 8.5.1.1.** Cloud validation and remote validation shall satisfy the same evidence, traceability, independence, and outcome requirements as local validation.

**Article 8.5.1.2.** Remote execution shall declare environment fidelity limits. BLOCKED shall be issued where fidelity is insufficient for the mandatory criteria under test.

**Article 8.5.1.3.** Network, hosting, or geography shall not become excuses for skipping mandatory authorities in user-facing paths.

---

# PART IX — AMENDMENTS

## TITLE I — AMENDMENT PROCEDURE

### CHAPTER I — Changing Validation Law

#### SECTION I — Explicit Amendment

**Article 9.1.1.1.** Amendments to this Charter must be explicit. Changes to validation governance require documented amendment to this instrument or a duly adopted successor proclaimed as such.

**Article 9.1.1.2.** No team, tool, or release process may bypass this Charter for convenience. Expediency is not amendment.

**Article 9.1.1.3.** Amendments shall state what prior Article they modify or supersede.

#### SECTION II — Superior Law Sync

**Article 9.1.1.4.** Upon LIC or LIEC amendment, this Charter shall be reviewed and revised as needed before amended interaction or engineering law governs production validation.

**Article 9.1.1.5.** Validation Authorities shall be updated to reflect charter changes before their amended criteria govern release.

**Article 9.1.1.6.** Implementations shall not be cleared under amended law by pre-amendment validation reports.

---

## TITLE II — PROPAGATION RULES

### CHAPTER I — Order of Effect

#### SECTION I — Propagation Sequence

**Article 9.2.1.1.** Amendment propagation shall follow order: LIC, then LIEC, then LVC, then Validation Authorities, then updated Validation Reports, then release consideration.

**Article 9.2.1.2.** Skipping a layer in propagation voids reliance on outcomes issued under stale law.

**Article 9.2.1.3.** Users shall be informed when amendments materially change validation-visible behavior affecting gestures, navigation, emergency, or recovery, as required by superior law.

#### SECTION II — Validator Updates

**Article 9.2.1.4.** Validator updates shall preserve historical traceability. Changes to criteria shall be versioned or dated in governance records.

**Article 9.2.1.5.** Validator updates shall not silently loosen mandatory safety criteria without charter amendment.

**Article 9.2.1.6.** New mandatory authorities require charter amendment or explicit registration under Part III Title XI with mandatory status declared.

---

## TITLE III — COMPATIBILITY REQUIREMENTS

### CHAPTER I — Coherence Across Documents

#### SECTION I — Compatibility Standard

**Article 9.3.1.1.** Amendments shall maintain compatibility with the authority hierarchy. This Charter shall never claim supremacy over the LIC or LIEC.

**Article 9.3.1.2.** Compatibility requirements prohibit validation rules that contradict constitutional Principles, Founding Purpose, or safety invariants unless the LIC has been duly amended.

**Article 9.3.1.3.** Successor instruments shall declare succession explicitly and preserve governance continuity for historical reports where possible.

#### SECTION II — Transitional Proof

**Article 9.3.1.4.** During transition periods, Validation Reports shall declare which law version they prove against.

**Article 9.3.1.5.** Mixed-version proof shall not clear release unless mandatory authorities under the new law have passed.

**Article 9.3.1.6.** Transitional shortcuts are prohibited for Emergency Authority and Navigation Reachability Authority.

---

## VALIDATION HIERARCHY

```
LIC
(LISA Interaction Constitution — supreme interaction law)
        ↓
LIEC
(LISA Interaction Engineering Charter — deterministic engineering requirements)
        ↓
LVC
(LISA Validation Charter — validation governance)
        ↓
Validation Authorities
(Guided Navigation, Gesture Conflict, Accessibility, Communication,
 Emergency, Navigation Reachability, Recovery, Context, Human Touch Parity,
 and future registered authorities)
        ↓
Validation Reports
(Official evidence-driven records of outcomes)
        ↓
Implementation
(Running systems — lowest authority)
```

---

## SUMMARY OF GOVERNANCE RESPONSIBILITIES

The LISA Validation Charter introduces the following governance responsibilities:

**Authority and limits.** Validation proves compliance; it does not create constitutional or engineering law. All validators are subordinate to the LIC, LIEC, and LVC.

**Validation principles.** Evidence over opinion, determinism, repeatability, traceability, independence, completeness, transparency, proportionality, and disciplined false-positive and false-negative handling govern every proof.

**Validation Authorities.** Nine current authorities plus a registration framework for future authorities, each with declared verification scope, reporting obligations, and mandatory-or-conditional status.

**Evidence standards.** Every official finding requires traceable evidence, reasoning, root cause where known, and remediation guidance tied to affected constitutional and engineering Articles.

**Official outcomes.** PASS, PASS WITH OBSERVATIONS, FAIL, BLOCKED, and NOT APPLICABLE have defined meanings that govern release decisions through Validation Reports.

**Release authority.** Mandatory authorities—always including Emergency and Navigation Reachability for user-facing changes—block release on FAIL or BLOCKED; readiness requires complete mandatory proof.

**Regression authority.** Validated guarantees may not silently regress; continuous validation and historical traceability are required.

**Future validation.** AI, eye-tracking, hardware, multilingual, cloud, and remote validators must register and remain subordinate to this Charter.

**Amendments.** Changes propagate through the hierarchy in order; validator criteria updates must preserve traceability and may not silently weaken safety proof.

---

*End of LISA Validation Charter, Draft Version 1.*
