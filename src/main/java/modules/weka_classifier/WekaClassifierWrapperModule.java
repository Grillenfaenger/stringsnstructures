package modules.weka_classifier;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.PatternSyntaxException;

import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;













import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.parallelization.CallbackReceiver;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.stocknews.articles.data.Article;
import de.uni_koeln.spinfo.stocknews.classification.workflow.WekaClassifier;
import base.workbench.ModuleRunner;

public class WekaClassifierWrapperModule extends ModuleImpl {

	// Main method for stand-alone execution
	public static void main(String[] args) throws Exception {
		ModuleRunner.runStandAlone(WekaClassifierWrapperModule.class, args);
	}

	// Strings identifying/describing in- and output pipes
	
	private final static String INPUT_ID = "json";
	private final static String INPUT_ARTICLES_ID = "article objects in json";
	private final static String OUTPUT_ID = "json";
	private final static String INPUT_DESC = "[text/json] TreeMap<Integer,TreeMap<String,Integer>>";
	private final static String INPUT_ARTICLES_DESC = "[text/json] List<Article>";
	private final static String OUTPUT_DESC = "[text/json] TreeMap<Integer,ZoneClassifyUnit>";	
	
	private final static String OUTPUT_SIMPLE_ID = "json";
	private final static String OUTPUT_SIMPLE_DESC = "[text/json] Bags of Words (class: TreeMap&lt;Integer,Integer&gt;)";
		
	private final static Type INPUT_TYPE = new TypeToken<TreeMap<Integer, TreeMap<String, Integer>>>() {
	}.getType();
	private final static Type INPUT_ARTICLE_TYPE = new TypeToken<TreeMap<Integer, TreeMap<String, Integer>>>() {
	}.getType();
	private final static Type OUTPUT_SIMPLE_TYPE = new TypeToken<TreeMap<Integer, Integer>>() {
	}.getType();
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String CLASSIFIERMETHOD = "method";
	public static final String PROPERTYKEY_TRAININGDATAFILE = "trainingdatafile";
	//public static final String PROPERTYKEY_REPLACEMENT = "replacement";
	//public static final String PROPERTYKEY_UNESCAPE = "unescape";
	
	// Define I/O IDs (must be unique for every input or output)
//	private final String INPUTID = "input";
//	private final String OUTPUTID = "output";
	
	// Local variables
	private String trainingdatafile;
	private String classifiermethod;

	public WekaClassifierWrapperModule(CallbackReceiver callbackReceiver, Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add description
		this.setDescription("Regular expression text replacement module. Can use escape characters (e.g. '\\n') and backreferences (marked with '$', e.g. '$1').");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(CLASSIFIERMETHOD, "Regular expression to search for");
		this.getPropertyDescriptions().put(PROPERTYKEY_TRAININGDATAFILE, "Path to classified articles to train classifier");
		//this.getPropertyDescriptions().put(PROPERTYKEY_REPLACEMENT, "Replacement for found strings");
		//this.getPropertyDescriptions().put(PROPERTYKEY_UNESCAPE, "Perform unescape operation on the replacement string before using it [true|false]");
		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Weka Classifier Wrapper Module"); // Property key for module name is defined in parent class
		this.getPropertyDefaultValues().put(CLASSIFIERMETHOD, "NaiveBayes");
		this.getPropertyDefaultValues().put(PROPERTYKEY_TRAININGDATAFILE, "/StockNewsClassification/output/classification/trainingData.txt");
		//this.getPropertyDefaultValues().put(PROPERTYKEY_REPLACEMENT, "o");
		//this.getPropertyDefaultValues().put(PROPERTYKEY_UNESCAPE, "true");
		
		// Define I/O
		InputPort bowPort = new InputPort(INPUT_ID, "bag of words input.", this);
		bowPort.addSupportedPipe(CharPipe.class);
		InputPort articlePort = new InputPort(INPUT_ARTICLES_ID, "json article objects.", this);
		articlePort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(OUTPUT_ID, "Plain text character output.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(bowPort);
		super.addInputPort(articlePort);
		super.addOutputPort(outputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		TreeMap<Integer,Integer> result = new TreeMap<Integer,Integer>();
		
		// A Gson object to serialize and deserialize with
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		final InputPort bowPort = this.getInputPorts().get(INPUT_ID);
		final InputPort articlePort = this.getInputPorts().get(INPUT_ARTICLES_ID);
		
		// the output: an empty result map mapping sentence Nrs to a map holding
		// the distance of this sentence to each other sentence
		
		if (bowPort.isConnected() && articlePort.isConnected()) {
			throw new Exception("Either simple input or article input has to be connected, not both.");
		} else if (bowPort.isConnected()) {
			final String bow_input = this.readStringFromInputPort(this.getInputPorts().get(INPUT_ID));
			final TreeMap<Integer, Map<String, Integer>> sentenceNrsToBagOfWords = gson.fromJson(bow_input, INPUT_TYPE);
			final Set<Integer> sentenceNrs = sentenceNrsToBagOfWords.keySet();

			System.out.println(sentenceNrsToBagOfWords);
			// make sure that the bags of words are ready to use
			if (sentenceNrs.size() == 0) {
				throw new Exception("No bags of words given");
			}
			
			// main functionality
			WekaClassifier classifier = new WekaClassifier(sentenceNrsToBagOfWords, new File(trainingdatafile), classifiermethod);
			TreeMap<Integer, ZoneClassifyUnit> classified = classifier.classify();
			
			HashMap<Integer,Integer> resultH = new HashMap<Integer,Integer>();
			for(Integer key : classified.keySet()){
				resultH.put(key, classified.get(key).getActualClassID());
			}
			
			result.putAll(resultH);
		} else if (articlePort.isConnected()){
			final String article_input = this.readStringFromInputPort(this.getInputPorts().get(INPUT_ARTICLES_ID));
			final List<Article> articles = gson.fromJson(article_input, INPUT_ARTICLE_TYPE);
			
			//TODO
			/*hier weiter:
			 * Etwas anderer Classifier, bzw. anderer Konstruktor
			 *
			 */
		
		}
		
		
		// serialize the classification result to json and flush it to the output
		// ports
		final String jsonOut = gson.toJson(result, OUTPUT_SIMPLE_TYPE);
		this.getOutputPorts().get(OUTPUT_SIMPLE_ID).outputToAllCharPipes(jsonOut);
		
		// Close outputs (important!)
		this.closeAllOutputs();
		
		// Done
		return true;
	}
	
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		//TODO Change!
		// Apply own properties
		this.classifiermethod = this.getProperties().getProperty(CLASSIFIERMETHOD, this.getPropertyDefaultValues().get(CLASSIFIERMETHOD));
		this.trainingdatafile = this.getProperties().getProperty(PROPERTYKEY_TRAININGDATAFILE, this.getPropertyDefaultValues().get(PROPERTYKEY_TRAININGDATAFILE));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}
