package org.geoserver.web.security.user;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.geoserver.security.GeoserverUserDao;
import org.geoserver.web.GeoServerWicketTestSupport;

public class UserListPageTest extends GeoServerWicketTestSupport {
    
    private GeoserverUserDao dao;

    @Override
    protected void setUpInternal() throws Exception {
        dao = GeoserverUserDao.get();
        dao.putUser(new User("user", "pwd", true, true, true, true, 
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_WFS_ALL"), new GrantedAuthorityImpl("ROLE_WMS_ALL")}));
        login();
        tester.startPage(UserPage.class);
    }

    public void testRenders() throws Exception {
        tester.assertRenderedPage(UserPage.class);
    }
    
    public void testEditUser() throws Exception {
        // the name link for the first user
        tester.clickLink("table:listContainer:items:1:itemProperties:0:component:link");
        tester.assertRenderedPage(EditUserPage.class);
        assertEquals("admin", tester.getComponentFromLastRenderedPage("userForm:username").getModelObject());
    }
    
//    public void testNewUser() throws Exception {
//        tester.clickLink("addUser");
//        tester.assertRenderedPage(NewUserPage.class);
//    }
    
//    public void testRemove() throws Exception {
//        dao.loadUserByUsername("user");
//        // the remove link for the second user
//        tester.clickLink("table:listContainer:items:2:itemProperties:3:component:link");
//        tester.assertRenderedPage(UserPage.class);
//        try {
//            dao.loadUserByUsername("user");
//            fail("The user should have been removed");
//        } catch(UsernameNotFoundException e) {
//            // fine
//        }
//    }
}
