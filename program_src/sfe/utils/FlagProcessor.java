package sfe.utils;

import java.util.HashMap;
import java.util.function.Predicate;

public class FlagProcessor {

	private Predicate<String>[] flagsPredicate;
	private QuatConsumer<String, String[], Integer, HashMap<String, Object>>[] flagsProcess;
	private HashMap<String, Object> flagsData;
	
	public FlagProcessor() {
		this.flagsProcess = null;
		this.flagsPredicate = null;
		this.flagsData = new HashMap<>();
	}
	
	public Object getData(String key) {
		return flagsData.get(key);
	}
	
	public QuatConsumer<String, String[], Integer, HashMap<String, Object>>[] getFlagsProcess() {
		return flagsProcess;
	}
	
	public Predicate<String>[] getFlagsPredicate() {
		return flagsPredicate;
	}
	
	public FlagProcessor setFlagProcess(@SuppressWarnings("unchecked") QuatConsumer<String, String[], Integer, HashMap<String, Object>>... flagsProcess) {
		this.flagsProcess = flagsProcess;
		return this;
	}
	
	public FlagProcessor setFlagsPredicate(@SuppressWarnings("unchecked") Predicate<String>... flagsPredicate) {
		this.flagsPredicate = flagsPredicate;
		return this;
	}
	
	public void process(String[] args) {
		process(args, 0, args.length - 1);
	}
	
	public void process(String[] args, int startIndex) {
		process(args, startIndex, args.length - 1);
	}
	
	public void process(String[] args, int startIndex, int endIndex) {
		for(int i = startIndex; i != endIndex; i++) {
			//search flag index
			int flagIndex = -1;
			
			for(int fi = 0; fi < flagsPredicate.length && flagIndex == -1; fi++) {
				if(flagsPredicate[fi].test(args[i]))
					flagIndex = fi;
			}
			
			if(flagIndex != -1)
				flagsProcess[flagIndex].accept(args[i], args, i, flagsData);
		}
	}
	
	public boolean containsFlagData(String name) {
		return flagsData.containsKey(name);
	}
	
	public HashMap<String, Object> getFlagsData() {
		return flagsData;
	}
}
 