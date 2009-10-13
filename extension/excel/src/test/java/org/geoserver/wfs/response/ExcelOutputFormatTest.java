package org.geoserver.wfs.response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;

import com.mockrunner.mock.web.MockHttpServletResponse;


public class ExcelOutputFormatTest extends WFSTestSupport {

    public void testOutputFormat() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp = getAsServletResponse( "wfs?request=GetFeature&version=1.0.0&typeName=sf:PrimitiveGeoFeature&outputFormat=excel");
        InputStream in = getBinaryInputStream(resp);
        
        // check the mime type
        assertEquals("application/msexcel", resp.getContentType());
        
        // check the content disposition
        assertEquals("attachment; filename=PrimitiveGeoFeature.xls", resp.getHeader("Content-Disposition"));
        
        // check we have the expected sheet
        HSSFWorkbook wb = new HSSFWorkbook(in);
        HSSFSheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);
        
        FeatureSource fs = getFeatureSource( MockData.PRIMITIVEGEOFEATURE ); 
            
        // check the number of rows in the output
        final int feautureRows = fs.getCount(Query.ALL);
        assertEquals(feautureRows + 1, sheet.getPhysicalNumberOfRows());
        
        // check the header is what we expect
        final SimpleFeatureType schema = (SimpleFeatureType) fs.getSchema();
        final HSSFRow header = sheet.getRow(0);
        assertEquals("FID", header.getCell(0).getRichStringCellValue().toString());
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            assertEquals(schema.getDescriptor(i).getLocalName(), header.getCell(i+1).getRichStringCellValue().toString());
        }
        
        // check some selected values to see if the content and data type is the one
        // we expect
        FeatureIterator fi = fs.getFeatures().features();
        SimpleFeature sf = (SimpleFeature) fi.next();
        fi.close();
        
        // ... a string cell       
        HSSFCell cell = sheet.getRow(1).getCell(1);
        assertEquals(HSSFCell.CELL_TYPE_STRING, cell.getCellType());
        assertEquals(sf.getAttribute(0), cell.getRichStringCellValue().toString());
        // ... a geom cell
        cell = sheet.getRow(1).getCell(4);
        assertEquals(HSSFCell.CELL_TYPE_STRING, cell.getCellType());
        assertEquals(sf.getAttribute(3).toString(), cell.getRichStringCellValue().toString());
        // ... a number cell
        cell = sheet.getRow(1).getCell(6);
        assertEquals(HSSFCell.CELL_TYPE_NUMERIC, cell.getCellType());
        assertEquals(((Number) sf.getAttribute(5)).doubleValue(), cell.getNumericCellValue());
        // ... a date cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(10);
        assertEquals(HSSFCell.CELL_TYPE_NUMERIC, cell.getCellType());
        assertEquals(sf.getAttribute(9), cell.getDateCellValue());
        // ... a boolean cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(12);
        assertEquals(HSSFCell.CELL_TYPE_BOOLEAN, cell.getCellType());
        assertEquals(sf.getAttribute(11), cell.getBooleanCellValue());
        // ... an empty cell (original value is null -> no cell)
        cell = sheet.getRow(1).getCell(3);
        assertNull(cell);
    }
    
    public void testMultipleFeatureTypes() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp = getAsServletResponse( "wfs?request=GetFeature&typeName=sf:PrimitiveGeoFeature,sf:GenericEntity&outputFormat=excel");
        InputStream in = getBinaryInputStream(resp);
        
        // check we have the expected sheets
        HSSFWorkbook wb = new HSSFWorkbook(in);
        HSSFSheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);
        
        // check the number of rows in the output
        FeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());
        
        sheet = wb.getSheet("GenericEntity");
        assertNotNull(sheet);
        
        // check the number of rows in the output
        fs = getFeatureSource(MockData.GENERICENTITY);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());
    }
}
