package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

@Component(name = "DisabledServiceB", enabled = false, property = "component=disabled")
@ServiceRanking(5)
public class DisabledServiceB implements TestService {

}
