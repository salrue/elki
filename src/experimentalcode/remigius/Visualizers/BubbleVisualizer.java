package experimentalcode.remigius.Visualizers;

import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.lisa.scale.DoubleScale;
import experimentalcode.lisa.scale.GammaFunction;
import experimentalcode.lisa.scale.LinearScale;
import experimentalcode.remigius.CommonSVGShapes;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

public class BubbleVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {
	
	public static final OptionID GAMMA_ID = OptionID.getOrCreateOptionID("bubble.gamma", "gamma-correction.");
	private final DoubleParameter GAMMA_PARAM = new DoubleParameter(GAMMA_ID);

	private DoubleScale normalizationScale;
	private DoubleScale plotScale;
	private GammaFunction gammaFunction;

	private AnnotationResult<Double> anResult;
	private Result result;

	private Clustering<Model> clustering;

	public BubbleVisualizer(Database<O> database, AnnotationResult<Double> anResult, Result r, DoubleScale normalizationScale, VisualizationManager<O> v){
		super(database, v);
		
		addOption(GAMMA_PARAM);
		
		this.anResult = anResult;

		this.normalizationScale = normalizationScale;
		this.plotScale = new LinearScale();
		this.gammaFunction = new GammaFunction();
		
		setupClustering();
		setupCSS();
	}

	private void setupClustering(){
		List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);

		if (clusterings != null && clusterings.size() > 0) {
			clustering = (Clustering<Model>) clusterings.get(0);
		} else {
			clustering = new ByLabelClustering<O>().run(database);
		}
	}

	private void setupCSS(){

		Iterator<Cluster<Model>> iter = clustering.getAllClusters().iterator();
		int clusterID = 0;

		while (iter.hasNext()){

			// just need to consume a cluster; creating IDs manually because cluster often return a null-ID.
			Cluster<Model> cluster = iter.next();
			clusterID+=1;

			CSSClass bubble = visManager.createCSSClass(CommonSVGShapes.CSS_BUBBLE_PREFIX + clusterID);
			bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, "0.001");

			// fill bubbles
			bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, COLORS.getColor(clusterID));
			bubble.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.5");

			// or don't fill them.
//							bubble.setStatement(SVGConstants.CSS_STROKE_VALUE, COLORS.getColor(clusterID));
//							bubble.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");

			visManager.registerCSSClass(bubble);
		}
	}
	
	@Override
	public List<String> setParameters(List<String> args) throws ParameterException {
		List<String> remainingParameters = super.setParameters(args);
		gammaFunction.setGamma(GAMMA_PARAM.getValue());
		return remainingParameters;
	}

	private Double getValue(int id){
		return anResult.getValueFor(id);
	}

	private Double getScaled(Double d){
		return gammaFunction.getScaled(plotScale.getScaled(normalizationScale.getScaled(d)));
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx, int dimy) {

		Iterator<Cluster<Model>> iter = clustering.getAllClusters().iterator();
		int clusterID = 0;

		while (iter.hasNext()){
			Cluster<Model> cluster = iter.next();
			clusterID+=1;
			for (int id : cluster.getIDs()){
				layer.appendChild(
						SHAPEGEN.createBubble(svgp.getDocument(), getPositioned(database.get(id), dimx), 1 - getPositioned(database.get(id), dimy),
								getScaled(getValue(id)), clusterID, id, dimx, dimy)
				);
			}
		}

		return new NumberVisualization(dimx, dimy, layer);
	}
	
	public String toString(){
		return "Bubbles";
	}
}
