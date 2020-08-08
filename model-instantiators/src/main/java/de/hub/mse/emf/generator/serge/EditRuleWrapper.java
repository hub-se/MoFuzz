package de.hub.mse.emf.generator.serge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.model.HenshinPackage;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResource;
import org.eclipse.emf.henshin.model.resource.HenshinResourceFactory;

public class EditRuleWrapper {

	public enum OperationKind {
		CREATE, ADD, SET_REFERENCE, MOVE, SET_ATTRIBUTE, UNSET_REFERENCE, REMOVE, DELETE
	}

	private String rulebase; // cpeo or basic
	
	private ResourceSetImpl henshinResourceSet;

	private Map<OperationKind, Set<Rule>> rulesByKind;
	private Set<Rule> allConstructRules; // CREATE, ADD, SET_REFERENCE
	private Set<Rule> allChangeRules; // MOVE, SET_ATTRIBUTE
	private Set<Rule> allDestructRules; // DELETE, REMOVE, UNSET_REFERENCE
	private Set<Rule> allRules;

	public EditRuleWrapper(String rulebase) {
		this.rulebase = rulebase;
		
		this.rulesByKind = new HashMap<OperationKind, Set<Rule>>();
		rulesByKind.put(OperationKind.CREATE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.ADD, new HashSet<Rule>());
		rulesByKind.put(OperationKind.SET_REFERENCE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.MOVE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.SET_ATTRIBUTE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.UNSET_REFERENCE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.REMOVE, new HashSet<Rule>());
		rulesByKind.put(OperationKind.DELETE, new HashSet<Rule>());

		this.allConstructRules = new HashSet<Rule>();
		this.allChangeRules = new HashSet<Rule>();
		this.allDestructRules = new HashSet<Rule>();
		this.allRules = new HashSet<Rule>();

		// Initialize Henshin (Henshin rules are just EMF models, too)
		this.henshinResourceSet = new ResourceSetImpl();

		this.henshinResourceSet.getPackageRegistry().put(HenshinPackage.eNS_URI, HenshinPackage.eINSTANCE);
		this.henshinResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(HenshinResource.FILE_EXTENSION,
				new HenshinResourceFactory());

		HenshinPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(HenshinPackage.eINSTANCE);

		// Load all the rules from *.henshin files
		String ruleBase = new File("").getAbsolutePath() + "/editrules/" + this.rulebase;
		File ruleFolder = new File(ruleBase);
		File[] ruleDirs = ruleFolder.listFiles();
		for (int i = 0; i < ruleDirs.length; i++) {
			String[] ruleFiles = ruleDirs[i].list();
			for (int j = 0; j < ruleFiles.length; j++) {
				String ruleFile = ruleDirs[i] + File.separator + ruleFiles[j];
				if (ruleFile.endsWith("henshin")) {
					Resource resource = this.henshinResourceSet.getResource(URI.createFileURI(ruleFile), true);
					Module module = (Module) resource.getContents().get(0);
					Rule rule = (Rule) module.getUnits().get(0);

					
					OperationKind kind = getOperationKind(rule);
					Set<Rule> rules = rulesByKind.get(kind);
					rules.add(rule);
					this.allRules.add(rule);
					if (kind == OperationKind.CREATE || kind == OperationKind.ADD || kind == OperationKind.SET_REFERENCE) {
						this.allConstructRules.add(rule);
					}
					if (kind == OperationKind.SET_ATTRIBUTE || kind == OperationKind.MOVE) {
						this.allChangeRules.add(rule);
					}
					if (kind == OperationKind.DELETE || kind == OperationKind.REMOVE || kind == OperationKind.UNSET_REFERENCE) {
						this.allDestructRules.add(rule);
					}
				}
			}
		}
	}

	public Set<Rule> getRulesByKind(OperationKind kind) {
		return rulesByKind.get(kind);
	}

	public Set<Rule> getAllRules() {
		return allRules;
	}

	public Set<Rule> getAllConstructRules() {
		return allConstructRules;
	}

	public void setAllChangeRules(Set<Rule> allChangeRules) {
		this.allChangeRules = allChangeRules;
	}

	public void setAllDestructRules(Set<Rule> allDestructRules) {
		this.allDestructRules = allDestructRules;
	}

