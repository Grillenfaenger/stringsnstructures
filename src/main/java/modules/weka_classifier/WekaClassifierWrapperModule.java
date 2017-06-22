package modules.weka_classifier;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
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
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
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
	
	private final static String INPUT_BOW_ID = "bag of words";
	private final static String INPUT_BOW_DESC = "[text/json] TreeMap<Integer,TreeMap<String,Integer>>";
	private final static String INPUT_ARTICLES_ID = "xls";
	private final static String INPUT_ARTICLES_DESC = "[text/json] Article texts from xls reader";
	private final static String INPUT_TEXT_ID = "text";
	private final static String INPUT_TEXT_DESC = "[text/plain] A newline separated List of texts.";
	private final static String OUTPUT_ID = "json";
	private final static String OUTPUT_DESC = "[text/json] TreeMap<Integer,ZoneClassifyUnit>";	
	
	private final static String OUTPUT_SIMPLE_ID = "json";
	private final static String OUTPUT_SIMPLE_DESC = "[text/json] An ID: class representation of the classification (class: TreeMap&lt;Integer,Integer&gt;)";
		
	private final static Type INPUT_TYPE = new TypeToken<TreeMap<Integer, TreeMap<String, Integer>>>() {
	}.getType();
	private final static Type INPUT_ARTICLE_TYPE = new TypeToken<TreeMap<Integer,String>>() {
	}.getType();
	private final static Type OUTPUT_SIMPLE_TYPE = new TypeToken<TreeMap<Integer, Integer>>() {
	}.getType();
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String CLASSIFIERMETHOD = "method";
	public static final String PROPERTYKEY_TRAININGDATAFILE = "trainingdatafile";

	
	// Define I/O IDs (must be unique for every input or output)
	
	// Local variables
	private String trainingdatafile;
	private String classifiermethod;

	public WekaClassifierWrapperModule(CallbackReceiver callbackReceiver, Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add description
		this.setDescription("Weka Classifier Wrapper Module. Classifies input texts respective to training data and classifier methods (u.a. Weka Classifiers)");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(CLASSIFIERMETHOD, "NB (Naive Bayes, default), KNN, R (Rocchio) or SVM (Support Vector Machine");
		this.getPropertyDescriptions().put(PROPERTYKEY_TRAININGDATAFILE, "Path to classified articles to train classifier");
		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Weka Classifier Wrapper Module"); // Property key for module name is defined in parent class
		this.getPropertyDefaultValues().put(CLASSIFIERMETHOD, "NB");
		this.getPropertyDefaultValues().put(PROPERTYKEY_TRAININGDATAFILE, "/Strings/src/test/resources/trainingData.json");
	
		
		// Define I/O
		InputPort textPort = new InputPort(INPUT_TEXT_ID, "newline seperated texts.", this);
		textPort.addSupportedPipe(CharPipe.class);
		InputPort bowPort = new InputPort(INPUT_BOW_ID, "bag of words input.", this);
		bowPort.addSupportedPipe(CharPipe.class);
		InputPort articlePort = new InputPort(INPUT_ARTICLES_ID, "Article texts from xls reader.", this);
		articlePort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(OUTPUT_SIMPLE_ID, "Plain text character output.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(textPort);
		super.addInputPort(bowPort);
		super.addInputPort(articlePort);
		super.addOutputPort(outputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		TreeMap<Integer,Integer> result = new TreeMap<Integer,Integer>();
		
		// A Gson object to serialize and deserialize with
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		final InputPort textPort = this.getInputPorts().get(INPUT_TEXT_ID);
		final InputPort bowPort = this.getInputPorts().get(INPUT_BOW_ID);
		final InputPort articlePort = this.getInputPorts().get(INPUT_ARTICLES_ID);
		
		// the output: an empty result map mapping sentence Nrs to a map holding
		// the distance of this sentence to each other sentence
		
		if (bowPort.isConnected() && articlePort.isConnected() || bowPort.isConnected() && textPort.isConnected() || articlePort.isConnected() && textPort.isConnected()) {
			throw new Exception("Use only one connected input.");
		} else if (textPort.isConnected()){
			final String text_input = this.readStringFromInputPort(this.getInputPorts().get(INPUT_TEXT_ID));
			
			final String[] sentences = Pattern.compile("\r\n|\n|\r").split(text_input);
			
			TreeMap<Integer,String> texts = new TreeMap<Integer,String>();
			for (Integer i = 0; i < sentences.length; i++) {
				texts.put(i, sentences[i]);
			}
			
			WekaClassifier classifier = new WekaClassifier(texts, new File(trainingdatafile), classifiermethod);
			final TreeMap<Integer, ZoneClassifyUnit> classified = classifier.classify();
			
			HashMap<Integer,Integer> resultH = new HashMap<Integer,Integer>();
			for(Integer key : classified.keySet()){
				resultH.put(key, classified.get(key).getActualClassID());
			}
			result.putAll(resultH);
			
		} else if (bowPort.isConnected()) {
			final String bow_input = this.readStringFromInputPort(this.getInputPorts().get(INPUT_BOW_ID));
			final TreeMap<Integer, Map<String, Integer>> sentenceNrsToBagOfWords = gson.fromJson(bow_input, INPUT_TYPE);

			// make sure that the bags of words are ready to use
			if (sentenceNrsToBagOfWords.size() == 0) {
				throw new Exception("No bags of words given");
			}
			final TreeMap<Integer,String> bowStrings = buildBowString(sentenceNrsToBagOfWords);
	
			// main functionality
			WekaClassifier classifier = new WekaClassifier(bowStrings, new File(trainingdatafile), classifiermethod);
			final TreeMap<Integer, ZoneClassifyUnit> classified = classifier.classify();
			
			HashMap<Integer,Integer> resultH = new HashMap<Integer,Integer>();
			for(Integer key : classified.keySet()){
				resultH.put(key, classified.get(key).getActualClassID());
			}
			result.putAll(resultH);
			
		} else if (articlePort.isConnected()){
			final String article_input = this.readStringFromInputPort(this.getInputPorts().get(INPUT_ARTICLES_ID));
			final TreeMap<Integer,String> texts = gson.fromJson(article_input, INPUT_ARTICLE_TYPE);
			
			
			System.out.println("6297: "+texts.get(6297));
//			TreeMap<Integer,String> texts = new TreeMap<Integer,String>();
//			for (Integer i = 0; i < articles.size(); i++) {
//				texts.put(i, articles.get(i).getTitle() + " " + articles.get(i).getContent());
//			}
			
			WekaClassifier classifier = new WekaClassifier(texts, new File(trainingdatafile), classifiermethod);
			final TreeMap<Integer, ZoneClassifyUnit> classified = classifier.classify();
			
			HashMap<Integer,Integer> resultH = new HashMap<Integer,Integer>();
			for(Integer key : classified.keySet()){
				resultH.put(key, classified.get(key).getActualClassID());
			}
			result.putAll(resultH);
			
			System.out.println(result.get(6297));
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
	
	private TreeMap<Integer, String> buildBowString(
			TreeMap<Integer, Map<String, Integer>> sentenceNrsToBagOfWords) {
		TreeMap<Integer,String> sMap = new TreeMap<Integer,String>();
		StringBuffer sb = new StringBuffer();
			for(Integer key : sentenceNrsToBagOfWords.keySet()){
				Map<String, Integer> article = sentenceNrsToBagOfWords.get(key); 
				for(String s : article.keySet()){
					for(int i = 0; i<article.get(s); i++){
						sb.append(s);
						sb.append(" ");
					}
				}
				System.out.println(sb.toString());
				sMap.put(key, sb.toString());
			}
		return sMap;
	}

}
