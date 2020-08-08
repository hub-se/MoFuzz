package nas.emf.repair.gen.benchmarks;

import static org.junit.Assert.assertSame;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.URIDistance;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLPackage;

import org.junit.Assert;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import edu.berkeley.cs.jqf.fuzz.Fuzz;

import nas.emf.repair.gen.uml.prototype.EMFModelGeneratorAdapter;
import de.hub.mse.emf.fuzz.JQFModelFuzzer;
import de.hub.mse.emf.generator.UMLGenerator;


@RunWith(JQFModelFuzzer.class)
public class EMFCompareTest {
	
    private IMerger.Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
	
	@Fuzz
	public void simpleCopyTest(@From(EMFModelGeneratorAdapter.class) Resource resource) {
    	Model model = (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model);
        EObject backup = EcoreUtil.copy(model);
        Comparison result = EMFCompare.builder().build()
                .compare(new DefaultComparisonScope(model, backup, null));
        Assert.assertEquals(0, result.getDifferences().size());
    }
	
	@Fuzz
	public void copyAllRightToLeft(@From(EMFModelGeneratorAdapter.class) Resource resource1, @From(EMFModelGeneratorAdapter.class) Resource resource2) {
    	Model model1 = (Model) EcoreUtil.getObjectByType(resource1.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model1);
        removeAllDuplicateCrossReferencesFrom(model1);
        EObject backup = EcoreUtil.copy(model1);

        Model model2 = (Model) EcoreUtil.getObjectByType(resource2.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model2);
        removeAllDuplicateCrossReferencesFrom(model2);
        EObject mutated = EcoreUtil.copy(model2);

        Comparison result = EMFCompare.builder().build()
                .compare(new DefaultComparisonScope(model2, backup, null));
        int nbDiffs = result.getDifferences().size();
        final IBatchMerger merger = new BatchMerger(mergerRegistry);
        merger.copyAllRightToLeft(result.getDifferences(), new BasicMonitor());

        for (Diff delta : result.getDifferences()) {
            assertSame(delta.getState(), DifferenceState.MERGED);
        }

        Comparison valid = EMFCompare.builder().build()
                .compare(new DefaultComparisonScope(model2, backup, null));
        List<Diff> differences = valid.getDifferences();

        Set<String> urisToDebug = Sets.newLinkedHashSet();
        for (ReferenceChange diff : Iterables.filter(differences, ReferenceChange.class)) {
            if (diff.getMatch().getRight() != null) {
                urisToDebug.add(new URIDistance().apply(diff.getMatch().getRight()).toString());
            }

        }
        
        
        if (urisToDebug.size() > 0) {
            // restart
            model2 = (Model) EcoreUtil.copy(mutated);
            result = EMFCompare.builder().build().compare(new DefaultComparisonScope(model2, backup, null));
            for (Diff diff : result.getDifferences()) {
                if (diff.getMatch().getRight() != null) {
                    String uri = new URIDistance().apply(diff.getMatch().getRight()).toString();
                    if (urisToDebug.contains(uri)) {
                        final IMerger diffMerger = mergerRegistry.getHighestRankingMerger(diff);
                        diffMerger.copyRightToLeft(diff, new BasicMonitor());
                    }
                }
            }
        }
		
        
        
        Assert.assertEquals("We still have differences after merging all of them (had " + nbDiffs
                + " to merge in the beginning)", 0, differences.size());

    }
	
	@Fuzz
	public void copyAllLeftToRight(@From(EMFModelGeneratorAdapter.class) Resource resource1, @From(EMFModelGeneratorAdapter.class) Resource resource2) {
		Model model1 = (Model) EcoreUtil.getObjectByType(resource1.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model1);
        removeAllDuplicateCrossReferencesFrom(model1);
        EObject backup = EcoreUtil.copy(model1);

        Model model2 = (Model) EcoreUtil.getObjectByType(resource2.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model2);
        removeAllDuplicateCrossReferencesFrom(model2);

        Comparison result = EMFCompare.builder().build()
                .compare(new DefaultComparisonScope(model2, backup, null));
        int nbDiffs = result.getDifferences().size();
        final IBatchMerger merger = new BatchMerger(mergerRegistry);
        merger.copyAllLeftToRight(result.getDifferences(), new BasicMonitor());

        Comparison valid = EMFCompare.builder().build()
                .compare(new DefaultComparisonScope(model2, backup, null));
        List<Diff> differences = valid.getDifferences();
        Assert.assertEquals("We still have differences after merging all of them (had " + nbDiffs
                + " to merge in the beginning)", 0, differences.size());
    }
	
	@Fuzz
	public void diffTest(@From(EMFModelGeneratorAdapter.class) Resource resource) {
    	Model model1 = (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model1);
        removeAllDuplicateCrossReferencesFrom(model1);
        EObject backup = EcoreUtil.copy(model1);
        
        UMLGenerator generator = new UMLGenerator(2000,10,10,1);
        SourceOfRandomness random = new SourceOfRandomness(new Random(24));
        Resource modelResource = generator.generate(random, null);
        Model model2 = (Model) EcoreUtil.getObjectByType(modelResource.getContents(), UMLPackage.Literals.MODEL);
        Assert.assertNotNull(model2);
        removeAllDuplicateCrossReferencesFrom(model2);
        
       // IEObjectMatcher matcher  = DefaultMatchEngine.createDefaultEObjectMatcher(UseIdentifiers.NEVER);
       // IComparisonFactory comparisonFactory = new DefaultComparisonFactory(new DefaultEqualityHelperFactory());
        IMatchEngine.Factory matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.NEVER);
        matchEngineFactory.setRanking(20);
        IMatchEngine.Factory.Registry matchEngineRegistry = new MatchEngineFactoryRegistryImpl();
        matchEngineRegistry.add(matchEngineFactory);
        EMFCompare comparator = EMFCompare.builder().setMatchEngineFactoryRegistry(matchEngineRegistry).build();
        
        //IComparisonScope scope = EMFCompare.createDefaultScope(resource.getResourceSet(), modelResource.getResourceSet());
        IComparisonScope scope = new DefaultComparisonScope(model1, model2, null);
        Comparison comparison = comparator.compare(scope);
        
        List<Diff> differences = comparison.getDifferences();
        /*
        IMerger.Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
        IBatchMerger merger = new BatchMerger(mergerRegistry);
        merger.copyAllRightToLeft(differences, new BasicMonitor());
        */
    }
	
	private static void removeAllDuplicateCrossReferencesFrom(EObject contentRoot) {
        for (EReference reference : contentRoot.eClass().getEAllReferences()) {
            if (!reference.isContainment() && !reference.isDerived() && reference.isMany()) {
                @SuppressWarnings("unchecked")
                final Iterator<EObject> crossReferences = ((List<EObject>)contentRoot.eGet(reference))
                        .iterator();
                final Set<EObject> noDupes = Sets.newHashSet();
                while (crossReferences.hasNext()) {
                    if (!noDupes.add(crossReferences.next())) {
                        crossReferences.remove();
                    }
                }
            }
        }

        final Iterator<EObject> contentIterator = contentRoot.eContents().iterator();
        while (contentIterator.hasNext()) {
            removeAllDuplicateCrossReferencesFrom(contentIterator.next());
        }
    }
}
