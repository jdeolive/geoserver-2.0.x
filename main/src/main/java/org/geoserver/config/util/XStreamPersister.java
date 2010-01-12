/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.MultiHashMap;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Utility class which loads and saves catalog and configuration objects to and
 * from an xstream.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class XStreamPersister {

    
    /**
     * Callback interface or xstream persister.
     */
    public static class Callback {
        protected void postEncodeWorkspace( WorkspaceInfo ws, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeNamespace( NamespaceInfo ns, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeDataStore( DataStoreInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeCoverageStore( CoverageStoreInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeFeatureType( FeatureTypeInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeCoverage( CoverageInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeLayer( LayerInfo ls, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeLayerGroup( LayerGroupInfo ls, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeReference( Object obj, String ref, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
    }
    
    /**
     * logging instance
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver" );
    
    /**
     * internal xstream instance
     */
    XStream xs;

    /**
     * GeoServer reference used to resolve references to gloal from services
     */
    GeoServer geoserver;
    
    /**
     * Catalog reference, used to resolve references to stores, workspaces 
     * + namespaces
     */
    Catalog catalog;
    
    /**
     * Callback instance. 
     */
    Callback callback;
    
    /**
     * Flag controlling how references to objects are encoded.
     */
    boolean referenceByName = false;
    
    /**
     * Constructs the persister and underlying xstream.
     */
    protected XStreamPersister() {
        this(null);
    }
    
    /**
     * Constructs the persister and underlying xstream specifying the stream driver explicitly.
     */
    protected XStreamPersister(HierarchicalStreamDriver streamDriver) {
        
        //control the order in which fields are sorted
        SortableFieldKeySorter sorter = new SortableFieldKeySorter();
        sorter.registerFieldOrder( CatalogImpl.class, new String[]{ "workspaces", "namespaces", "stores", "styles", 
            /* these we actually omit, but the sorter needs them specified */
            "layerGroups", "resources", "maps", "listeners", "layers",  "resourcePool", "resourceLoader", "LOGGER" } ); 
        
        ReflectionProvider reflectionProvider = new CustomReflectionProvider( new FieldDictionary( sorter ) ); 
            //new Sun14ReflectionProvider( new FieldDictionary( sorter  ) ); 
        if ( streamDriver != null ) {
            xs = new XStream( reflectionProvider, streamDriver );
        }
        else {
            xs = new XStream( reflectionProvider );    
        }
        xs.setMode(XStream.NO_REFERENCES);
        
        init(xs);
    }
    
    protected void init(XStream xs) {
        // Default implementations
        initImplementationDefaults(xs);
        
        // Aliases
        xs.alias("global", GeoServerInfo.class);
        xs.alias("logging", LoggingInfo.class);
        xs.alias("jai", JAIInfo.class);
        xs.alias("catalog", Catalog.class);
        xs.alias("namespace", NamespaceInfo.class);
        xs.alias("workspace", WorkspaceInfo.class);
        xs.alias("dataStore", DataStoreInfo.class);
        xs.alias("coverageStore", CoverageStoreInfo.class);
        xs.alias("style",StyleInfo.class);
        xs.alias( "featureType", FeatureTypeInfo.class);
        xs.alias( "coverage", CoverageInfo.class);
        xs.alias( "coverageDimension", CoverageDimensionInfo.class);
        xs.alias( "metadataLink", MetadataLinkInfo.class);
        xs.alias( "attribute", AttributeTypeInfo.class );
        xs.alias( "layer", LayerInfo.class);
        xs.alias( "layerGroup", LayerGroupInfo.class);
        xs.alias( "gridGeometry", GridGeometry2D.class);
        xs.alias( "projected", DefaultProjectedCRS.class);
        xs.alias( "attribution", AttributionInfo.class );
        xs.aliasField("abstract", ResourceInfoImpl.class, "_abstract" );
        
        // GeoServerInfo
        xs.omitField(impl(GeoServerInfo.class), "clientProperties");
        xs.omitField(impl(GeoServerInfo.class), "geoServer");
        xs.registerLocalConverter(impl(GeoServerInfo.class), "metadata", new MetadataMapConverter());
        
        // ServiceInfo
        xs.omitField(impl(ServiceInfo.class), "clientProperties");
        xs.omitField(impl(ServiceInfo.class), "geoServer");
        xs.registerLocalConverter(impl(ServiceInfo.class), "metadata", new MetadataMapConverter());
        
        // Catalog
        xs.omitField(impl(Catalog.class), "resourcePool");
        xs.omitField(impl(Catalog.class), "resourceLoader");
        xs.omitField(impl(Catalog.class), "resources");
        xs.omitField(impl(Catalog.class), "listeners");
        xs.omitField(impl(Catalog.class), "layers");
        xs.omitField(impl(Catalog.class), "maps");
        xs.omitField(impl(Catalog.class), "layerGroups");
        xs.omitField(impl(Catalog.class), "LOGGER");
        xs.registerLocalConverter(impl(Catalog.class), "stores",
                new StoreMultiHashMapConverter());
        xs.registerLocalConverter(impl(Catalog.class), "namespaces",
                new SpaceMapConverter("namespace"));
        xs.registerLocalConverter(impl(Catalog.class), "workspaces",
                new SpaceMapConverter("workspace"));
        
        
        //WorkspaceInfo
        xs.omitField( impl(WorkspaceInfo.class), "_default");
        xs.registerLocalConverter( impl(WorkspaceInfo.class), "metadata", new MetadataMapConverter() );
        
        //NamespaceInfo
        xs.omitField( impl(NamespaceInfo.class), "catalog");
        xs.omitField( impl(NamespaceInfo.class), "_default");
        xs.registerLocalConverter( impl(NamespaceInfo.class), "metadata", new MetadataMapConverter() );
        
        // StoreInfo
        xs.omitField(impl(StoreInfo.class), "catalog");
        xs.omitField(impl(StoreInfo.class), "error");
        //xs.omitField(StoreInfo.class), "workspace"); //handled by StoreInfoConverter
        xs.registerLocalConverter(impl(StoreInfo.class), "workspace", new ReferenceConverter(WorkspaceInfo.class));
        xs.registerLocalConverter(impl(StoreInfo.class), "connectionParameters", new BreifMapConverter() );
        xs.registerLocalConverter(impl(StoreInfo.class), "metadata", new MetadataMapConverter());
        
        // StyleInfo
        xs.omitField(impl(StyleInfo.class), "catalog");
        xs.registerLocalConverter(impl(StyleInfo.class), "metadata", new MetadataMapConverter() );
        
        // ResourceInfo
        xs.omitField( impl(ResourceInfo.class), "catalog");
        xs.omitField( impl(ResourceInfo.class), "crs" );
        xs.registerLocalConverter( impl(ResourceInfo.class), "nativeCRS", new CRSConverter());
        xs.registerLocalConverter( impl(ResourceInfo.class), "store", new ReferenceConverter(StoreInfo.class));
        xs.registerLocalConverter( impl(ResourceInfo.class), "namespace", new ReferenceConverter(NamespaceInfo.class));
        xs.registerLocalConverter( impl(ResourceInfo.class), "metadata", new MetadataMapConverter() );
        
        // FeatureTypeInfo
        
        // CoverageInfo

        // CoverageDimensionInfo
        xs.registerLocalConverter( impl(CoverageDimensionInfo.class), "range", new NumberRangeConverter());
        
        // AttributeTypeInfo
        xs.omitField( impl(AttributeTypeInfo.class), "featureType");
        xs.omitField( impl(AttributeTypeInfo.class), "attribute");
        
        // LayerInfo
        //xs.omitField( LayerInfo.class), "resource");
        xs.registerLocalConverter( impl(LayerInfo.class), "resource", new ReferenceConverter( ResourceInfo.class ) );
        xs.registerLocalConverter( impl(LayerInfo.class), "defaultStyle", new ReferenceConverter( StyleInfo.class ) );
        xs.registerLocalConverter( impl(LayerInfo.class), "styles", new ReferenceCollectionConverter( StyleInfo.class ) );
        xs.registerLocalConverter( impl(LayerInfo.class), "metadata", new MetadataMapConverter() );
        
        // LayerGroupInfo
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "layers", new ReferenceCollectionConverter( LayerInfo.class ));
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "styles", new ReferenceCollectionConverter( StyleInfo.class ));
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "metadata", new MetadataMapConverter() );
        
        //ReferencedEnvelope
        xs.registerLocalConverter( ReferencedEnvelope.class, "crs", new SRSConverter() );
        xs.registerLocalConverter( GeneralEnvelope.class, "crs", new SRSConverter() );
        
        // ServiceInfo
        xs.omitField( impl(ServiceInfo.class), "geoServer" );
        
        // Converters
        xs.registerConverter(new SpaceInfoConverter());
        xs.registerConverter(new StoreInfoConverter());
        xs.registerConverter(new ResourceInfoConverter());
        xs.registerConverter(new FeatureTypeInfoConverter());
        xs.registerConverter(new CoverageInfoConverter());
        xs.registerConverter(new LayerInfoConverter());
        xs.registerConverter(new LayerGroupInfoConverter());
        xs.registerConverter(new GridGeometry2DConverter());
        xs.registerConverter(new ProxyCollectionConverter( xs.getMapper() ) );
        
        callback = new Callback();
    }

    public XStream getXStream() {
        return xs;
    }

    public ClassAliasingMapper getClassAliasingMapper() {
        return (ClassAliasingMapper) xs.getMapper().lookupMapperOfType( ClassAliasingMapper.class );
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public void setGeoServer(GeoServer geoserver) {
        this.geoserver = geoserver;
    } 
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    public void setReferenceByName(boolean referenceByName) {
        this.referenceByName = referenceByName;
    }
    
    public void setExcludeIds() {
        xs.omitField( WorkspaceInfoImpl.class, "id");
        xs.omitField( NamespaceInfoImpl.class, "id");
        xs.omitField(StoreInfoImpl.class, "id");
        xs.omitField(StyleInfoImpl.class, "id");
        xs.omitField( ResourceInfoImpl.class, "id");
        xs.omitField( LayerInfoImpl.class, "id");
        xs.omitField(LayerGroupInfoImpl.class, "id" );
        xs.omitField(AttributeTypeInfoImpl.class, "id");
    }
    
    /**
     * Saves an object to persistence.
     * 
     * @param obj The object to save. 
     * @param out The stream to save the object to.
     * 
     * @throws IOException
     */
    public void save(Object obj, OutputStream out) throws IOException {
        //unwrap dynamic proxies
        obj = unwrapProxies( obj );
        xs.toXML(obj, new OutputStreamWriter( out, "UTF-8" ));
    }
    
    /**
     * Unwraps any proxies around the object.
     * <p>
     * If the object is not being proxied it is passed back.
     * </p>
     */
    public static Object unwrapProxies( Object obj ) {
        obj = SecureCatalogImpl.unwrap( obj );
        obj = GeoServerImpl.unwrap( obj );
        obj = CatalogImpl.unwrap( obj );
        return obj;
    }
    
    /**
     * Loads an object from peristence.
     * 
     * @param in The input stream to read the object from.
     * @param clazz The class of the expected object.
     * 
     * @throws IOException
     */
    public <T> T load(InputStream in, Class<T> clazz ) throws IOException {
        T obj = clazz.cast( xs.fromXML( in ) );
        
        //call resolve() to ensure that any references created during loading
        // get resolved to actual objects, for instance for links from datastores
        // to workspaces
        if ( obj instanceof CatalogImpl ) {
            ((CatalogImpl)obj).resolve();
        }
        
        return obj;
    }

    /**
     * Sets up mappings from interface to implementation classes.
     * 
     */
    protected void initImplementationDefaults(XStream xs) {
        //configuration
        xs.addDefaultImplementation(GeoServerInfoImpl.class, GeoServerInfo.class);
        xs.addDefaultImplementation(LoggingInfoImpl.class, LoggingInfo.class);
        xs.addDefaultImplementation(JAIInfoImpl.class, JAIInfo.class);
        xs.addDefaultImplementation(ContactInfoImpl.class, ContactInfo.class);
        xs.addDefaultImplementation(AttributionInfoImpl.class, AttributionInfo.class);
        
        //catalog
        xs.addDefaultImplementation(CatalogImpl.class, Catalog.class);
        xs.addDefaultImplementation(NamespaceInfoImpl.class, NamespaceInfo.class);
        xs.addDefaultImplementation(WorkspaceInfoImpl.class, WorkspaceInfo.class);
        xs.addDefaultImplementation(DataStoreInfoImpl.class, DataStoreInfo.class);
        xs.addDefaultImplementation(CoverageStoreInfoImpl.class, CoverageStoreInfo.class);
        xs.addDefaultImplementation(StyleInfoImpl.class, StyleInfo.class);
        xs.addDefaultImplementation(FeatureTypeInfoImpl.class, FeatureTypeInfo.class );
        xs.addDefaultImplementation(CoverageInfoImpl.class, CoverageInfo.class);
        xs.addDefaultImplementation(CoverageDimensionImpl.class, CoverageDimensionInfo.class);
        xs.addDefaultImplementation(MetadataLinkInfoImpl.class, MetadataLinkInfo.class);
        xs.addDefaultImplementation(AttributeTypeInfoImpl.class, AttributeTypeInfo.class );
        xs.addDefaultImplementation(LayerInfoImpl.class, LayerInfo.class);
        xs.addDefaultImplementation(LayerGroupInfoImpl.class, LayerGroupInfo.class );
        
        //supporting objects
        xs.addDefaultImplementation(GridGeometry2D.class, GridGeometry.class );
        xs.addDefaultImplementation(DefaultGeographicCRS.class, CoordinateReferenceSystem.class);
        
        //collections
        xs.addDefaultImplementation(ArrayList.class, List.class);
    }
    
    protected Class impl(Class interfce) {
        //special case case classes, they don't get registered as default implementations
        // only concrete classes do
        if (interfce == ServiceInfo.class) {
            return ServiceInfoImpl.class;
        }
        if (interfce == StoreInfo.class) {
            return StoreInfoImpl.class;
        }
        if (interfce == ResourceInfo.class) {
            return ResourceInfoImpl.class;
        }
        
        Class clazz = getXStream().getMapper().defaultImplementationOf(interfce); 
        if (clazz == null) {
            throw new RuntimeException("No default mapping for " + interfce);
        }
        return clazz;
    }
    
    /**
     * Custom reflection provider which unwraps proxies, and skips empty collections
     * and maps.
     */
    class CustomReflectionProvider extends Sun14ReflectionProvider {
        
        public CustomReflectionProvider( FieldDictionary fd ) {
            super( fd );
        }
        
        @Override
        public void visitSerializableFields(Object object, Visitor visitor) {
            super.visitSerializableFields(object, new VisitorWrapper(visitor));
        }
        
        class VisitorWrapper implements ReflectionProvider.Visitor {

            Visitor wrapped;
            
            public VisitorWrapper( Visitor wrapped ) {
                this.wrapped = wrapped;
            }
            
            public void visit(String name, Class type, Class definedIn,
                    Object value) {
                
                //skip empty collections + maps
                if ( value instanceof Collection && ((Collection)value).isEmpty() ) {
                    return;
                }
                if ( value instanceof Map && ((Map)value).isEmpty() ) {
                    return;
                }
                
                //unwrap any proxies
                value = unwrapProxies(value);
                wrapped.visit( name, type, definedIn, value);
            }
            
        }
    }

    //
    // custom converters
    //
    
    //simple object converters
    /**
     * Map converter which encodes a map more breifly than the standard map converter.
     */
    protected class BreifMapConverter extends MapConverter {
        
        public BreifMapConverter() {
            super(getXStream().getMapper());
        }
        
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
        
            Map map = (Map) source;
            for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                
                if ( entry.getValue() == null ) {
                    continue;
                }
                
                writer.startNode("entry");
                writer.addAttribute( "key", entry.getKey().toString());
                if ( entry.getValue() != null ) {
                    writer.setValue(entry.getValue().toString());
                }
                
                writer.endNode();
            }
        }

        @Override
        protected void populateMap(HierarchicalStreamReader reader,
                UnmarshallingContext context, Map map) {
            
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                
                //we support two syntaxes here:
                // 1) <key>value</key>
                // 2) <key><type>value</type></key>
                // 3) <entry key="">value</entry>
                // 4) <entry>
                //      <type>key</type>
                //      <type>value</type>
                //    </entry>
                String key = reader.getNodeName();
                Object value = null;
                if ( "entry".equals( key ) ) {
                    if ( reader.getAttribute( "key") != null ) {
                        //this is case 3
                        key = reader.getAttribute( "key" );
                        value = reader.getValue();
                    }
                    else if ( reader.hasMoreChildren() ){
                        //this is case 4
                        reader.moveDown();
                        
                        key = reader.getValue();
                        
                        reader.moveUp();
                        reader.moveDown();
                        
                        value = reader.getValue();
                        
                        reader.moveUp();
                    }

                }
                else {
                    boolean old = false;
                    if (reader.hasMoreChildren()) {
                        //this handles case 2
                        old = true;
                        reader.moveDown();    
                    }
                    
                    value = readItem(reader, context, map);
                    
                    if ( old ) {
                        reader.moveUp();    
                    }
                }

                map.put(key, value);

                reader.moveUp();
            }
        }

        @Override
        protected Object readItem(HierarchicalStreamReader reader, UnmarshallingContext context,
                Object current) {
            return reader.getValue();
        }
    }
    
    /**
     * Custom converter for the special metadata map.
     */
    class MetadataMapConverter extends BreifMapConverter {
        
        @Override
        public boolean canConvert(Class type) {
            return MetadataMap.class.equals(type) || super.canConvert(type);
        }
        
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            if ( source instanceof MetadataMap) {
                MetadataMap mdmap = (MetadataMap) source;
                source = mdmap.getMap();
            }
            
            super.marshal(source, writer, context);
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map map = (Map) super.unmarshal(reader, context);
            if ( !(map instanceof MetadataMap ) ) {
                map = new MetadataMap(map);
            }
            return map;
        }
    }
    /**
     * Converters which encodes an object by a reference, or its id.
     */
    //class ReferenceConverter extends AbstractSingleValueConverter {
    class ReferenceConverter implements Converter {
        Class clazz;
        
        public ReferenceConverter( Class clazz ) {
            this.clazz = clazz;
        }

        public boolean canConvert(Class type) {
            return clazz.isAssignableFrom( type );
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            //could be a proxy, unwrap it
            source = CatalogImpl.unwrap( source );
            
            //gets its id
            String id = (String) OwsUtils.get( source, "id" );
            if ( id != null && !referenceByName) {
                writer.startNode("id");
                writer.setValue( id );
                writer.endNode();
            }
            else {
                //use name if no id set
                String name = (String) OwsUtils.get( source, "name" );
                if ( name != null ) {
                    writer.startNode("name");
                    writer.setValue( name );
                    writer.endNode();
                }
                else {
                    throw new IllegalArgumentException( "Unable to marshal reference with no id or name.");
                }
            }
            
            callback.postEncodeReference( source, id, writer, context );
        }
        
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            
            String ref = null;
            if ( reader.hasMoreChildren() ) {
                reader.moveDown();
                ref = reader.getValue();
                reader.moveUp();
            }
            else {
                ref = reader.getValue();
            }
            Object proxy = ResolvingProxy.create( ref, clazz );
            Object resolved = proxy;
            if ( catalog != null ) {
                resolved = ResolvingProxy.resolve( catalog, proxy );
            }
            
            return CatalogImpl.unwrap( resolved );
        }
    }
    class ReferenceCollectionConverter extends CollectionConverter {
        Class clazz;
        public ReferenceCollectionConverter(Class clazz) {
            super( getXStream().getMapper() );
            this.clazz = clazz;
        }
        
        @Override
        protected void writeItem(Object item, MarshallingContext context,
                HierarchicalStreamWriter writer) {
            ClassAliasingMapper cam = 
                (ClassAliasingMapper) mapper().lookupMapperOfType( ClassAliasingMapper.class );
            
            String elementName = cam.serializedClass( clazz );
            if ( elementName == null ) {
                elementName = cam.serializedClass( item.getClass() );
            }
            writer.startNode(elementName);
            if(item != null)
                context.convertAnother( item, new ReferenceConverter( clazz ) );
            writer.endNode();
        }
        
        @Override
        protected Object readItem(HierarchicalStreamReader reader,
                UnmarshallingContext context, Object current) {
            return context.convertAnother( current, clazz, new ReferenceConverter( clazz ) );
        }
    }
    /**
     * Converter which unwraps proxies in a collection.
     */
    class ProxyCollectionConverter extends CollectionConverter {

        public ProxyCollectionConverter(Mapper mapper) {
            super(mapper);
        }
        
        @Override
        protected void writeItem(Object item, MarshallingContext context,
                HierarchicalStreamWriter writer) {
                        
            super.writeItem(unwrapProxies(item), context, writer);
        }
    }
    
    /**
     * Converter for coordinate reference system objects that converts by SRS code.
     */
    static class SRSConverter extends AbstractSingleValueConverter {
        
        public boolean canConvert(Class type) {
            return CoordinateReferenceSystem.class.isAssignableFrom(type);
        }
        
        @Override
        public String toString(Object obj) {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) obj;
            try {
                return "EPSG:" + CRS.lookupEpsgCode(crs, true);
            } 
            catch (FactoryException e) {
                XStreamPersister.LOGGER.warning( "Could not determine epsg code of crs, encoding as WKT");
                return crs.toWKT();
            }
        }
        
        @Override
        public Object fromString(String str) {
            if ( str.toUpperCase().startsWith( "EPSG:") ) {
                try {
                    return CRS.decode( str );
                } 
                catch (Exception e) {
                    XStreamPersister.LOGGER.log( Level.WARNING, "Error decode epsg code: "+str, e );
                }    
            }
            else {
                try {
                    return CRS.parseWKT( str );
                } 
                catch (FactoryException e) {
                    XStreamPersister.LOGGER.log( Level.WARNING, "Error decode wkt: "+str, e );
                }
            }
            return null;
        }
    }
    
    /**
     * Converter for coordinate reference system objects that converts by WKT. 
     *
     */
    static class CRSConverter extends AbstractSingleValueConverter {

        @Override
        public boolean canConvert(Class type) {
            return CoordinateReferenceSystem.class.isAssignableFrom(type);
        }

        @Override
        public String toString(Object obj) {
            return ((CoordinateReferenceSystem)obj).toWKT();
        }
        
        @Override
        public Object fromString(String str) {
            try {
                return CRS.parseWKT( str );
            } 
            catch (Exception e) {
                try {
                    return new SRSConverter().fromString( str );
                }
                catch( Exception e1 ) {}
                
                throw new RuntimeException( e );
            }
        }
        
    }
    
    /**
     * Converter for coverage grid geometry.
     *
     */
    class GridGeometry2DConverter extends AbstractReflectionConverter {
        public GridGeometry2DConverter() {
            super( GridGeometry2D.class );
        }

        @Override
        protected void doMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
         
            GridGeometry2D g = (GridGeometry2D) source;
            MathTransform tx = g.getGridToCRS();

            writer.addAttribute("dimension", String.valueOf(g.getGridRange().getDimension()));
            
            //grid range
            StringBuffer low = new StringBuffer();
            StringBuffer high = new StringBuffer();
            for (int r = 0; r < g.getGridRange().getDimension(); r++) {
                low.append(g.getGridRange().getLow(r)).append(" ");
                high.append(g.getGridRange().getHigh(r)+1).append(" ");
            }
            low.setLength(low.length()-1);
            high.setLength(high.length()-1);
            
            writer.startNode("range");
            writer.startNode( "low" ); writer.setValue( low.toString() ); writer.endNode();
            writer.startNode( "high" ); writer.setValue( high.toString() ); writer.endNode();
            writer.endNode();
            
            //transform
            if (tx instanceof AffineTransform) {
                AffineTransform atx = (AffineTransform) tx;
                
                writer.startNode("transform");
                writer.startNode("scaleX"); writer.setValue(Double.toString( atx.getScaleX())); writer.endNode();
                writer.startNode("scaleY"); writer.setValue(Double.toString( atx.getScaleY())); writer.endNode();
                writer.startNode("shearX"); writer.setValue(Double.toString( atx.getShearX())); writer.endNode();
                writer.startNode("shearX"); writer.setValue(Double.toString( atx.getShearY())); writer.endNode();
                writer.startNode("translateX"); writer.setValue(Double.toString( atx.getTranslateX())); writer.endNode();
                writer.startNode("translateY"); writer.setValue(Double.toString( atx.getTranslateY())); writer.endNode();
                writer.endNode();
            }
            
            //crs
            writer.startNode("crs");
            context.convertAnother( g.getCoordinateReferenceSystem(), 
                new SingleValueConverterWrapper( new SRSConverter() ) );
            writer.endNode();
           
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
             int[] high,low;
            
            //reader.moveDown(); //grid
            
            reader.moveDown(); //range
            
            reader.moveDown(); //low
            low = toIntArray( reader.getValue() );
            reader.moveUp();
            reader.moveDown(); //high
            high = toIntArray( reader.getValue() );
            reader.moveUp();
            
            reader.moveUp(); //range
            
            if ( reader.hasMoreChildren() ) {
                reader.moveDown(); //transform or crs
            }
            
            AffineTransform2D gridToCRS = null;
            if ( "transform".equals( reader.getNodeName() ) ) {
                double sx,sy,shx,shy,tx,ty;
                
                reader.moveDown(); //scaleX
                sx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //scaleY
                sy = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //shearX
                shx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //shearY
                shy = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //translateX
                tx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //translateY
                ty = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                

                // set tranform
                gridToCRS = new AffineTransform2D(sx, shx, shy, sy, tx, ty);
                
                reader.moveUp();
                if ( reader.hasMoreChildren() ) {
                    reader.moveDown(); //crs
                }
            }
            
            CoordinateReferenceSystem crs = null;
            if ( "crs".equals( reader.getNodeName() ) ) {
                crs = (CoordinateReferenceSystem) context.convertAnother( null, CoordinateReferenceSystem.class, 
                    new SingleValueConverterWrapper( new SRSConverter() ));
                reader.moveUp();
            }
            
            // new grid range
            GeneralGridEnvelope gridRange = new GeneralGridEnvelope(low, high);
            
            
            GridGeometry2D gg = new GridGeometry2D( gridRange, gridToCRS, crs );
            return serializationMethodInvoker.callReadResolve(gg);
        }
            
        int[] toIntArray( String s ) {
            String[] split = s.split( " " );
            int[] ints = new int[split.length];
            for ( int i = 0; i < split.length; i++ ) {
                ints[i] = Integer.parseInt( split[i] );
            }
            return ints;
        }
    }
    class NumberRangeConverter extends AbstractReflectionConverter {
     
        @Override
        public boolean canConvert(Class clazz) {
            return NumberRange.class.isAssignableFrom( clazz );
        }
        
        @Override
        public void marshal(Object original, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            NumberRange range = (NumberRange) original;
            
            writer.startNode("min");
            if ( Double.isInfinite( ((Number)range.getMinValue()).doubleValue() ) ) {
                context.convertAnother( "-inf" );
            }
            else {
                context.convertAnother( range.getMinValue() );  
            }
            writer.endNode();
            
            writer.startNode("max");
            if ( Double.isInfinite( ((Number)range.getMaxValue()).doubleValue() )) {
                context.convertAnother( "inf");
            }
            else {
                context.convertAnother( range.getMaxValue() );  
            }
            writer.endNode();
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            //JD: we handle infinite manually b/c the json serializer chokes on inifinte values 
            // b/c JSON does not support it
            Double min = null, max = null;
            while( reader.hasMoreChildren() ) {
                reader.moveDown();
                if ( "min".equals( reader.getNodeName() ) ) {
                    if ( !"-inf".equals( reader.getValue() ) ) {
                        min = Double.parseDouble( reader.getValue() ); 
                    }
                }
                if ( "max".equals( reader.getNodeName() ) ) {
                    if ( !"inf".equals( reader.getValue() ) ) {
                        max = Double.parseDouble( reader.getValue() ); 
                    }
                }
                reader.moveUp();
            }
            
            min = min != null ? min : Double.NEGATIVE_INFINITY;
            max = max != null ? max : Double.POSITIVE_INFINITY;
            
            return NumberRange.create( min.doubleValue(), true, max.doubleValue(), true );
        }
    }
    
    //catalog object converters
    /**
     * Base class for all custom reflection based converters.
     */
    class AbstractReflectionConverter extends ReflectionConverter {
        Class clazz;

        public AbstractReflectionConverter() {
            this(Object.class);
        }

        public AbstractReflectionConverter(Class clazz) {
            super(getXStream().getMapper(),getXStream().getReflectionProvider());
            this.clazz = clazz;
        }

        @Override
        public boolean canConvert(Class type) {
            return clazz.isAssignableFrom( type );
        }
        
        @Override
        protected void doMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            super.doMarshal(source, writer, context);
            postDoMarshal(source,writer,context);
        }
        
        protected void postDoMarshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        }
    }

    /**
     * Converter for workspaces and namespaces.
     */
    class SpaceInfoConverter extends AbstractReflectionConverter {
        @Override
        public boolean canConvert(Class type) {
            return WorkspaceInfo.class.isAssignableFrom(type) || 
                NamespaceInfo.class.isAssignableFrom(type);
        }
        
        @Override
        protected void postDoMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            if ( source instanceof WorkspaceInfo ) {
                callback.postEncodeWorkspace( (WorkspaceInfo)source,writer,context );
            }
            else {
                callback.postEncodeNamespace( (NamespaceInfo) source,writer,context );
            }
        }
    }
    /**
     * Converter for data stores and coverage stores.
     */
    class StoreInfoConverter extends AbstractReflectionConverter {

        public StoreInfoConverter() {
            super(StoreInfo.class);
        }

        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            
            StoreInfo store = (StoreInfo) result;
            if ( store instanceof DataStoreInfo ) {
                callback.postEncodeDataStore( (DataStoreInfo) store, writer, context );
            }
            else {
                callback.postEncodeCoverageStore( (CoverageStoreInfo) store, writer, context );
            }
            
        }
        public Object doUnmarshal(Object result,
                HierarchicalStreamReader reader, UnmarshallingContext context) {
            StoreInfo store = (StoreInfo) super.doUnmarshal(result, reader, context);
            
            LOGGER.info( "Loaded store '" +  store.getName() +  "', " + (store.isEnabled() ? "enabled" : "disabled") );
            return store;
        }
    }

    /**
     * Converter for multi hash maps containing coverage stores and data stores.
     */
    static class StoreMultiHashMapConverter implements Converter {
        public boolean canConvert(Class type) {
            return MultiHashMap.class.equals(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            MultiHashMap map = (MultiHashMap) source;
            for (Object v : map.values()) {
                if (v instanceof DataStoreInfo) {
                    writer.startNode("dataStore");
                    context.convertAnother(v);
                    writer.endNode();
                }
                if (v instanceof CoverageStoreInfo ) {
                    writer.startNode( "coverageStore" );
                    context.convertAnother(v);
                    writer.endNode();
                }
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            MultiHashMap map = new MultiHashMap();
            
            while( reader.hasMoreChildren() ) {
                reader.moveDown();
                
                Object o = 0;
                if ( "dataStore".equals( reader.getNodeName() ) ) {
                    o = context.convertAnother( map, DataStoreInfoImpl.class );
                }
                else {
                    o = context.convertAnother( map, CoverageStoreInfoImpl.class );
                }
                map.put( o.getClass(), o );
                
                reader.moveUp();
            }
            
            return map;
        }
    }

    /**
     * Converter for handling maps containing workspaces and namespaces.
     *
     */
    static class SpaceMapConverter implements Converter {

        String name;
        
        public SpaceMapConverter( String name ) {
            this.name = name;
        }
        
        public boolean canConvert(Class type) {
            return Map.class.isAssignableFrom(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            Map map = (Map) source;
            
            for (Object o : map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if ( e.getKey() == null ) {
                    continue;
                }
                
                writer.startNode(name);
                if ( map.get( null ) == e.getValue() ) {
                    writer.addAttribute("default", "true");
                }
                context.convertAnother(e.getValue());
                writer.endNode();
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            Map map = new HashMap();
            
            while( reader.hasMoreChildren() ) {
               reader.moveDown();

               boolean def = "true".equals( reader.getAttribute( "default") );
               
               if ( "namespace".equals( name ) ) {
                   NamespaceInfoImpl ns = (NamespaceInfoImpl) context.convertAnother( map, NamespaceInfoImpl.class );
                   map.put( ns.getPrefix() , ns );
                   if ( def ) {
                       map.put( null, ns );
                   }
                   LOGGER.info( "Loading namespace '" + ns.getPrefix() + "'" );
               }
               else {
                   WorkspaceInfoImpl ws = (WorkspaceInfoImpl) context.convertAnother( map, WorkspaceInfoImpl.class );
                   map.put( ws.getName() , ws );
                   if ( def ) {
                       map.put( null, ws );
                   }
                   LOGGER.info( "Loading workspace '" + ws.getName() + "'" );
               }
               
               reader.moveUp();
            }
            
            return map;
        }
    }

    /**
     * Base converter for handling resources.
     */
    class ResourceInfoConverter extends AbstractReflectionConverter {
        
        public ResourceInfoConverter() {
            this(ResourceInfo.class);
        }
        
        public ResourceInfoConverter(Class clazz ) {
            super(clazz);
        }
        
        public Object doUnmarshal(Object result,
                HierarchicalStreamReader reader, UnmarshallingContext context) {
            ResourceInfo obj = (ResourceInfo) super.doUnmarshal(result, reader, context);
            
            String enabled = obj.isEnabled() ? "enabled" : "disabled";
            String type = obj instanceof CoverageInfo ? "coverage" : 
                obj instanceof FeatureTypeInfo ? "feature type" : "resource";
            
            LOGGER.info( "Loaded " + type + " '" + obj.getName() + "', " + enabled );
            
            return obj;
        }
    }
    
    /**
     * Converter for feature types.
     */
    class FeatureTypeInfoConverter extends ResourceInfoConverter {

        public FeatureTypeInfoConverter() {
            super(FeatureTypeInfo.class);
        }
        
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            FeatureTypeInfoImpl featureType = (FeatureTypeInfoImpl) result;
            
            //ensure null list does not result
            if ( featureType.getAttributes() == null ){
                featureType.setAttributes(new ArrayList());
            }
            
            callback.postEncodeFeatureType(featureType, writer, context);
        }
    }
    
    /**
     * Converter for feature types.
     */
    class CoverageInfoConverter extends ResourceInfoConverter {
        public CoverageInfoConverter() {
            super( CoverageInfo.class );
        }
        
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            callback.postEncodeCoverage((CoverageInfo)result, writer, context);
        }
    }
    
    /**
     * Converter for layers.
     */
    class LayerInfoConverter extends AbstractReflectionConverter {

        public LayerInfoConverter() {
            super( LayerInfo.class );
        }
        
        @Override
        protected void doMarshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            // write out the name, which is a derived property now
            // TODO: remove this when resource/publishing split is done
            LayerInfo l = (LayerInfo) source;
            writer.startNode("name");
            writer.setValue(l.getName());
            writer.endNode();

            super.doMarshal(source, writer, context);
        }
                
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            /*
            LayerInfo l = (LayerInfo) result;
            writer.startNode("resource");
            context.convertAnother( l.getResource(), new ReferenceConverter( ResourceInfo.class ) );
            writer.endNode();
            */
            callback.postEncodeLayer( (LayerInfo) result, writer, context );
        }
    }
  
    /**
     * Converter for layer groups.
     */
    class LayerGroupInfoConverter extends AbstractReflectionConverter {

        public LayerGroupInfoConverter() {
            super( LayerGroupInfo.class );
        }

        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            callback.postEncodeLayerGroup((LayerGroupInfo)result, writer, context);
        }
    }
}
