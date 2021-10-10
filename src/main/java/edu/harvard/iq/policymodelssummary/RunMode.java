package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.io.PolicyModelDataParser;
import edu.harvard.iq.policymodels.io.PolicyModelLoadingException;
import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.metadata.PolicyModelData;
import edu.harvard.iq.policymodels.parser.PolicyModelLoadResult;
import edu.harvard.iq.policymodels.parser.PolicyModelLoader;
import java.nio.file.Path;

/**
 * A convenience class for writing run modes (e.g. diff, summary).
 *
 * @author michael
 */
public abstract class RunMode {
  
    protected ConsoleOut o = new ConsoleOut();
    
    protected RunMode( String name ) {
        o.setStage(name);
    }
    
    
    protected PolicyModel loadModel( Path mpdelPath ) throws PolicyModelLoadingException {
        PolicyModelDataParser pmdParser = new PolicyModelDataParser();
        final PolicyModelData modelData = pmdParser.read(mpdelPath);

        if ( modelData == null ) {
            o.println("Model loading failed.");
            return null;
        }

        PolicyModelLoadResult loadRes = PolicyModelLoader.verboseLoader().load(modelData);

        if ( loadRes.isSuccessful() ) {
            o.println(String.format("Model '%s' loaded\n", loadRes.getModel().getMetadata().getTitle()));
        } else {
            o.println("Failed to load model");
            loadRes.getMessages().forEach( m -> o.println(m.getLevel().toString() + " " + m.getMessage()));
        }
        return loadRes.getModel();
    }
    
}
