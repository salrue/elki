package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.RangeSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Factory for visualizers to generate an SVG-Element containing a cube as
 * marker representing the selected range for each dimension
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionCubeVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Range";

  /**
   * Constructor
   */
  public SelectionCubeVisualizer() {
    super(NAME);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  @Override
  public void init(VisualizerContext<? extends NV> context) {
    super.init(context);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new SelectionCubeVisualization<NV>(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
  }

  /**
   * Visualizer for generating an SVG-Element containing a cube as marker
   * representing the selected range for each dimension
   * 
   * @author Heidi Kolb
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class SelectionCubeVisualization<NV extends NumberVector<NV, ?>> extends Projection2DVisualization<NV> implements ContextChangeListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String MARKER = "selectionCubeMarker";

    /**
     * CSS class for the filled cube
     */
    public static final String CSS_CUBE = "selectionCube";

    /**
     * CSS class for the cube frame
     */
    public static final String CSS_CUBEFRAME = "selectionCubeFrame";

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public SelectionCubeVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA - 1);
      addCSSClasses(svgp);
      context.addContextChangeListener(this);
      incrementalRedraw();
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the cube
      if(!svgp.getCSSClassManager().contains(CSS_CUBE)) {
        CSSClass cls = new CSSClass(this, CSS_CUBE);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.15");
        try {
          svgp.getCSSClassManager().addClass(cls);
        }
        catch(CSSNamingConflict e) {
          de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        }
      }
      // Class for the cube frame
      if(!svgp.getCSSClassManager().contains(CSS_CUBEFRAME)) {
        CSSClass cls = new CSSClass(this, CSS_CUBEFRAME);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, SVGConstants.CSS_BLUE_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, "0.5");
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, "0.3");

        try {
          svgp.getCSSClassManager().addClass(cls);
        }
        catch(CSSNamingConflict e) {
          de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        }
      }
    }

    /**
     * Generates a cube and a frame depending on the selection stored in the
     * context
     * 
     * @param svgp The plot
     * @param proj The projection
     */
    private void setSVGRect(SVGPlot svgp, VisualizationProjection proj) {
      DBIDSelection selContext = context.getSelection();
      if(selContext instanceof RangeSelection) {
        DoubleDoublePair[] ranges = ((RangeSelection) selContext).getRanges();
        int dim = context.getDatabase().dimensionality();

        double[] min = new double[dim];
        double[] max = new double[dim];
        for(int d = 0; d < dim; d++) {
          if(ranges != null && ranges[d] != null) {
            min[d] = ranges[d].first;
            max[d] = ranges[d].second;
          }
          else {
            min[d] = proj.getScale(d + 1).getMin();
            max[d] = proj.getScale(d + 1).getMax();
          }
        }
        {
          Element r = SVGHyperCube.drawFilled(svgp, CSS_CUBE, proj, min, max);
          SVGUtil.setCSSClass(r, CSS_CUBE);
          layer.appendChild(r);
        }
        {
          Element r = SVGHyperCube.drawFrame(svgp, proj, min, max);
          SVGUtil.setCSSClass(r, CSS_CUBEFRAME);
          layer.appendChild(r);
        }
      }
    }

    @Override
    protected boolean testRedraw(ContextChangedEvent e) {
      return super.testRedraw(e) || (e instanceof SelectionChangedEvent);
    }

    @Override
    protected void redraw() {
      DBIDSelection selContext = context.getSelection();
      if(selContext != null && selContext instanceof RangeSelection) {
        setSVGRect(svgp, proj);
      }
    }
  }
}