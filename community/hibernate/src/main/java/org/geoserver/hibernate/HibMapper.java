/*

 */

package org.geoserver.hibernate;

import org.apache.log4j.Logger;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.hibernate.beans.AttributeTypeInfoImplHb;
import org.geoserver.catalog.hibernate.beans.AttributionInfoImplHb;
import org.geoserver.catalog.hibernate.beans.CoverageDimensionInfoImplHb;
import org.geoserver.catalog.hibernate.beans.CoverageInfoImplHb;
import org.geoserver.catalog.hibernate.beans.CoverageStoreInfoImplHb;
import org.geoserver.catalog.hibernate.beans.DataStoreInfoImplHb;
import org.geoserver.catalog.hibernate.beans.FeatureTypeInfoImplHb;
import org.geoserver.catalog.hibernate.beans.LayerInfoImplHb;
import org.geoserver.catalog.hibernate.beans.NamespaceInfoImplHb;
import org.geoserver.catalog.hibernate.beans.LayerGroupInfoImplHb;
import org.geoserver.catalog.hibernate.beans.StyleInfoImplHb;
import org.geoserver.catalog.impl.MapInfoImpl;
import org.geoserver.catalog.hibernate.beans.WorkspaceInfoImplHb;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.hibernate.beans.ContactInfoImplHb;
import org.geoserver.config.hibernate.beans.LoggingInfoImplHb;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.services.hibernate.beans.WCSInfoImplHb;
import org.geoserver.services.hibernate.beans.WFSInfoImplHb;
import org.geoserver.services.hibernate.beans.WMSInfoImplHb;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;

/**
 * 
 * @author ETj <etj at geo-solutions.it>
 */
public class HibMapper {
    private final static Logger LOGGER = Logger.getLogger(HibMapper.class);

    public static Class<?> mapHibernableClass(Class<?> clazz) {
        if (Hibernable.class.isAssignableFrom(clazz))
            return clazz;

        // CATALOG STUFF

        else if (CoverageStoreInfo.class.isAssignableFrom(clazz))
            return CoverageStoreInfoImplHb.class;
        else if (DataStoreInfo.class.isAssignableFrom(clazz))
            return DataStoreInfoImplHb.class;
        else if (StoreInfo.class.isAssignableFrom(clazz)) // mind the order!
            return StoreInfoImpl.class;

        else if (CoverageInfo.class.isAssignableFrom(clazz))
            return CoverageInfoImplHb.class;
        else if (FeatureTypeInfo.class.isAssignableFrom(clazz))
            return FeatureTypeInfoImplHb.class;
        else if (ResourceInfo.class.isAssignableFrom(clazz)) // mind the order!
            return ResourceInfoImpl.class;

        else if (LayerInfo.class.isAssignableFrom(clazz))
            return LayerInfoImplHb.class;
        else if (LayerGroupInfo.class.isAssignableFrom(clazz))
            return LayerGroupInfoImplHb.class;

        else if (NamespaceInfo.class.isAssignableFrom(clazz))
            return NamespaceInfoImplHb.class;
        else if (WorkspaceInfo.class.isAssignableFrom(clazz))
            return WorkspaceInfoImplHb.class;

        else if (CoverageDimensionInfo.class.isAssignableFrom(clazz))
            return CoverageDimensionInfoImplHb.class;

        else if (AttributeTypeInfo.class.isAssignableFrom(clazz))
            return AttributeTypeInfoImplHb.class;

        else if (AttributionInfo.class.isAssignableFrom(clazz))
            return AttributionInfoImplHb.class;

        else if (StyleInfo.class.isAssignableFrom(clazz))
            return StyleInfoImplHb.class;

        // SERVICE STUFF

        else if (WMSInfo.class.isAssignableFrom(clazz))
            return WMSInfoImplHb.class;
        else if (WCSInfo.class.isAssignableFrom(clazz))
            return WCSInfoImplHb.class;
        else if (WFSInfo.class.isAssignableFrom(clazz))
            return WFSInfoImplHb.class;
        else if (ServiceInfo.class.isAssignableFrom(clazz)) // mind the order!
            return ServiceInfoImpl.class; // this is not declared as hibernable :(

        else if (LoggingInfo.class.isAssignableFrom(clazz))
            return LoggingInfoImplHb.class;

        else if (ContactInfo.class.isAssignableFrom(clazz))
            return ContactInfoImplHb.class;

        else {
            // these classes are directly persisted since they already are hibernate-friendly
            if (!MapInfoImpl.class.equals(clazz)) {
                LOGGER.warn("Cannot map class " + clazz.getName()
                        + ". Trying to force it into hibernate...");
            }
            return clazz;
            // throw new IllegalArgumentException("Cannot map class " + clazz.getName());
        }
    }

}
