package modules.weka_classifier;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import base.workbench.ModuleRunner;
import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;
import common.parallelization.CallbackReceiver;
import de.uni_koeln.spinfo.classification.core.featureEngineering.FeatureUnitTokenizer;
import de.uni_koeln.spinfo.stocknews.articles.data.Article;
import de.uni_koeln.spinfo.stocknews.articles.processing.RicProcessing;
import de.uni_koeln.spinfo.stocknews.evaluation.data.TrainingDataCollection;
import de.uni_koeln.spinfo.stocknews.stocks.data.Trend;

public class ResolveClassificationModule extends ModuleImpl {
	
	public static final Pattern ricPattern = Pattern.compile("[A-Z]{1,4}\\.[A-Z]{1,2}");
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String PROPERTYKEY_TRAININGDATA = "training data";
	
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT_CLASSIFICATON = "Result of WekaClassifier Wrapper module";
	private static final String ID_INPUT_TEXT = "text input";
	private static final String ID_INPUT_ARTICLES = "articles";
	private static final String ID_OUTPUT = "output";
	
	private final static Type INPUT_CLASSIFICATION_TYPE = new TypeToken<TreeMap<Integer, Integer>>() {
	}.getType();
	private final static Type INPUT_ARTICLE_TYPE = new TypeToken<TreeMap<Integer,String>>() {
	}.getType();
	private final static Type OUTPUT_TYPE = new TypeToken<List<PrettyResult>>() {
	}.getType();
	
	// Local variables
	private String trainingdataFilepath;
	private Map<Integer, Trend> classDefinition;
	private Set<String> coveredRics;
	
	public ResolveClassificationModule(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add module description
		this.setDescription("<p>Resolves classification from WekaClassifierWrapper into readable form by reassociating it with the input text</p>");
		
		// You can override the automatic category selection (for example if a module is to be shown in "deprecated")
		//this.setCategory("Examples");

		// the module's name is defined as a property
		// Property key for module name is defined in parent class
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Resolve Classification Module");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_TRAININGDATA, "Path to training data file containing class definition metadata");

		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(PROPERTYKEY_TRAININGDATA, "/Strings/src/test/resources/trainingData.json");

		
		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~).
		 * Every port can support a range of pipe types (currently
		 * byte or character pipes). Output ports can provide data
		 * to multiple pipe instances at once, input ports can
		 * in contrast only obtain data from one pipe instance.
		 */
		InputPort classificationPort = new InputPort(ID_INPUT_CLASSIFICATON, "Result of WekaClassifier Wrapper.", this);
		classificationPort.addSupportedPipe(CharPipe.class);
		InputPort textPort = new InputPort(ID_INPUT_TEXT, "Plain text character input.", this);
		textPort.addSupportedPipe(CharPipe.class);
		InputPort articlePort = new InputPort(ID_INPUT_ARTICLES, "json article objects.", this);
		articlePort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(ID_OUTPUT, "Plain text character output.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(classificationPort);
		super.addInputPort(textPort);
		super.addInputPort(articlePort);
		super.addOutputPort(outputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		final InputPort textPort = this.getInputPorts().get(ID_INPUT_TEXT);
		final InputPort articlesPort = this.getInputPorts().get(ID_INPUT_ARTICLES);
		TreeMap<Integer,String> articles = new TreeMap<Integer,String>();

		if (textPort.isConnected() && articlesPort.isConnected()) {
			throw new Exception("Either text input or article input has to be connected, not both..");
		} else if (textPort.isConnected()){
			final String text = this.readStringFromInputPort(this.getInputPorts().get(ID_INPUT_TEXT));
			final String[] sentencesBuff = Pattern.compile("\r\n|\n|\r").split(text);
			
			for (int i = 0; i < sentencesBuff.length; i++) {
				articles.put(i, sentencesBuff[i]);
			}
		} else if (articlesPort.isConnected()){
			final String article_input = this.readStringFromInputPort(this.getInputPorts().get(ID_INPUT_ARTICLES));
			articles = gson.fromJson(article_input, INPUT_ARTICLE_TYPE);
			
//			String[] sentencesBuff = new String[articles.size()];
//			for (Integer i = 0; i < articles.size(); i++) {
//				sentencesBuff[i] = articles.get(i).getTitle() + " " + articles.get(i).getContent();
//			}
//			sentences = sentencesBuff;
			//TODO
		}
		
		final String classifiedString = this.readStringFromInputPort(this.getInputPorts().get(ID_INPUT_CLASSIFICATON));
		final TreeMap<Integer, Integer> classified = gson.fromJson(classifiedString, INPUT_CLASSIFICATION_TYPE);
	
		try (Reader reader = new FileReader(trainingdataFilepath)) {

			// Convert JSON to Java Object
			 
			TrainingDataCollection tdColl = gson.fromJson(reader, TrainingDataCollection.class);
			this.classDefinition = invert(tdColl.getClasses());
			this.coveredRics = tdColl.getCoveredRics();
		}
		List<PrettyResult> results = new ArrayList<PrettyResult>();
		for(int key : classified.keySet()){
			Set<String> extractedTags = RicProcessing.extractTags(articles.get(key));
			extractedTags.addAll(findRics(articles.get(key)));
			PrettyResult res = new PrettyResult(key,extractedTags,classDefinition.get(classified.get(key)),articles.get(key));
			System.out.println(res);
			results.add(res);
		}
		final String jsonOut = gson.toJson(results, OUTPUT_TYPE);
		this.getOutputPorts().get(ID_OUTPUT).outputToAllCharPipes(jsonOut);
		
		// Close outputs (important!)
		this.closeAllOutputs();
		
		// Done
		return true;
	}
	
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		// Apply own properties
		trainingdataFilepath = this.getProperties().getProperty(PROPERTYKEY_TRAININGDATA, this.getPropertyDefaultValues().get(PROPERTYKEY_TRAININGDATA));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}
	
	private <V, K> Map<V, K> invert(Map<K, V> map) {

	    Map<V, K> inv = new HashMap<V, K>();

	    for (Entry<K, V> entry : map.entrySet())
	        inv.put(entry.getValue(), entry.getKey());

	    return inv;
	}
	
	private Set<String> findRics(String text){
		Set<String> rics = new TreeSet<String>();
		
		Matcher matcher = ricPattern.matcher(text);
		while(matcher.find()){
			rics.add(matcher.group().trim());
		}
		return rics;
	}

	class PrettyResult{
		
		int id;
		Set<String> rics;
		Trend trend;
		String content;
		
		public PrettyResult(int id, Set<String> rics, Trend trend, String content) {
			super();
			this.id = id;
			this.rics = rics;
			this.trend = trend;
			this.content = content;
		}

		@Override
		public String toString() {
			return "Result [id=" + id + ", rics=" + rics + ", trend=" + trend
					+ ", content=" + content + "]";
		}
		
		
	}

}
