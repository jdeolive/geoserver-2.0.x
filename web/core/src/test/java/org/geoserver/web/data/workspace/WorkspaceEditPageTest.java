package org.geoserver.web.data.workspace;

import java.util.List;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;

public class WorkspaceEditPageTest extends GeoServerWicketTestSupport {

    private WorkspaceInfo citeWorkspace;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
        citeWorkspace = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        tester.startPage(new WorkspaceEditPage(citeWorkspace));

        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }

    public void testLoad() {
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertModelValue("form:name", MockData.CITE_PREFIX);
        tester.assertModelValue("form:uri", MockData.CITE_URI);
    }

    public void testValidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "http://www.geoserver.org");
        form.submit();

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
    }

    public void testInvalidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "not a valid uri");
        form.submit();

        tester.assertRenderedPage(WorkspaceEditPage.class);
        List<ValidationErrorFeedback> messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        assertEquals("Invalid URI syntax: not a valid uri", messages.get(0).getMessage());
    }

    /**
     * See GEOS-3322, upon a namespace URI change the datastores connection parameter shall be
     * changed accordingly
     */
    public void testUpdatesDataStoresNamespace() {
        final Catalog catalog = getCatalog();
        final List<DataStoreInfo> storesInitial = catalog.getStoresByWorkspace(citeWorkspace,
                DataStoreInfo.class);

        final NamespaceInfo citeNamespace = catalog.getNamespaceByPrefix(citeWorkspace.getName());

        for (DataStoreInfo store : storesInitial) {
            assertEquals(citeNamespace.getURI(), store.getConnectionParameters().get("namespace"));
        }

        FormTester form = tester.newFormTester("form");
        final String newNsURI = "http://www.geoserver.org/changed";
        form.setValue("uri", newNsURI);
        form.submit();
        tester.assertNoErrorMessage();

        List<DataStoreInfo> storesChanged = catalog.getStoresByWorkspace(citeWorkspace,
                DataStoreInfo.class);
        for (DataStoreInfo store : storesChanged) {
            assertEquals(newNsURI, store.getConnectionParameters().get("namespace"));
        }
    }
}
