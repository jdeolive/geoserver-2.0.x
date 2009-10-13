package org.geoserver.security.decorators;

import static org.easymock.EasyMock.*;

import org.acegisecurity.AcegiSecurityException;
import org.geoserver.security.SecureObjectsTest;
import org.geoserver.security.SecureCatalogImpl.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.FeatureType;

public class ReadOnlyDataAccessTest extends SecureObjectsTest {

    private DataAccess da;

    private NameImpl name;

    protected void setUp() throws Exception {
        super.setUp();

        FeatureSource fs = createNiceMock(FeatureSource.class);
        replay(fs);
        FeatureType schema = createNiceMock(FeatureType.class);
        replay(schema);
        da = createNiceMock(DataAccess.class);
        name = new NameImpl("blah");
        expect(da.getFeatureSource(name)).andReturn(fs);
        replay(da);
    }

    public void testDontChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.HIDE);
        ReadOnlyFeatureSource fs = (ReadOnlyFeatureSource) ro.getFeatureSource(name);
        assertEquals(WrapperPolicy.HIDE, fs.policy);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.RO_CHALLENGE);
        ReadOnlyFeatureSource fs = (ReadOnlyFeatureSource) ro.getFeatureSource(name);
        assertEquals(WrapperPolicy.RO_CHALLENGE, fs.policy);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with a security exception");
        } catch (AcegiSecurityException e) {
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with a security exception");
        } catch (AcegiSecurityException e) {
        }
    }

}
