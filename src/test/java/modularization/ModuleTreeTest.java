package modularization;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import modules.ModuleImpl;
import modules.ModuleTree;
import modules.basemodules.ConsoleWriterModule;
import modules.basemodules.FileWriterModule;
import modules.oanc.OANC;
import modules.oanc.OANCXMLParser;

import org.junit.Test;

public class ModuleTreeTest {

	@Test
	public void test() throws Exception {
		
		String oancLoc0 = "src"+File.separator+"test"+File.separator+"data"+File.separator;
		
		File eingabeVerzeichnis = new File(oancLoc0);
		assertTrue(eingabeVerzeichnis.exists() && eingabeVerzeichnis.isDirectory());
		System.out.println("Eingabeverzeichnis "+oancLoc0+" existiert.");
		
		String outputFileLocation = System.getProperty("java.io.tmpdir")+File.separator+"test.txt";
		System.out.println("Schreibe Testausgabe nach "+outputFileLocation);
		
		// Set up module tree
		ModuleTree moduleTree = new ModuleTree();
		
		// Prepare OANC module
		Properties oancProperties = new Properties();
		oancProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "OANC");
		oancProperties.setProperty(OANC.PROPERTYKEY_OANCLOCATION, oancLoc0);
		OANC oanc = new OANC(moduleTree,oancProperties);
		
		moduleTree.setRootModule(oanc); // Necessary before adding more modules!
		
		// Prepare FileWriter module
		Properties fileWriterProperties = new Properties();
		fileWriterProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "FileWriter");
		fileWriterProperties.setProperty(FileWriterModule.PROPERTYKEY_OUTPUTFILE, outputFileLocation);
		fileWriterProperties.setProperty(FileWriterModule.PROPERTYKEY_ENCODING, "UTF-8");
		FileWriterModule fileWriter = new FileWriterModule(moduleTree,fileWriterProperties);
		
		// Prepare OANC parser module
		Properties oancParserProperties = new Properties();
		oancParserProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "OANC-Parser");
		oancParserProperties.setProperty(OANCXMLParser.PROPERTYKEY_ADDSTARTSYMBOL, Boolean.toString(true));
		oancParserProperties.setProperty(OANCXMLParser.PROPERTYKEY_ADDTERMINALSYMBOL, Boolean.toString(true));
		oancParserProperties.setProperty(OANCXMLParser.PROPERTYKEY_CONVERTTOLOWERCASE, Boolean.toString(true));
		oancParserProperties.setProperty(OANCXMLParser.PROPERTYKEY_KEEPPUNCTUATION, Boolean.toString(true));
		oancParserProperties.setProperty(OANCXMLParser.PROPERTYKEY_OUTPUTANNOTATEDJSON, Boolean.toString(true));
		OANCXMLParser oancParser = new OANCXMLParser(moduleTree,oancParserProperties);
		
		// Prepare FileReader module
		/*Properties fileReaderProperties = new Properties();
		fileReaderProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "FileReader");
		fileReaderProperties.setProperty(FileReaderModule.PROPERTYKEY_INPUTFILE, outputFileLocation);
		FileReaderModule fileReader = new FileReaderModule(moduleTree,fileReaderProperties);*/
		
		// Prepare ConsoleWriter module
		Properties consoleWriterProperties = new Properties();
		consoleWriterProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "ConsoleWriter");
		ConsoleWriterModule consoleWriter = new ConsoleWriterModule(moduleTree,consoleWriterProperties);
		
		// Prepare ExampleModule module
		/*Properties exampleModuleProperties = new Properties();
		exampleModuleProperties.setProperty(ModuleImpl.PROPERTYKEY_NAME, "Example Module");
		exampleModuleProperties.setProperty(ExampleModule.PROPERTYKEY_REGEX, "[aeiu]");
		exampleModuleProperties.setProperty(ExampleModule.PROPERTYKEY_REPLACEMENT, "o");
		ExampleModule exampleModule = new ExampleModule(moduleTree, exampleModuleProperties);*/
		
		// Add modules to tree
		moduleTree.addModule(oancParser, oanc);
		moduleTree.addModule(fileWriter, oancParser);
		moduleTree.addModule(consoleWriter, oancParser);
		//moduleTree.addModule(exampleModule, oancParser);
		//moduleTree.addModule(consoleWriter, exampleModule);
		
		// Print tree
		System.out.println(moduleTree.prettyPrint());
		
		
		// Run modules in tree
		System.out.println("Attempting to run module tree");
		moduleTree.runModules(true);
		
		assertTrue(true);
	}

}
