package de.hub.mse.emf.fuzz.junit.quickcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;

import de.hub.mse.emf.fuzz.junit.quickcheck.generator.InputSavingGenerator;

public class ModelGenerationStatus implements GenerationStatus{
	
	/** This might be used to map IDs to model instances/inputs? */
	private final Map<Key<?>, Object> contextValues = new HashMap<>();
		
	/** The number of inputs currently stored in the queue. */
	private int currentQueueSize = 0;
	
	/** Index of the current parent model instance. */
	private int currentParentInputIdx = 0;
	
	/** Number of child inputs generated for the current parent model. */
	private int currentChildCount = 0;
	
	/** Target number of child inputs to be generated for current parent input. */
	private int currentTargetChildCount = 0;
	
	/** Number of completed cycles */
	private int cyclesCompleted = 0;
	
	/** Callback to save inputs.*/
	Consumer<Integer> saveInputCallBack = null;

	public ModelGenerationStatus(Generator generator) {
		if (generator instanceof InputSavingGenerator) {
			this.saveInputCallBack = ((InputSavingGenerator) generator).generateSaveCallBack();
		}
	}
	
	@Override
	public int attempts() {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override public <T> GenerationStatus setValue(Key<T> key, T value) {
        contextValues.put(key, value);
        return this;
    }

    @Override public <T> Optional<T> valueOf(Key<T> key) {
        return Optional.ofNullable(key.cast(contextValues.get(key)));
    }

    @Override public int size() {
        return this.currentQueueSize;
    }
    
    public int getCurrentParentIdx() {
    	return this.currentParentInputIdx;
    }
    
    public int getCurrentChildCount() {
    	return this.currentChildCount;
    }
    
    public int getTargetNumChildren() {
    	return this.currentTargetChildCount;
    }
    
    public int getNumCycles() {
    	return this.cyclesCompleted;
    }
    
    public void update(int queueSize, int parentInputIdx, int childCount, int targetChildCount, int cycles) {
    	this.currentQueueSize = queueSize;
    	this.currentParentInputIdx = parentInputIdx;
    	this.currentChildCount = childCount;
    	this.currentTargetChildCount = targetChildCount;
    	this.cyclesCompleted = cycles;
    }
    
    public void saveInput(int value) {
    	if(saveInputCallBack != null) {
    		saveInputCallBack.accept(value);
    	}
    }

}
