package no.javatime.core.runtime;

import java.util.Collection;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.Extenders;

/**
 * This listener should only listen to services registered by extenders. The filter is
 * {@link Extender#EXTENDER_FILTER}.
 * <p>
 * If the filter is removed or its value is set to {@code false} the extender is removed but not
 * unregistered. The service must than be unregistered by using the service layer.
 * 
 * @param <S> type of extender
 */
public class ExtenderServiceListener<S> implements ServiceListener {

	/**
	 * 
	 * @param event {@code ServiceEvent} object from the framework.
	 */
	@Override
	final public void serviceChanged(final ServiceEvent event) {
		
		Long sid = null;
		ServiceReference<?> sr = null;
		Extender<?> extender = null;
		switch (event.getType()) {
		case ServiceEvent.REGISTERED:
			sr = event.getServiceReference();
			sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			extender = Extenders.getExtender(sr);
			if (null != extender) {
				System.out.println("Registering extender: " + extender.getServiceInterfaceName());
			}
			break;	
		case ServiceEvent.MODIFIED_ENDMATCH:
			sr = event.getServiceReference();
			sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			extender = Extenders.getExtender(sr);
			if (null != extender) {
				System.out.println("Modifying extender: " + extender.getServiceInterfaceName());
			}
			break;
		case ServiceEvent.UNREGISTERING:
			sr = event.getServiceReference();
			sid = (Long) sr.getProperty(Constants.SERVICE_ID);
			extender = Extenders.getExtender(sr);
			if (null != extender) {
				System.out.println("Unregistering extender: " + extender.getServiceInterfaceName());
				// This is the bundle tracker who registered this extender
				BundleTracker<Collection<Extender<?>>> bt = extender.getBundleTracker();
				if (bt instanceof ExtenderBundleTracker) {
					// ((ExtenderBundleTracker) bt).;
				}
			}
			break;
		}
	}
}
