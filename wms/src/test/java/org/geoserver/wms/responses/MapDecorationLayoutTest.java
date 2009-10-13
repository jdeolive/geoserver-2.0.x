package org.geoserver.wms.responses;

import org.vfny.geoserver.wms.WMSMapContext;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MapDecorationLayoutTest extends TestCase {
    public TestSuite suite() { return new TestSuite(MapDecorationLayout.class); }

    private class MockMapDecoration implements MapDecoration {
        Dimension request;
        Rectangle expect;

        public MockMapDecoration(Dimension toRequest, Rectangle toExpect) {
            this.request = toRequest;
            this.expect = toExpect;
        }

        public void loadOptions(Map<String,String> options) {}

        public Dimension findOptimalSize(Graphics2D g2d, WMSMapContext mapContext) {
            return this.request;
        }

        public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContext mapContext) 
        throws Exception {
            assertEquals("Calculated width matches expected", expect.width, paintArea.width);
            assertEquals("Calculated height matches expected", expect.height, paintArea.height);
        }
    }

    private Graphics2D createMockGraphics(int x, int y) {
        BufferedImage b = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
        return (Graphics2D)b.getGraphics();
    }

    public void testStaticSize() throws Exception {
        Graphics2D g2d = createMockGraphics(256, 256);
        MapDecorationLayout dl = new MapDecorationLayout();

        // if we have a component that calculates a size which fits in the image, 
        // does it get that size?
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(100, 100),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(0,0)
        ));

        // if we have a component with a user-mandated size which fits in the image, 
        // does it get that size?
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(100, 100),
                new Rectangle(0, 0, 50, 50)
            ),
            MapDecorationLayout.Block.Position.CR,
            new Dimension(50, 50),
            new Point(0,0)
        ));

        dl.paint(g2d, new Rectangle(0, 0, 256, 256), null);
    }

    public void testSquished() throws Exception {
        Graphics2D g2d = createMockGraphics(100, 100);
        MapDecorationLayout dl = new MapDecorationLayout();

        // for dynamically sized components, expect to be scaled to fit in view
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(150, 100),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(100, 150),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(150, 150),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(0,0)
        ));

        // for components with specified sizes, let them run off the edge. 
        // TODO: Should we preserve the aspect ratio for sized components that don't fit? 
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(150, 150),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            new Dimension(100, 150),
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(150, 150),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            new Dimension(150, 100),
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(150, 150),
                new Rectangle(0, 0, 100, 100)
            ),
            MapDecorationLayout.Block.Position.CR,
            new Dimension(150, 150),
            new Point(0,0)
        ));

        dl.paint(g2d, new Rectangle(0, 0, 100, 100), null);
    }

    public void testPosition() {
        Graphics2D g2d = createMockGraphics(100, 100);
        MapDecorationLayout dl = new MapDecorationLayout();

        // try all the position variants to verify that they actually put the block in the right 
        // position
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(0, 0, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UL,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 0, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UC,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(90, 0, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UR,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(0, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CL,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CC,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(90, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(0, 90, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LL,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 90, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LC,
            null,
            new Point(0,0)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(90, 90, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LR,
            null,
            new Point(0,0)
        ));

        dl.paint(g2d, new Rectangle(0, 0, 100, 100), null);
    }

    public void testOffset() {
        Graphics2D g2d = createMockGraphics(100, 100);
        MapDecorationLayout dl = new MapDecorationLayout();

        // try offsets with differing positions 
        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(10, 10, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UL,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 10, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UC,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(80, 10, 10, 10)
            ),
            MapDecorationLayout.Block.Position.UR,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(10, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CL,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CC,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(80, 45, 10, 10)
            ),
            MapDecorationLayout.Block.Position.CR,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(10, 80, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LL,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(45, 80, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LC,
            null,
            new Point(10,10)
        ));

        dl.addBlock(new MapDecorationLayout.Block(
            new MockMapDecoration(
                new Dimension(10, 10),
                new Rectangle(80, 80, 10, 10)
            ),
            MapDecorationLayout.Block.Position.LR,
            null,
            new Point(10,10)
        ));

        dl.paint(g2d, new Rectangle(0, 0, 100, 100), null);
    }
}
