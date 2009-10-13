package org.geoserver.security;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;

public class DataAccessRuleDAOTest extends TestCase {

    DataAccessRuleDAO dao;
    Properties props;
    
    @Override
    protected void setUp() throws Exception {
        // make a nice little catalog that does always tell us stuff is there 
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName((String) anyObject())).andReturn(new WorkspaceInfoImpl()).anyTimes();
        expect(catalog.getLayerByName((String) anyObject())).andReturn(new LayerInfoImpl()).anyTimes();
        replay(catalog);
        
        // prepare some base rules
        props = new Properties();
        props.put("mode", "CHALLENGE");
        props.put("topp.states.w", "ROLE_TSW");
        props.put("topp.*.w", "ROLE_TW");
        props.put("*.*.r", "*");
        
        dao = new MemoryDataAccessRuleDAO(catalog, props);
    }
    
    public void testParse() {
        assertEquals(3, dao.getRules().size());
        
        // check the first rule
        DataAccessRule rule = dao.getRules().get(0);
        assertEquals("*.*.r", rule.getKey());
        assertEquals(1, rule.getRoles().size());
        assertEquals("*", rule.getRoles().iterator().next());
    }
    
    public void testAdd() {
        assertEquals(3, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        assertTrue(dao.addRule(newRule));
        assertEquals(4, dao.getRules().size());
        assertEquals(newRule, dao.getRules().get(1));
        assertFalse(dao.addRule(newRule));
    }
    
    public void testRemove() {
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        assertFalse(dao.removeRule(newRule));
        DataAccessRule first = dao.getRules().get(0);
        assertTrue(dao.removeRule(first));
        assertFalse(dao.removeRule(first));
        assertEquals(2, dao.getRules().size());
    }
    
    public void testStore() {
        Properties newProps = dao.toProperties();
        
        // properties equality does not seem to work...
        assertEquals(newProps.size(), props.size());
        for (Object key : newProps.keySet()) {
            Object newValue = newProps.get(key);
            Object oldValue = newProps.get(key);
            assertEquals(newValue, oldValue);
        }
    }
    
    public void testParsePlain() {
        DataAccessRule rule = dao.parseDataAccessRule("a.b.r", "ROLE_WHO_CARES");
        assertEquals("a", rule.getWorkspace());
        assertEquals("b", rule.getLayer());
        assertEquals(AccessMode.READ, rule.getAccessMode());
    }
    
    public void testParseSpaces() {
        DataAccessRule rule = dao.parseDataAccessRule(" a  . b . r ", "ROLE_WHO_CARES");
        assertEquals("a", rule.getWorkspace());
        assertEquals("b", rule.getLayer());
        assertEquals(AccessMode.READ, rule.getAccessMode());
    }
    
    public void testParseEscapedDots() {
        DataAccessRule rule = dao.parseDataAccessRule("w. a\\.b . r ", "ROLE_WHO_CARES");
        assertEquals("w", rule.getWorkspace());
        assertEquals("a.b", rule.getLayer());
        assertEquals(AccessMode.READ, rule.getAccessMode());

    }
    
}
