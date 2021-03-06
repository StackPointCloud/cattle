package io.cattle.platform.api.stack;

import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.core.addon.ComposeConfig;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.TypeIdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StackExportConfigActionHandler implements ActionHandler {

    ObjectManager objectManager;
    ComposeExportService composeExportService;

    public StackExportConfigActionHandler(ObjectManager objectManager, ComposeExportService composeExportService) {
        super();
        this.objectManager = objectManager;
        this.composeExportService = composeExportService;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack stack = (Stack) obj;
        List<? extends Long> serviceIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceConstants.FIELD_SERVICE_IDS).asList(Long.class);

        List<? extends Service> services = objectManager.children(stack, Service.class);
        List<Service> toExport = new ArrayList<>();
        for (Service service : services) {
            // export only non-removed requested services
            if ((serviceIds == null || serviceIds.isEmpty()) || serviceIds.contains(service.getId())) {
                if (service.getRemoved() == null && !service.getState().equals(CommonStatesConstants.REMOVED)) {
                    toExport.add(service);
                }
            }
        }
        String composeConfig = "";
        try {
            IdFormatter idFormatter = new TypeIdFormatter(request.getSchemaFactory());
            composeConfig = composeExportService.buildComposeConfig(idFormatter.formatId(stack.getKind(), stack.getId()).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ComposeConfig(composeConfig);
    }
}
