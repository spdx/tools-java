/**
 * SPDX-FileContributor: Gary O'Neall
 * SPDX-FileCopyrightText: Copyright (c) 2025 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 * <br/>
 * Full example of an SPDX document using all classes
 */

package org.spdx.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import org.spdx.core.DefaultModelStore;
import org.spdx.core.IModelCopyManager;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v3_0_1.SpdxModelClassFactoryV3;
import org.spdx.library.model.v3_0_1.ai.AIPackage;
import org.spdx.library.model.v3_0_1.ai.EnergyUnitType;
import org.spdx.library.model.v3_0_1.ai.SafetyRiskAssessmentType;
import org.spdx.library.model.v3_0_1.build.Build;
import org.spdx.library.model.v3_0_1.core.*;
import org.spdx.library.model.v3_0_1.dataset.ConfidentialityLevelType;
import org.spdx.library.model.v3_0_1.dataset.DatasetAvailabilityType;
import org.spdx.library.model.v3_0_1.dataset.DatasetPackage;
import org.spdx.library.model.v3_0_1.dataset.DatasetType;
import org.spdx.library.model.v3_0_1.expandedlicensing.ExtendableLicense;
import org.spdx.library.model.v3_0_1.security.*;
import org.spdx.library.model.v3_0_1.simplelicensing.AnyLicenseInfo;
import org.spdx.library.model.v3_0_1.simplelicensing.SimpleLicensingText;
import org.spdx.library.model.v3_0_1.software.*;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.Verify;
import org.spdx.v3jsonldstore.JsonLDStore;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.spdx.tools.Verify.JSON_SCHEMA_RESOURCE_V3;


/**
 * This class attempts to implement all the SPDX specification classes and most of the properties.
 * <p>
 * It will generate a resulting serialization that can be used as a full serialization example.
 * </p>
 * <p>
 * This example is current as of the version 3.0.1 of the SPDX Specification
 * </p>
 */
public class FullSpdxV3Example {