	public boolean applyRule(Resource modelResource, Rule rule, int timeoutSeconds) {
		boolean success = false;

		// Load the model into an EGraph:
		EGraph graph = new EGraphImpl(modelResource.getContents().get(0));

		// Create an engine and a rule application:
		Engine engine = new EngineImpl();
		RuleApplication ruleApp = new RuleApplicationImpl(engine);
		ruleApp.setEGraph(graph);
		ruleApp.setRule(rule);

		List<Match> matchList = new ArrayList<Match>();

		final Runnable stuffToDo = new Thread() {
			@Override
			public void run() {
				// Find the matches (this can be time consuming)
				Iterable<Match> allMatches = engine.findMatches(rule, graph, null);
				for (Iterator<Match> iterator = allMatches.iterator(); iterator.hasNext();) {
					matchList.add(iterator.next());
				}
			}
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(stuffToDo);

		try {
			future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			/* Handle the interruption. Or ignore it. */
		} catch (ExecutionException ee) {
			/* Handle the error. Or ignore it. */
		} catch (TimeoutException te) {
			/* Handle the timeout. Or ignore it. */
		}
		if (!executor.isTerminated()) {
			// Stop the code that hasn't finished.
			executor.shutdownNow();
			//System.out.println("==> Found matches: " + matchList.size());
		}

		Match match = (Match) RandomUtils.randomlyGetFromList(matchList);

		// Add the value parameters to the match
		if (match != null) {
			randomlySupplyParameters(match, ruleApp);

			// Workaround(?) to not loose the value parameters
			ruleApp.setPartialMatch(match);
			ruleApp.setCompleteMatch(match);

			try {
				success = ruleApp.execute(null);
			} catch (Exception e) {
				System.err.println("----------------------------------------------");
				System.err.println(rule.getModule().getName());
				e.printStackTrace();
				System.err.println("----------------------------------------------");
			}
		} else {
			success = false;
		}

		return success;
	}

	public void randomlySupplyParameters(Match match, RuleApplication ruleApp) {
		for (Parameter param : match.getRule().getParameters()) {
			if (param.getType().getName().equals("String")) {
				String s = RandomUtils.randomlyGetString();
				match.setParameterValue(param, s);
				ruleApp.setParameterValue(param.getName(), s);
			} else if (param.getType().getName().equals("Boolean")) {
				boolean b = RandomUtils.randomlyGetBoolean();
				match.setParameterValue(param, b);
				ruleApp.setParameterValue(param.getName(), b);
			} else if (param.getType().getName().equals("Integer")) {
				int i = RandomUtils.randomlyGetInteger();
				match.setParameterValue(param, i);
				ruleApp.setParameterValue(param.getName(), i);
			} else if (param.getType().getName().equals("UnlimitedNatural")) {
				int i = RandomUtils.randomlyGetInteger();
				i = Math.abs(i);
				match.setParameterValue(param, i);
				ruleApp.setParameterValue(param.getName(), i);
			} else if (param.getType().getName().equals("Real")) {
				double d = RandomUtils.randomlyGetDouble();
				match.setParameterValue(param, d);
				ruleApp.setParameterValue(param.getName(), d);
			} else {
				String e = RandomUtils.randomlyGetEnumLiteral((EEnum) param.getType());
				match.setParameterValue(param, e);
				ruleApp.setParameterValue(param.getName(), e);
			}
		}
	}

	public OperationKind getOperationKind(Rule rule) {
		if (rule.getModule().getName().startsWith("CREATE")) {
			return OperationKind.CREATE;
		}
		if (rule.getModule().getName().startsWith("ADD")) {
			return OperationKind.ADD;
		}
		if (rule.getModule().getName().startsWith("SET_REFERENCE")) {
			return OperationKind.SET_REFERENCE;
		}
		if (rule.getModule().getName().startsWith("MOVE")) {
			return OperationKind.MOVE;
		}
		if (rule.getModule().getName().startsWith("SET_ATTRIBUTE")) {
			return OperationKind.SET_ATTRIBUTE;
		}
		if (rule.getModule().getName().startsWith("UNSET_REFERENCE")) {
			return OperationKind.UNSET_REFERENCE;
		}
		if (rule.getModule().getName().startsWith("REMOVE")) {
			return OperationKind.REMOVE;
		}
		if (rule.getModule().getName().startsWith("DELETE")) {
			return OperationKind.DELETE;
		}

		assert (false); // Unknown kind
		return null;

	}

	// public Rule randomlyGetApplicableRule(Resource modelResource) {
	// Set<Rule> applicableRules = getApplicableRules(modelResource);
	// return (Rule) RandomUtils.randomlyGetFromSet(applicableRules);
	// }

	// private Set<EClass> getAvailableTypes(Resource modelResource) {
	// Set<EClass> res = new HashSet<EClass>();
	// for (Iterator<EObject> iterator = modelResource.getAllContents();
	// iterator.hasNext();) {
	// EClass clazz = iterator.next().eClass();
	// res.add(clazz);
	// res.addAll(clazz.getEAllSuperTypes());
	// }
	//
	// return res;
	// }

	// public Set<Rule> getApplicableRules(Resource modelResource) {
	// Set<Rule> res = new HashSet<Rule>();
	// Set<EClass> modelTypes = getAvailableTypes(modelResource);
	// for (Rule rule : this.allRules) {
	// boolean include = true;
	// for (Node node : rule.getLhs().getNodes()) {
	// if (!modelTypes.contains(node.getType())) {
	// include = false;
	// break;
	// }
	// }
	// // TODO also check PAC
	// if (include) {
	// res.add(rule);
	// }
	// }
	//
	// return res;
	// }
}
