/**
 * SPDX-FileContributor: Gary O'Neall
 * SPDX-FileCopyrightText: Copyright (c) 2025 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 * <br/>
 * Example of serializing a single expanded license
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
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v3_0_1.SpdxModelClassFactoryV3;
import org.spdx.library.model.v3_0_1.core.CreationInfo;
import org.spdx.library.model.v3_0_1.core.Element;
import org.spdx.library.model.v3_0_1.core.ProfileIdentifierType;
import org.spdx.library.model.v3_0_1.core.SpdxDocument;
import org.spdx.library.model.v3_0_1.expandedlicensing.ExtendableLicense;
import org.spdx.library.model.v3_0_1.simplelicensing.AnyLicenseInfo;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.Verify;
import org.spdx.v3jsonldstore.JsonLDStore;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.spdx.tools.Verify.JSON_SCHEMA_RESOURCE_V3;

/**
 * Simple example serializing a single expanded license
 */
public class ExpandedLicenseExampleV3 {

    static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
            modelStore.setUseExternalListedElements(true); // setting this to false will include all the listed license details in the document
            String defaultDocUri = "https://spdx.github.io/spdx-spec/v3.0.1/examples/complex-license-eaa46bdcfa20";
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
                    .addProfileConformance(ProfileIdentifierType.EXPANDED_LICENSING)
                    .build();
            doc.setIdPrefix(prefix);
            AnyLicenseInfo complexLicense = doc.createConjunctiveLicenseSet(prefix + "complexlicense")
                    // CustomLicense
                    .addMember(doc.createCustomLicense(prefix + "LicenseRef-customlicense1")
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
            doc.getRootElements().add(complexLicense);
            doc.getElements().add(complexLicense);
            List<String> warnings = new ArrayList<>();
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
            warnings.addAll(complexLicense.verify());
            try (OutputStream outStream = new FileOutputStream(outFile)) {
                modelStore.serialize(outStream, doc);
            }
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
