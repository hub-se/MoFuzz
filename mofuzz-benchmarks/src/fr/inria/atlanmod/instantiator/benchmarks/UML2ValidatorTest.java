package fr.inria.atlanmod.instantiator.benchmarks;

import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.util.UMLValidator;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import fr.inria.atlanmod.instantiator.AtlanModUMLGenerator;
import de.hub.mse.emf.fuzz.JQFModelFuzzer;

import edu.berkeley.cs.jqf.fuzz.Fuzz;

@RunWith(JQFModelFuzzer.class)
public class UML2ValidatorTest {

    @Fuzz
    public void test(@From(AtlanModUMLGenerator.class) Resource resource) { 	
    	BasicDiagnostic diagnosticChain = new BasicDiagnostic();
    	for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			EObject eObject = it.next();
			Diagnostician.INSTANCE.validate(eObject, diagnosticChain);
			UMLValidator.INSTANCE.validate(eObject, diagnosticChain, null);
		}
    }
    
}