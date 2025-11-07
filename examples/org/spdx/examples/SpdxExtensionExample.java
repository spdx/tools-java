package org.spdx.examples;

import org.spdx.core.IModelCopyManager;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v3_0_1.extension.Extension;
import org.spdx.storage.IModelStore;
import org.spdx.storage.PropertyDescriptor;

import javax.annotation.Nullable;
import java.util.Optional;

public class SpdxExtensionExample extends Extension {

    static final PropertyDescriptor EXTENSION_PROPERTY_DESCRIPTOR = new PropertyDescriptor("extensionProp", "https://my/extension/namespace/");

    public SpdxExtensionExample(IModelStore modelStore, String objectUri, @Nullable IModelCopyManager copyManager, boolean create, String idPrefix) throws InvalidSPDXAnalysisException {
        super(modelStore, objectUri, copyManager, create, idPrefix);
    }

    public SpdxExtensionExample(IModelStore modelStore, String objectUri, @Nullable IModelCopyManager copyManager, boolean create, String specVersion, String idPrefix) throws InvalidSPDXAnalysisException {
        super(modelStore, objectUri, copyManager, create, idPrefix);
    }

    public SpdxExtensionExample setExtensionProperty(String value) throws InvalidSPDXAnalysisException {
        setPropertyValue(EXTENSION_PROPERTY_DESCRIPTOR, value);
        return this;
    }

    public Optional<String> getExtensionProperty() throws InvalidSPDXAnalysisException {
        return getStringPropertyValue(EXTENSION_PROPERTY_DESCRIPTOR);
    }

    @Override
    public String getType() {
        return "Extension.example";
    }
}
