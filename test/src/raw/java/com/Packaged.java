package com;

public class Packaged {
	public Packaged() {
	
	}
	
#ifdef TEST
	public Packaged(String testName) {
		System.out.println("Packaged(" + testName + ")");
	}
#endif
}