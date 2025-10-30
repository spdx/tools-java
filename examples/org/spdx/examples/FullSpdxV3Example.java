package org.spdx.examples;


import org.spdx.core.DefaultModelStore;
import org.spdx.core.IModelCopyManager;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v3_0_1.SpdxConstantsV3;
import org.spdx.library.model.v3_0_1.SpdxModelClassFactoryV3;
import org.spdx.library.model.v3_0_1.core.*;
import org.spdx.library.model.v3_0_1.simplelicensing.AnyLicenseInfo;
import org.spdx.library.model.v3_0_1.software.*;
import org.spdx.storage.IModelStore;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.v3jsonldstore.JsonLDStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

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
        try (ISerializableModelStore modelStore = new JsonLDStore(new InMemSpdxStore())) {
            String prefix = "https://spdx.github.io/spdx-spec/v3.0.1/examples/full-example-eaa46bdcfa20#";
            DefaultModelStore.initialize(modelStore, prefix, copyManager);
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
            addCoreClasses(prefix, doc);
            Sbom sbom = addSoftwareClasses(prefix, doc);
            try (OutputStream outStream = new FileOutputStream(outFile)) {
                modelStore.serialize(outStream);
            }
        }
    }

    private static void addCoreClasses(String prefix, SpdxDocument doc) throws InvalidSPDXAnalysisException {
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
        // DictionaryEntry - TODO: Change to make sure it has been created
        // Element - Abstract
        // ElementCollection - Abstract
        // ExternalIdentifier - TODO: Change to make sure it has been created
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
        doc.getSpdxImports().add(doc.createExternalMap(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setExternalSpdxId(orgUri)
                        .setLocationHint(orgLocation)
                .build());
        // Hash - Used in file
        // IndividualElement - Used in software package originated by
        // IntegrityMethod - Used in file and package
        // LifecycleScopedRelationship - TODO: Change to make sure it has been created
        // NamespaceMap - Used in doc already
        // PackageVerificationCode - Going to ignore - deprecated
        // Person - Used in creation info
        // PositiveIntegerRange - TODO: Change to make sure it has been created
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
                        .addExternalRef(doc.createExternalRef(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                                .setExternalRefType(ExternalRefType.MAVEN_CENTRAL)
                                .addLocator("org.spdx:tools-java")
                                .build())
                .build());

    }

    private static Sbom addSoftwareClasses(String prefix, SpdxDocument doc) throws InvalidSPDXAnalysisException {
        // Sbom
        Sbom sbom = doc.createSbom(prefix + "aibom")
                    .setName("AI SBOM")
                    .addSbomType(SbomType.ANALYZED)
                    .addProfileConformance(ProfileIdentifierType.CORE)
                    .addProfileConformance(ProfileIdentifierType.SOFTWARE)
                    .addProfileConformance(ProfileIdentifierType.BUILD)
                    .addProfileConformance(ProfileIdentifierType.SECURITY)
                    .addProfileConformance(ProfileIdentifierType.EXPANDED_LICENSING)
                .build();
        doc.getElements().add(sbom);
        doc.getRootElements().add(sbom);
        // Package
        SpdxPackage pkg = doc.createSpdxPackage(prefix + "tools-java")
                .setName("tools-java")
                .setPrimaryPurpose(SoftwarePurpose.APPLICATION)
                .addAdditionalPurpose(SoftwarePurpose.LIBRARY)
                .addAttributionText("Maintained by the SPDX Community")
                .setBuiltTime(LocalDateTime.of(2025, 10, 15, 9, 10)
                        .format(SPDX_DATE_FORMATTER))
        // ContentIdentifier
                .addContentIdentifier(doc.createContentIdentifier(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setContentIdentifierType(ContentIdentifierType.GITOID)
                        .setContentIdentifierValue("23bd470259f55641eb72b0c5d733edac014a4554")
                        .build())
                .setCopyrightText("Copyright (c) Source Auditor Inc.")
                .setDescription("A command-line utility for creating, converting, comparing, and validating SPDX documents across multiple formats.")
                .setDownloadLocation("https://github.com/spdx/tools-java/releases/download/v2.0.2/tools-java-2.0.2.zip")
                .addExternalIdentifier(doc.createExternalIdentifier(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setExternalIdentifierType(ExternalIdentifierType.URL_SCHEME)
                        .setIdentifier("https://github.com/spdx/tools-java")
                        .setIssuingAuthority("GitHub")
                        .build())
                .addExternalRef(doc.createExternalRef(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
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
                .addVerifiedUsing(doc.createHash(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setAlgorithm(HashAlgorithm.SHA256)
                        .setHashValue("c37ce759c3867780d55791a1804101d288fa921e77ed791e6c053fd5d7513d0d")
                        .build())
                .build();
        doc.getElements().add(pkg);
        sbom.getElements().add(pkg);
        sbom.getRootElements().add(pkg);
        // File
        SpdxFile sourceFile = doc.createSpdxFile(prefix + "example-source")
                .setPrimaryPurpose(SoftwarePurpose.SOURCE)
                .setContentType("text/plain")
                .setCopyrightText("Copyright (c) 2025 Source Auditor Inc.")
                .setFileKind(FileKindType.FILE)
                .setName("./examples/org/spdx/examples/FullSpdxV3Example.java")
                .build();
        sbom.getElements().add(sourceFile);
        doc.getElements().add(sourceFile);
        // Relationships - declared license, concluded license, generated from
        doc.getElements().add(doc.createRelationship(prefix + "example-source-to-pkg")
                        .setRelationshipType(RelationshipType.GENERATES)
                        .setFrom(sourceFile)
                        .addTo(pkg)
                .build());
        AnyLicenseInfo declared = LicenseInfoFactory.parseSPDXLicenseString("Apache-2.0",
                doc.getModelStore(), prefix, doc.getCopyManager(), new ArrayList<>());
        AnyLicenseInfo concluded = LicenseInfoFactory.parseSPDXLicenseString("Apache-2.0",
                doc.getModelStore(), prefix, doc.getCopyManager(), new ArrayList<>());
        doc.getElements().add(doc.createRelationship(prefix + "source-declared")
                        .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                        .setFrom(sourceFile)
                        .addTo(declared)
                .build());
        doc.getElements().add(doc.createRelationship(prefix + "source-concluded")
                .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                .setFrom(sourceFile)
                .addTo(concluded)
                .build());
        doc.getElements().add(doc.createRelationship(prefix + "pkg-declared")
                .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                .setFrom(pkg)
                .addTo(declared)
                .build());
        doc.getElements().add(doc.createRelationship(prefix + "pkg-concluded")
                .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                .setFrom(pkg)
                .addTo(concluded)
                .build());
        // Snippet
        Snippet snippet = doc.createSnippet(prefix + "snippet")
                .addAttributionText("Example code created by Gary O'Neall")
                .setDescription("Main method for the FullSpdxV3Example.java")
                .setCopyrightText("Copyright (c) 2025 Source Auditor Inc.")
                .setByteRange(doc.createPositiveIntegerRange(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setBeginIntegerRange(43)
                        .setEndIntegerRange(89)
                        .build())
                .setLineRange(doc.createPositiveIntegerRange(doc.getModelStore().getNextId(IModelStore.IdType.Anonymous))
                        .setBeginIntegerRange(1548)
                        .setEndIntegerRange(3955)
                        .build())
                .setName("main(String[] args)")
                .setSnippetFromFile(sourceFile)
                .build();
        doc.getElements().add(snippet);
        sbom.getElements().add(snippet);
        doc.getElements().add(doc.createRelationship(prefix + "snippet-declared")
                .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
                .setFrom(snippet)
                .addTo(declared)
                .build());
        doc.getElements().add(doc.createRelationship(prefix + "snippet-concluded")
                .setRelationshipType(RelationshipType.HAS_CONCLUDED_LICENSE)
                .setFrom(snippet)
                .addTo(concluded)
                .build());
        // SoftwareArtifact - Abstract
        return sbom;
    }


    private static void addAIandDataClasses(String prefix, SpdxDocument doc) throws InvalidSPDXAnalysisException {
        Bom aiBom = doc.createBom(prefix + "aibom")
                .setName("AI SBOM")
                .addProfileConformance(ProfileIdentifierType.CORE)
                .addProfileConformance(ProfileIdentifierType.SOFTWARE)
                .addProfileConformance(ProfileIdentifierType.AI)
                .addProfileConformance(ProfileIdentifierType.DATASET)
                .build();
        doc.getElements().add(aiBom);
        doc.getRootElements().add(aiBom);
    }

    private static void usage() {
        System.out.println("Generates an SPDX JSON-LD file containing all of the supported classes.");
        System.out.println("Usage: FullSpdxV3Example outputfile");
    }
}