    static final DateTimeFormatter SPDX_DATE_FORMATTER = DateTimeFormatter.ofPattern(SpdxConstantsCompatV2.SPDX_DATE_FORMAT);
    static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static class ExampleBuilder {
        private final String prefix;
        private final SpdxDocument doc;
        private Sbom sBom = null;
        private SpdxPackage pkg = null;

        public ExampleBuilder(String prefix, SpdxDocument doc) {
            this.prefix = prefix;
            this.doc = doc;
        }

        void build() throws InvalidSPDXAnalysisException {
            addCoreClasses();
            addSoftwareClasses();
            addAIandDataClasses();
            addSecurityClasses();
            addSimpleLicensingClasses();
            addExpandedLicensingClasses();
            addBuildClasses();
            addExtensionClasses();
        }

        private String getNextAnonId() throws InvalidSPDXAnalysisException {
            return doc.getModelStore().getNextId(IModelStore.IdType.Anonymous);
        }

        private void addExtensionClasses() throws InvalidSPDXAnalysisException {
            //TODO: The following is causing a schema validation error - uncomment when resolved
//            ModelRegistry.getModelRegistry().registerExtensionType("Extension.example",
//                    SpdxExtensionExample.class);
//            SpdxExtensionExample extension = new SpdxExtensionExample(doc.getModelStore(),
//                    prefix + "extension", doc.getCopyManager(), true, prefix);
//            extension.setExtensionProperty("Extension property value");
//            doc.getExtensions().add(extension);
            doc.getExtensions().add(doc.createCdxPropertiesExtension(getNextAnonId())
                    .addCdxProperty(doc.createCdxPropertyEntry(getNextAnonId())
                            .setCdxPropName("CDXProperty")
                            .setCdxPropValue("Property Value")
                            .build())
                    .build());
        }

        private void addBuildClasses() throws InvalidSPDXAnalysisException {
            Build build = doc.createBuild(prefix + "build")
                    .setBuildType("https://github.com/spdx/tools-java/blob/master/pom.xml")
                    .setComment("Builds use the maven-release-plugin")
                    .setBuildStartTime(LocalDateTime.of(2025, 10, 15, 11, 42)
                            .format(SPDX_DATE_FORMATTER))
                    .setBuildEndTime(LocalDateTime.of(2025, 10, 15, 11, 50)
                            .format(SPDX_DATE_FORMATTER))
                    .addConfigSourceDigest(doc.createHash(getNextAnonId())
                            .setAlgorithm(HashAlgorithm.SHA256)
                            .setHashValue("cc75cc9bfad1fb047f15fd60fe48806a9614c17bfee073e79e5ac3bd3e5d5271 ")
                            .build())
                    .addConfigSourceEntrypoint("release")
                    .addConfigSourceUri("https://repo1.maven.org/maven2/org/spdx/tools-java/2.0.2/tools-java-2.0.2.pom")
                    .addEnvironment(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("OS")
                            .setValue("Windows11")
                            .build())
                    .addParameter(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("Next Snapshot Version")
                            .setValue("2.0.3-SNAPSHOT")
                            .build())
                    .build();

            // hasInput relationship
            SpdxFile pomFile = doc.createSpdxFile(prefix + "pomfile")
                    .setName("pom.xml")
                    .setFileKind(FileKindType.FILE)
                    .addVerifiedUsing(doc.createHash(getNextAnonId())
                            .setAlgorithm(HashAlgorithm.SHA256)
                            .setHashValue("cc75cc9bfad1fb047f15fd60fe48806a9614c17bfee073e79e5ac3bd3e5d5271")
                            .build())
                    .build();
            doc.getElements().add(pomFile);
            sBom.getElements().add(pomFile);
            SpdxFile srcDir = doc.createSpdxFile(prefix + "src")
                    .setName("src")
                    .setFileKind(FileKindType.DIRECTORY)
                    .build();
            doc.getElements().add(srcDir);
            sBom.getElements().add(srcDir);
            Relationship hasInput = doc.createLifecycleScopedRelationship(prefix + "hasinput")
                    .setRelationshipType(RelationshipType.HAS_INPUT)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .setScope(LifecycleScopeType.BUILD)
                    .setFrom(build)
                    .addTo(srcDir)
                    .addTo(pomFile)
                    .build();
            doc.getElements().add(hasInput);
            SpdxFile jarWithDependencies = doc.createSpdxFile(prefix + "jarwdeps")
                    .setName("tools-java-2.0.2-jar-with-dependencies.jar")
                    .setFileKind(FileKindType.FILE)
                    .addVerifiedUsing(doc.createHash(getNextAnonId())
                            .setAlgorithm(HashAlgorithm.SHA256)
                            .setHashValue("3b326e4ea0e901d71a58627ca14c7d7ec36fc7bdb01308a78de99de2171c7904")
                            .build())
                    .build();
            doc.getElements().add(jarWithDependencies);
            Relationship hasOutput = doc.createRelationship(prefix + "hasoutput")
                    .setRelationshipType(RelationshipType.HAS_OUTPUT)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .setFrom(build)
                    .addTo(jarWithDependencies)
                    .build();
            doc.getElements().add(hasOutput);
        }

        private  void addExpandedLicensingClasses() throws InvalidSPDXAnalysisException {
            // ConjunctiveLicenseSet
            AnyLicenseInfo complexLicense = doc.createConjunctiveLicenseSet(prefix + "complexlicense")
                    // CustomLicense
                    .addMember(doc.createCustomLicense(prefix + "LicenseRef-customlicense3")
                            .setLicenseText("This is the license text for my custom license")
                            .setName("Gary's Custom License")
                            .addSeeAlso("https://example.com")
                            .build())
                    // OrLaterOperator
                    .addMember(doc.createOrLaterOperator(prefix + "complexorlater")
                            // ListedLicense
                            .setSubjectLicense(doc.createListedLicense("http://spdx.org/licenses/EPL-1.0")
                                    .setName("Eclipse Public License 1.0")
                                    .setLicenseText("Eclipse Public License - v 1.0\n\nTHE ACCOMPANYING PROGRAM IS PROVIDED" +
                                            " UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE (\"AGREEMENT\"). ANY USE, REPRODUCTION " +
                                            "OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENTS ACCEPTANCE OF THIS AGREEMENT.\n\n1. " +
                                            "DEFINITIONS\n\n\"Contribution\" means:\n     a) in the case of the initial Contributor...")
                                    .setIsFsfLibre(true)
                                    .setComment("EPL replaced the CPL on 28 June 2005.")
                                    .addSeeAlso("https://opensource.org/licenses/EPL-1.0")
                                    .build())
                            .build())
                    // DisjunctiveLicenseSet
                    .addMember(doc.createDisjunctiveLicenseSet(prefix + "complexdisjunctive")
                            // WithAdditionOperator
                            .addMember(doc.createWithAdditionOperator(prefix + "complexwith")
                                        .setSubjectExtendableLicense((ExtendableLicense) LicenseInfoFactory.parseSPDXLicenseString("GPL-2.0-or-later"))
                                    // ListedLicenseException
                                    .setSubjectAddition(doc.createListedLicenseException("http://spdx.org/licenses/Autoconf-exception-2.0")
                                            .setName("Autoconf exception 2.0")
                                            .setComment("Typically used with GPL-2.0-only or GPL-2.0-or-later")
                                            .setAdditionText("As a special exception, the Free Software Foundation gives unlimited " +
                                                    "permission to copy, distribute and modify the ...")
                                            .addSeeAlso("http://ftp.gnu.org/gnu/autoconf/autoconf-2.59.tar.gz")
                                            .build())
                                    .build())
                        .addMember(doc.createWithAdditionOperator(prefix + "complexwithcustomaddition")
                                .setSubjectExtendableLicense((ExtendableLicense) LicenseInfoFactory.parseSPDXLicenseString("Apache-2.0"))
                                // CustomLicenseAddition
                                .setSubjectAddition(doc.createCustomLicenseAddition(prefix + "complexcustomaddition")
                                        .setName("My License Addition")
                                        .setAdditionText("Custom addition text - just for me")
                                        .addSeeAlso("https://example.com")
                                        .build())
                                .build())
                    // ExtendableLicense - Abstract
                    // IndividualLicensingInfo - used by listed license
                    // License - Abstract
                            .addMember(LicenseInfoFactory.parseSPDXLicenseString("MIT"))
                            .build())
                    .build();
            doc.getElements().add(complexLicense);
        }

        private void addSimpleLicensingClasses() throws InvalidSPDXAnalysisException {
            // SimpleLicensingText
            String simpleLicenseId = "LicenseRef-simpletext";
            String simpleAdditionId = "LicenseRef-simpleaddition";
            SimpleLicensingText slt = doc.createSimpleLicensingText(prefix + simpleLicenseId)
                    .setLicenseText("This is the license text to go with my license expression")
                    .build();
            doc.getElements().add(slt);
            SimpleLicensingText simpleaddition = doc.createSimpleLicensingText(prefix + simpleAdditionId)
                    .setLicenseText("This is the custom addition text")
                    .build();
            doc.getElements().add(simpleaddition);
            // LicenseExpression
            doc.getElements().add(doc.createLicenseExpression(prefix + "licenseexpression")
                            .setLicenseExpression("Apache-2.0 AND " + simpleLicenseId + " WITH " + simpleAdditionId)
                            .addCustomIdToUri(doc.createDictionaryEntry(getNextAnonId())
                                    .setKey(simpleLicenseId)
                                    .setValue(prefix + simpleLicenseId)
                                    .build())
                            .addCustomIdToUri(doc.createDictionaryEntry(getNextAnonId())
                                    .setKey(simpleAdditionId)
                                    .setValue(prefix + simpleAdditionId)
                                    .build())
                    .build());
            // AnyLicenseInfo - Abstract
        }

        private void addSecurityClasses() throws InvalidSPDXAnalysisException {
            // First - let's add a dependeny with a known vulnerability
            SpdxPackage log4j = doc.createSpdxPackage(prefix + "log4j")
                    .setName("Apache Log4j 2")
                    .setPackageVersion("2.14.1")
                    .setPackageUrl("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1")
                    .addExternalIdentifier(doc.createExternalIdentifier(getNextAnonId())
                            .setExternalIdentifierType(ExternalIdentifierType.CPE23)
                            .setIssuingAuthority("NVD")
                            .setIdentifier("cpe:2.3:a:apache:log4j:2.14.1:-:*:*:*:*:*:*")
                            .build())
                    .build();
            doc.getElements().add(log4j);
            sBom.getElements().add(log4j);
            Relationship depRelationship = doc.createRelationship(prefix + "log4jdep")
                    .setFrom(pkg)
                    .addTo(log4j)
                    .setRelationshipType(RelationshipType.HAS_DYNAMIC_LINK)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .build();
            doc.getElements().add(depRelationship);
            sBom.getElements().add(depRelationship);
            // Since we don't want the vulnerabilities to be in the more static SBOMs, let's create a different collection
            Bundle securityBundle = doc.createBundle(prefix + "securitybundle")
                    .setContext("Security information related to "+sBom.getObjectUri())
                    .build();
            // Vulnerability
            Vulnerability vuln = doc.createVulnerability(prefix + "log4jvuln")
                    .setSummary("Apache Log4j2 versions 2.0-alpha1 through 2.16.0 did not protect from uncontrolled recursion from self-referential lookups.")
                    .setDescription("Apache Log4j2 versions 2.0-alpha1 through 2.16.0 (excluding 2.12.3 and 2.3.1) did not " +
                            "protect from uncontrolled recursion from self-referential lookups. This allows an attacker " +
                            "with control over ...")
                    .setPublishedTime(LocalDateTime.of(2021, 12, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .addExternalIdentifier(doc.createExternalIdentifier(getNextAnonId())
                            .setExternalIdentifierType(ExternalIdentifierType.CVE)
                            .setIdentifier("CVE-2021-45105")
                            .addIdentifierLocator("https://www.cve.org/CVERecord?id=CVE-2021-45105")
                            .build())
                    .addExternalRef(doc.createExternalRef(getNextAnonId())
                            .setExternalRefType(ExternalRefType.SECURITY_ADVISORY)
                            .addLocator("https://nvd.nist.gov/vuln/detail/CVE-2021-45105")
                            .build())
                    .build();
            doc.getElements().add(vuln);
            securityBundle.getElements().add(vuln);
            Relationship log4jVulnRel = doc.createRelationship(prefix + "log4jvulnrelationship")
                    .setRelationshipType(RelationshipType.HAS_ASSOCIATED_VULNERABILITY)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .setFrom(log4j)
                    .addTo(vuln)
                    .build();
            doc.getElements().add(log4jVulnRel);
            securityBundle.getElements().add(log4jVulnRel);
            Relationship pkgVulnRel = doc.createRelationship(prefix + "pkgvulnrelationship")
                    .setRelationshipType(RelationshipType.HAS_ASSOCIATED_VULNERABILITY)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .setFrom(pkg)
                    .addTo(vuln)
                    .build();
            doc.getElements().add(pkgVulnRel);
            securityBundle.getElements().add(pkgVulnRel);
            // CvssV2VulnAssessmentRelationship
            Agent supplierAgent = doc.createAgent(prefix + "assessmentagent")
                    .setName("Supplier of Assessments")
                    .setComment("This would be the supplier of the vulnerability assessments")
                    .build();
            CvssV2VulnAssessmentRelationship cvssV2 = doc.createCvssV2VulnAssessmentRelationship(prefix + "cvssv2vuln")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setScore(5.0)
                    .setVectorString("(AV:N/AC:M/Au:N/C:P/I:N/A:N)")
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(cvssV2);
            securityBundle.getElements().add(cvssV2);
            // CvssV3VulnAssessmentRelationship
            CvssV3VulnAssessmentRelationship cvssV3 = doc.createCvssV3VulnAssessmentRelationship(prefix + "cvssv3vuln")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setScore(5.0)
                    .setSeverity(CvssSeverityType.CRITICAL)
                    .setVectorString("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H")
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(cvssV3);
            securityBundle.getElements().add(cvssV3);
            // CvssV4VulnAssessmentRelationship
            CvssV4VulnAssessmentRelationship cvssV4 = doc.createCvssV4VulnAssessmentRelationship(prefix + "cvssv4vuln")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setScore(5.0)
                    .setSeverity(CvssSeverityType.CRITICAL)
                    .setVectorString("(AV:N/AC:M/Au:N/C:P/I:N/A:N)")
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(cvssV4);
            securityBundle.getElements().add(cvssV4);
            // EpssVulnAssessmentRelationship
            EpssVulnAssessmentRelationship epss = doc.createEpssVulnAssessmentRelationship(prefix + "epss")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setProbability(0.01)
                    .setPercentile(0.4)
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(epss);
            securityBundle.getElements().add(epss);
            // ExploitCatalogVulnAssessmentRelationship
            //TODO: The schema has "locator" for the field while the generated Java code has "securityLocator"
            //Need to regenerate the library then uncomment the example below
            ExploitCatalogVulnAssessmentRelationship excat = doc.createExploitCatalogVulnAssessmentRelationship(prefix + "exploitcat")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setCatalogType(ExploitCatalogType.KEV)
                    .setSecurityLocator("https://www.cisa.gov/known-exploited-vulnerabilities-catalog")
                    .setExploited(true)
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(excat);
            securityBundle.getElements().add(excat);

            // SsvcVulnAssessmentRelationship
            SsvcVulnAssessmentRelationship ssvs = doc.createSsvcVulnAssessmentRelationship(prefix + "ssvs")
                    .setRelationshipType(RelationshipType.HAS_ASSESSMENT_FOR)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setDecisionType(SsvcDecisionType.ACT)
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(ssvs);
            securityBundle.getElements().add(ssvs);
            // VexAffectedVulnAssessmentRelationship
            VexAffectedVulnAssessmentRelationship vexAffected = doc.createVexAffectedVulnAssessmentRelationship(prefix + "vexaffected")
                    .setRelationshipType(RelationshipType.AFFECTS)
                    .setFrom(vuln)
                    .addTo(log4j)
                    .setActionStatement("Upgrade to version 2.20 or later")
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(vexAffected);
            securityBundle.getElements().add(vexAffected);
            // VexFixedVulnAssessmentRelationship
            VexFixedVulnAssessmentRelationship vexFixed = doc.createVexFixedVulnAssessmentRelationship(prefix + "vexfixed")
                    .setRelationshipType(RelationshipType.AFFECTS)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(vexFixed);
            securityBundle.getElements().add(vexFixed);
            // VexNotAffectedVulnAssessmentRelationship
            VexNotAffectedVulnAssessmentRelationship vexNotAffected = doc.createVexNotAffectedVulnAssessmentRelationship(prefix + "vexnotaffected")
                    .setRelationshipType(RelationshipType.AFFECTS)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .setJustificationType(VexJustificationType.INLINE_MITIGATIONS_ALREADY_EXIST)
                    .setImpactStatement("No longer using this vulnerable part of this library.")
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(vexNotAffected);
            securityBundle.getElements().add(vexNotAffected);
            // VexUnderInvestigationVulnAssessmentRelationship
            VexUnderInvestigationVulnAssessmentRelationship vexUnderInvestigation = doc.createVexUnderInvestigationVulnAssessmentRelationship(prefix + "vexunderinvestigation")
                    .setRelationshipType(RelationshipType.AFFECTS)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .setAssessedElement(log4j)
                    .setSuppliedBy(supplierAgent)
                    .setPublishedTime(LocalDateTime.of(2023, 9, 18, 0, 0)
                            .format(SPDX_DATE_FORMATTER))
                    .build();
            doc.getElements().add(vexUnderInvestigation);
            securityBundle.getElements().add(vexUnderInvestigation);
            // VexVulnAssessmentRelationship - Abstract
            // VulnAssessmentRelationship - Abstract
        }

        private void addCoreClasses() throws InvalidSPDXAnalysisException {
            // Agent - Abstract, already in creation info
            // Annotation
            doc.getElements().add(doc.createAnnotation(prefix + "docannotation")
                    .setStatement("This document is for example purposes only")
                    .setAnnotationType(AnnotationType.OTHER)
                    .setSubject(doc)
                    .build());
            // Artifact - Abstract - used in software package and several others
            // Bom - will be used as an AI BOM and software BOM
            // Bundle
            doc.getElements().add(doc.createBundle(prefix + "bundle")
                    .setComment("This is just an example of a concrete Bundle class - the elements are not used elsewhere in the SPDX document")
                    .setContext("Custom Licenses")
                    .addElement(doc.createCustomLicense(prefix + "LicenseRef-CustomLicense1")
                            .setLicenseText("This is a custom license text number one.")
                            .build())
                    .addElement(doc.createCustomLicense(prefix + "LicenseRef-CustomLicense2")
                            .setLicenseText("This is a custom license text number two.")
                            .build())
                    .build());
            // CreationInfo - Already created
            // DictionaryEntry - Used in several places including SimpleLicensing
            // Element - Abstract
            // ElementCollection - Abstract
            // ExternalIdentifier - Used in Security profile
            // Organization
            doc.getCreationInfo().getCreatedBys().add(doc.createOrganization(prefix + "spdxorg")
                    .setName("System Package Data Exchange (SPDX)")
                    .build());
            // ExternalMap
            String orgLocation = "https://external/organization/spdxdata";
            String orgPrefix = orgLocation + "#";
            String orgUri = orgPrefix + "org";
            ExternalOrganization externalOrg = new ExternalOrganization(doc.getModelStore(),
                    orgUri, doc.getCopyManager(),
                    true, orgLocation);
            doc.getCreationInfo().getCreatedBys().add(externalOrg);
            doc.getSpdxImports().add(doc.createExternalMap(getNextAnonId())
                    .setExternalSpdxId(orgUri)
                    .setLocationHint(orgLocation)
                    .build());
            // Hash - Used in file
            // IndividualElement - Used in software package originated by
            // IntegrityMethod - Used in file and package
            // LifecycleScopedRelationship
            // NamespaceMap - Used in doc already
            // PackageVerificationCode - Going to ignore - deprecated
            // Person - Used in creation info
            // PositiveIntegerRange - Used in snippets
            // Relationship - Used in software
            // SoftwareAgent
            doc.getCreationInfo().getCreatedBys().add(doc.createSoftwareAgent(prefix + "softwareagent")
                    .setName("SPDX Spec Github CI")
                    .build());
            // SpdxDocument - already used
            // ExternalRef
            // Tool
            doc.getCreationInfo().getCreatedUsings().add(doc.createTool(prefix + "creationtool")
                    .setName("tools-java")
                    .setComment("Created by the FullSpdxV3Example.java utility in tools-java")
                    .addExternalRef(doc.createExternalRef(getNextAnonId())
                            .setExternalRefType(ExternalRefType.MAVEN_CENTRAL)
                            .addLocator("org.spdx:tools-java")
                            .build())
                    .build());
        }

        private void addSoftwareClasses() throws InvalidSPDXAnalysisException {
            // Sbom
            sBom = doc.createSbom(prefix + "sbom")
                    .setName("AI SBOM")
                    .addSbomType(SbomType.ANALYZED)
                    .addProfileConformance(ProfileIdentifierType.CORE)
                    .addProfileConformance(ProfileIdentifierType.SOFTWARE)
                    .addProfileConformance(ProfileIdentifierType.BUILD)
                    .addProfileConformance(ProfileIdentifierType.SECURITY)
                    .addProfileConformance(ProfileIdentifierType.EXPANDED_LICENSING)
                    .build();
            doc.getElements().add(sBom);
            doc.getRootElements().add(sBom);
            // Package
            pkg = doc.createSpdxPackage(prefix + "tools-java")
                    .setName("tools-java")
                    .setPrimaryPurpose(SoftwarePurpose.APPLICATION)
                    .addAdditionalPurpose(SoftwarePurpose.LIBRARY)
                    .addAttributionText("Maintained by the SPDX Community")
                    .setBuiltTime(LocalDateTime.of(2025, 10, 15, 9, 10)
                            .format(SPDX_DATE_FORMATTER))
                    // ContentIdentifier
                    .addContentIdentifier(doc.createContentIdentifier(getNextAnonId())
                            .setContentIdentifierType(ContentIdentifierType.GITOID)
                            .setContentIdentifierValue("23bd470259f55641eb72b0c5d733edac014a4554")
                            .build())
                    .setCopyrightText("Copyright (c) Source Auditor Inc.")
                    .setDescription("A command-line utility for creating, converting, comparing, and validating SPDX documents across multiple formats.")
                    .setDownloadLocation("https://github.com/spdx/tools-java/releases/download/v2.0.2/tools-java-2.0.2.zip")
                    .addExternalIdentifier(doc.createExternalIdentifier(getNextAnonId())
                            .setExternalIdentifierType(ExternalIdentifierType.URL_SCHEME)
                            .setIdentifier("https://github.com/spdx/tools-java")
                            .setIssuingAuthority("GitHub")
                            .build())
                    .addExternalRef(doc.createExternalRef(getNextAnonId())
                            .setExternalRefType(ExternalRefType.MAVEN_CENTRAL)
                            .addLocator("org.spdx:tools-java:jar:2.0.2")
                            .build())
                    .setPackageUrl("pkg:maven/org.spdx/tools-java@2.0.2")
                    .setPackageVersion("2.0.2")
                    .setReleaseTime(LocalDateTime.of(2025, 10, 15, 11, 50)
                            .format(SPDX_DATE_FORMATTER))
                    .setSourceInfo("This package came from the original source - the official SPDX GitHub repo and build process")
                    .addStandardName("SPDX Version 2.X and SPDX Version 3.0")
                    .setHomePage("https://github.com/spdx/tools-java")
                    .addOriginatedBy(new SpdxOrganization())
                    .setSuppliedBy(new SpdxOrganization())
                    .setSummary("A command-line utility for creating, converting, comparing, and validating SPDX documents across multiple formats.")
                    .addSupportLevel(SupportType.LIMITED_SUPPORT)
                    .setValidUntilTime(LocalDateTime.of(2027, 10, 15, 9, 10)
                            .format(SPDX_DATE_FORMATTER))
                    .addVerifiedUsing(doc.createHash(getNextAnonId())
                            .setAlgorithm(HashAlgorithm.SHA256)
                            .setHashValue("c37ce759c3867780d55791a1804101d288fa921e77ed791e6c053fd5d7513d0d")
                            .build())
                    .build();
            doc.getElements().add(pkg);
            sBom.getElements().add(pkg);
            sBom.getRootElements().add(pkg);
            // File
            SpdxFile sourceFile = doc.createSpdxFile(prefix + "example-source")
                    .setPrimaryPurpose(SoftwarePurpose.SOURCE)
                    .setContentType("text/plain")
                    .setCopyrightText("Copyright (c) 2025 Source Auditor Inc.")
                    .setFileKind(FileKindType.FILE)
                    .setName("./examples/org/spdx/examples/FullSpdxV3Example.java")
                    .build();
            sBom.getElements().add(sourceFile);
            doc.getElements().add(sourceFile);
            // Relationships - declared license, concluded license, generated from
            doc.getElements().add(doc.createRelationship(prefix + "example-source-to-pkg")
                    .setRelationshipType(RelationshipType.GENERATES)
                    .setFrom(sourceFile)
                    .addTo(pkg)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .build());
            AnyLicenseInfo declared = LicenseInfoFactory.parseSPDXLicenseString("Apache-2.0",
                    doc.getModelStore(), prefix, doc.getCopyManager(), new ArrayList<>());
            AnyLicenseInfo concluded = LicenseInfoFactory.parseSPDXLicenseString("Apache-2.0",
                    doc.getModelStore(), prefix, doc.getCopyManager(), new ArrayList<>());
            doc.getElements().add(doc.createRelationship(prefix + "source-declared")
                    .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                    .setFrom(sourceFile)
                    .addTo(declared)
                    .setCompleteness(RelationshipCompleteness.NO_ASSERTION)
                    .build());
            doc.getElements().add(doc.createRelationship(prefix + "source-concluded")
                    .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                    .setFrom(sourceFile)
                    .addTo(concluded)
                    .setCompleteness(RelationshipCompleteness.COMPLETE)
                    .build());
            doc.getElements().add(doc.createRelationship(prefix + "pkg-declared")
                    .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                    .setFrom(pkg)
                    .addTo(declared)
                    .setCompleteness(RelationshipCompleteness.NO_ASSERTION)
                    .build());
            doc.getElements().add(doc.createRelationship(prefix + "pkg-concluded")
                    .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                    .setFrom(pkg)
                    .addTo(concluded)
                    .setCompleteness(RelationshipCompleteness.COMPLETE)
                    .build());
            // Snippet
            Snippet snippet = doc.createSnippet(prefix + "snippet")
                    .addAttributionText("Example code created by Gary O'Neall")
                    .setDescription("Main method for the FullSpdxV3Example.java")
                    .setCopyrightText("Copyright (c) 2025 Source Auditor Inc.")
                    .setByteRange(doc.createPositiveIntegerRange(getNextAnonId())
                            .setBeginIntegerRange(43)
                            .setEndIntegerRange(89)
                            .build())
                    .setLineRange(doc.createPositiveIntegerRange(getNextAnonId())
                            .setBeginIntegerRange(1548)
                            .setEndIntegerRange(3955)
                            .build())
                    .setName("main(String[] args)")
                    .setSnippetFromFile(sourceFile)
                    .build();
            doc.getElements().add(snippet);
            sBom.getElements().add(snippet);
            doc.getElements().add(doc.createRelationship(prefix + "snippet-declared")
                    .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                    .setFrom(snippet)
                    .addTo(declared)
                    .setCompleteness(RelationshipCompleteness.COMPLETE)
                    .build());
            doc.getElements().add(doc.createRelationship(prefix + "snippet-concluded")
                    .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                    .setFrom(snippet)
                    .addTo(concluded)
                    .setCompleteness(RelationshipCompleteness.COMPLETE)
                    .build());
            // SoftwareArtifact - Abstract
        }

        private void addAIandDataClasses() throws InvalidSPDXAnalysisException {
            Bom aiBom = doc.createBom(prefix + "aibom")
                    .setName("AI SBOM")
                    .addProfileConformance(ProfileIdentifierType.CORE)
                    .addProfileConformance(ProfileIdentifierType.SOFTWARE)
                    .addProfileConformance(ProfileIdentifierType.AI)
                    .addProfileConformance(ProfileIdentifierType.DATASET)
                    .build();
            doc.getElements().add(aiBom);
            doc.getRootElements().add(aiBom);
            // DatasetPackage
            DatasetPackage dataset = doc.createDatasetPackage(prefix + "dataset")
                    .addAnonymizationMethodUsed("Perturbation")
                    .setConfidentialityLevel(ConfidentialityLevelType.GREEN)
                    .setDataCollectionProcess("WWW data under open licenses")
                    .setDataCollectionProcess("Crawler")
                    .addDataPreprocessing("Anonymization using perturbation of sensitive data")
                    .setDatasetAvailability(DatasetAvailabilityType.QUERY)
                    .setDatasetNoise("Includes data input by humans - subject to error")
                    .setDatasetSize(4000000)
                    .addDatasetType(DatasetType.TEXT)
                    .setDatasetUpdateMechanism("Automated crawler")
                    .setHasSensitivePersonalInformation(PresenceType.NO)
                    .setIntendedUse("LLM training")
                    .addKnownBias("Typical human bias representative from the global WWW")
                    .addSensor(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("crawler")
                            .setValue("webcrawler")
                            .build())
                    .setBuiltTime(LocalDateTime.of(2025, 10, 15, 11, 50)
                            .format(SPDX_DATE_FORMATTER))
                    .addOriginatedBy(doc.createOrganization(prefix + "dataorg")
                            .setName("Data Corp.")
                            .build())
                    .setReleaseTime(LocalDateTime.of(2025, 10, 22, 8, 50)
                            .format(SPDX_DATE_FORMATTER))
                    .setDownloadLocation("https://com.data-corp.data/mydata")
                    .setPrimaryPurpose(SoftwarePurpose.DATA)
                    .build();
            doc.getElements().add(dataset);
            aiBom.getElements().add(dataset);
            // AIPackage
            AIPackage aiPackage = doc.createAIPackage(prefix + "aipackage")
                    .setAutonomyType(PresenceType.YES)
                    .addDomain("Automotive")
                    // EnergyConsumption
                    .setEnergyConsumption(doc.createEnergyConsumption(getNextAnonId())
                            // EnergyConsumptionDescription
                            .addFinetuningEnergyConsumption(doc.createEnergyConsumptionDescription(getNextAnonId())
                                    .setEnergyQuantity(150.0)
                                    .setEnergyUnit(EnergyUnitType.KILOWATT_HOUR)
                                    .build())
                            .addInferenceEnergyConsumption(doc.createEnergyConsumptionDescription(getNextAnonId())
                                    .setEnergyQuantity(0.7)
                                    .setEnergyUnit(EnergyUnitType.KILOWATT_HOUR)
                                    .build())
                            .addTrainingEnergyConsumption(doc.createEnergyConsumptionDescription(getNextAnonId())
                                    .setEnergyQuantity(15000.3)
                                    .setEnergyUnit(EnergyUnitType.KILOWATT_HOUR)
                                    .build())
                            .build())
                    .addHyperparameter(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("Hidden layers")
                            .setValue("14")
                            .build())
                    .setInformationAboutApplication("Used in self driving cars")
                    .setInformationAboutTraining("Trained from data collected from auto cameras, sensors and WWW")
                    .setLimitation("Limited by amount of situations encountered from autos used for training")
                    .addMetric(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("Operator Interventions")
                            .setValue("432")
                            .build())
                    .addMetricDecisionThreshold(doc.createDictionaryEntry(getNextAnonId())
                            .setKey("Operator Interventions")
                            .setValue("100")
                            .build())
                    .addModelDataPreprocessing("1. data cleaning")
                    .addModelExplainability("Behaviors from the auto driving car when observed from a safety driver")
                    .setSafetyRiskAssessment(SafetyRiskAssessmentType.SERIOUS)
                    .addStandardCompliance("UL 4600")
                    .addTypeOfModel("LLM")
                    .setUseSensitivePersonalInformation(PresenceType.NO)
                    .build();
            doc.getElements().add(aiPackage);
            aiBom.getElements().add(aiPackage);
            Relationship usesData = doc.createRelationship(prefix + "usesdata")
                    .setRelationshipType(RelationshipType.TRAINED_ON)
                    .setFrom(dataset)
                    .addTo(aiPackage)
                    .setCompleteness(RelationshipCompleteness.INCOMPLETE)
                    .build();
            doc.getElements().add(usesData);
            aiBom.getElements().add(usesData);
        }
    }

    /**
     * @param args args[0] is the file path for the output serialized file
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            usage();
            System.exit(1);
        }
        File outFile = new File(args[0]);
        if (outFile.exists()) {
            System.out.printf("%s already exists.\n", args[0]);
            System.exit(1);
        }
        if (!outFile.createNewFile()) {
            System.out.printf("Unable to create file %s\n", args[0]);
            System.exit(1);
        }
        if (!outFile.canWrite()) {
            System.out.printf("Can not write to file %s\n", args[0]);
            System.exit(1);
        }
        SpdxModelFactory.init();
        IModelCopyManager copyManager = new ModelCopyManager();
        try (JsonLDStore modelStore = new JsonLDStore(new InMemSpdxStore())) {
            modelStore.setUseExternalListedElements(true);
            String defaultDocUri = "https://spdx.github.io/spdx-spec/v3.0.1/examples/full-example-eaa46bdcfa20";
            String prefix = defaultDocUri + "#";
            DefaultModelStore.initialize(modelStore, defaultDocUri, copyManager);
            CreationInfo creationInfo = SpdxModelClassFactoryV3.createCreationInfo(
                    modelStore, prefix + "garyagent", "Gary O'Neall",
                    copyManager);
            SpdxDocument doc = creationInfo.createSpdxDocument(prefix + "document")
                    .setDataLicense(LicenseInfoFactory.getListedLicenseById("CC0"))
                    .addNamespaceMap(creationInfo.createNamespaceMap(modelStore.getNextId(IModelStore.IdType.Anonymous))
                            .setNamespace(prefix)
                            .setPrefix("example")
                            .build())
                    .addProfileConformance(ProfileIdentifierType.CORE)
                    .addProfileConformance(ProfileIdentifierType.SOFTWARE)
                    .addProfileConformance(ProfileIdentifierType.BUILD)
                    .addProfileConformance(ProfileIdentifierType.AI)
                    .addProfileConformance(ProfileIdentifierType.DATASET)
                    .addProfileConformance(ProfileIdentifierType.SECURITY)
                    .addProfileConformance(ProfileIdentifierType.EXPANDED_LICENSING)
                    .build();
            doc.setIdPrefix(prefix);
            ExampleBuilder builder = new ExampleBuilder(prefix, doc);
            builder.build();
            List<String> warnings = new ArrayList<>();
            // Add all the elements to the doc to make sure everything gets serialized
            Collection<Element> docElements = doc.getElements();
            SpdxModelFactory.getSpdxObjects(modelStore, copyManager, null, null, prefix).forEach(
                    modelObject -> {
                        if (modelObject instanceof Element) {
                            Element element = (Element)modelObject;
                            if (!docElements.contains(element) && !element.equals(doc)) {
                                warnings.add("Element not in the document elements: " + element.getObjectUri());
                                docElements.add(element);
                            }
                        }
                    }
            );

            // Verify using the SPDX Java Library
            warnings.addAll(doc.verify());
            try (OutputStream outStream = new FileOutputStream(outFile)) {
                modelStore.serialize(outStream, doc);
            }

            // Validate using the schema
            JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
            JsonSchema schema;
            try (InputStream is = Verify.class.getResourceAsStream("/" + JSON_SCHEMA_RESOURCE_V3)) {
                schema = jsonSchemaFactory.getSchema(is);
            }
            JsonNode root;
            try (InputStream is = new FileInputStream(outFile)) {
                root = JSON_MAPPER.readTree(is);
            }
            Set<ValidationMessage> messages = schema.validate(root);
            for (ValidationMessage msg:messages) {
                warnings.add(msg.toString());
            }
            if (!warnings.isEmpty()) {
                System.out.println("Generated document contains the following warnings:");
                for (String warning:warnings) {
                    System.out.print("\t");
                    System.out.println(warning);
                }
            }
        }
    }

    private static void usage() {
        System.out.println("Generates an SPDX JSON-LD file containing all of the supported classes.");
        System.out.println("Usage: FullSpdxV3Example outputfile");
    }
}
