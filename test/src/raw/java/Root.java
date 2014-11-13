public class Root {
	// Comment with some Chinese characters: 广州

	public void run() {
#ifdef RELEASE
		validate();
#else
		//validate();
#endif
	}
#ifdef TEST
	public void test() {
	}
#endif

#ifndef RELEASE
	public void internal() {
	}
#endif 
}
