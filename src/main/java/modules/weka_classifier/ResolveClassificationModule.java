package modules.weka_classifier;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
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
import de.uni_koeln.spinfo.stocknews.articles.processing.RicProcessing;
import de.uni_koeln.spinfo.stocknews.evaluation.data.TrainingDataCollection;
import de.uni_koeln.spinfo.stocknews.stocks.data.Trend;

public class ResolveClassificationModule extends ModuleImpl {
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String PROPERTYKEY_TRAININGDATA = "training data";
//	public static final String PROPERTYKEY_DELIMITER_B = "delimiter B";
//	public static final String PROPERTYKEY_DELIMITER_OUTPUT = "delimiter out";
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT_CLASSIFICATON = "Result of the WekaClassifier Wrapper";
	private static final String ID_INPUT_TEXT = "initial input file of the Classification";
	private static final String ID_OUTPUT = "output";
//	private static final String ID_OUTPUT_ENTWINED_CAPITALISED = "capitals";
	
	private final static Type INPUT_CLASSIFICATION_TYPE = new TypeToken<TreeMap<Integer, Integer>>() {
	}.getType();
	private final static Type OUTPUT_TYPE = new TypeToken<List<PrettyResult>>() {
	}.getType();
	
	// Local variables
	private String trainingdataFilepath;
	private Map<Integer, Trend> classDefinition;
	private List<String> coveredRics;
//	private String inputdelimiter_b;
//	private String outputdelimiter;

//	// Main method for stand-alone execution
//	public static void main(String[] args) throws Exception {
//		ModuleRunner.runStandAlone(ResolveClassificationModule.class, args);
//	}
	
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
//		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Example Module");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_TRAININGDATA, "Path to training data file containing class definition metadata");

		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Resolve Classification Module");
		this.getPropertyDefaultValues().put(PROPERTYKEY_TRAININGDATA, "C:/Users/avogt/git/SNC/StockNewsClassification/output/classification/trainingDataTEST2.json");

		
		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~).
		 * Every port can support a range of pipe types (currently
		 * byte or character pipes). Output ports can provide data
		 * to multiple pipe instances at once, input ports can
		 * in contrast only obtain data from one pipe instance.
		 */
		InputPort inputPort1 = new InputPort(ID_INPUT_CLASSIFICATON, "Result of WekaClassifier Wrapper.", this);
		inputPort1.addSupportedPipe(CharPipe.class);
		InputPort inputPort2 = new InputPort(ID_INPUT_TEXT, "Plain text character input.", this);
		inputPort2.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(ID_OUTPUT, "Plain text character output.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(inputPort1);
		super.addInputPort(inputPort2);
		super.addOutputPort(outputPort);
//		super.addOutputPort(capsOutputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		final String text = this.readStringFromInputPort(this.getInputPorts().get(ID_INPUT_TEXT));
		final String[] sentences = Pattern.compile("\r\n|\n|\r").split(text);
		
		final String classifiedString = this.readStringFromInputPort(this.getInputPorts().get(ID_INPUT_CLASSIFICATON));
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final TreeMap<Integer, Integer> classified = gson.fromJson(classifiedString, INPUT_CLASSIFICATION_TYPE);
	
		try (Reader reader = new FileReader(trainingdataFilepath)) {

			// Convert JSON to Java Object
			 
			TrainingDataCollection tdColl = gson.fromJson(reader, TrainingDataCollection.class);
			this.classDefinition = invert(tdColl.getClasses());
			this.coveredRics = tdColl.getCoveredRics();
		}
		List<PrettyResult> results = new ArrayList<PrettyResult>();
		for(int key : classified.keySet()){
			List<String> extractedTags = RicProcessing.extractTags(sentences[key]);
			PrettyResult res = new PrettyResult(key,extractedTags,classDefinition.get(classified.get(key)),sentences[key]);
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

	class PrettyResult{
		
		int id;
		List<String> rics;
		Trend trend;
		String content;
		
		public PrettyResult(int id, List<String> rics, Trend trend, String content) {
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
