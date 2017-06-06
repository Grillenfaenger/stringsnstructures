package modules.weka_classifier;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;
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

public class ResolveClassificationModule extends ModuleImpl {
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String PROPERTYKEY_CLASSES_DEF = "classes definition";
//	public static final String PROPERTYKEY_DELIMITER_B = "delimiter B";
//	public static final String PROPERTYKEY_DELIMITER_OUTPUT = "delimiter out";
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT_CLASSIFICATON = "Result of the WekaClassifier Wrapper";
	private static final String ID_INPUT_TEXT = "initial input file of the Classification";
	private static final String ID_OUTPUT = "output";
//	private static final String ID_OUTPUT_ENTWINED_CAPITALISED = "capitals";
	
	private final static Type INPUT_CLASSIFICATION_TYPE = new TypeToken<TreeMap<Integer, Integer>>() {
	}.getType();
	
	// Local variables
	private String classDefinition;
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
		this.getPropertyDescriptions().put(PROPERTYKEY_CLASSES_DEF, "Path to class definition file");

		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Resolve Classification Module");
		this.getPropertyDefaultValues().put(PROPERTYKEY_CLASSES_DEF, "C:/Users/avogt/git/SNC/StockNewsClassification/output/classification/trainingDataTEST_classes.txt");

		
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
	
		for(int key : classified.keySet()){
			// TODO	Programmlogik
//			sentences[key];
		}
		
		
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
		this.classDefinition = this.getProperties().getProperty(PROPERTYKEY_CLASSES_DEF, this.getPropertyDefaultValues().get(PROPERTYKEY_CLASSES_DEF));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}
