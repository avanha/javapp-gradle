package com.spritzinc.tools.gradle.javapp;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.StopActionException;
import org.gradle.api.tasks.TaskAction;

public class JavaPPTask extends DefaultTask {
	private ConfigurableFileTree files;
	private File outputDir;
	
	private List<Spec> specs;

	public void from(ConfigurableFileTree files) {
		this.files = files;
	}
	
	public void to(File to) {
		this.outputDir = to; 
	}

	/**
     * Returns the source files for this task.
     * 
     * @return The source files.
     */
    @InputFiles @SkipWhenEmpty @Optional
    public List<File> getSource() {
    	System.out.println("PreProcessFilesAction.getSource()");
    	
    	List<Spec> specs = getSpecs();
    	List<File> source = new ArrayList<File>(specs.size());
    	
    	for (Spec spec : specs) {
    		source.add(spec.in);
    	}
    	
    	return source; 
    }
    
    @OutputFiles
    public List<File> getTarget() {
    	System.out.println("PreProcessFilesAction.getTarget()");
    	
    	List<Spec> specs = getSpecs();
    	List<File> target = new ArrayList<File>(specs.size());
    	
    	for (Spec spec : specs) {
    		target.add(spec.out);
    	}
    	
    	return target;
    }
	
	@TaskAction
	public void process() {
		System.out.println("PreProcessFilesAction.execute(). files: " + files);
		
		List<Spec> specs = getSpecs();
		
		for (Spec spec : specs) {
			System.out.println(spec.in.toString() + "->" + spec.out.toString());
		}
	}
	
	
	private List<Spec> getSpecs() {
		if (specs == null) {
			if (files == null) {
				throw new StopActionException("No inputs defined");
			}
			
			if (outputDir == null) {
				throw new StopActionException("No output directory defined");
			}
			
			Set<File> inputs = files.getFiles();
			List<Spec> specs = new ArrayList<Spec>(inputs.size());
			Path base = files.getDir().toPath();
			
			for (File in : files.getFiles()) {
				Path relative = base.relativize(in.toPath());
				File out = new File(outputDir, relative.toString());
				System.out.println(in.toString() + "->" + out.toString());
				specs.add(new Spec(in, out));
			}
			
			this.specs = specs;
		}
		
		return specs;
	}
	
	private class Spec {
		File in;
		File out;
		
		Spec(File in, File out) {
			this.in = in;
			this.out = out;
		}
	}
}
